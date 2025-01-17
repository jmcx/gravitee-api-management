/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Event.EventProperties.API_ID;
import static io.gravitee.repository.management.model.Event.EventProperties.ID;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.NewEventEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.EventNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE
 */
@Component
public class EventServiceImpl extends TransactionalService implements EventService {

    private final Logger LOGGER = LoggerFactory.getLogger(EventServiceImpl.class);

    @Lazy
    @Autowired
    private EventRepository eventRepository;

    @Lazy
    @Autowired
    private EventLatestRepository eventLatestRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PlanService planService;

    @Autowired
    private io.gravitee.rest.api.service.v4.PlanService planServiceV4;

    @Autowired
    private FlowService flowService;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private io.gravitee.rest.api.service.v4.FlowService flowServiceV4;

    @Autowired
    private PlanConverter planConverter;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public EventEntity findById(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Find event by ID: {}", id);

            Optional<Event> event = eventRepository.findById(id);

            if (event.isPresent()) {
                return convert(executionContext, event.get());
            }

            throw new EventNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an event using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an event using its ID " + id, ex);
        }
    }

    @Override
    public EventEntity createApiEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        String apiId,
        Map<String, String> properties
    ) {
        Map<String, String> eventProperties = initializeEventProperties(properties);
        if (apiId != null) {
            eventProperties.put(Event.EventProperties.API_ID.getValue(), apiId);
        }
        EventEntity event = createEvent(executionContext, environmentsIds, type, null, eventProperties);
        createOrPatchLatestEvent(apiId, event);
        return event;
    }

    @Override
    public EventEntity createApiEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        Api api,
        Map<String, String> properties
    ) {
        Map<String, String> eventProperties = initializeEventProperties(properties);
        Api apiDefinition = api != null ? buildApiEventPayload(executionContext, api) : null;
        if (apiDefinition != null) {
            eventProperties.put(Event.EventProperties.API_ID.getValue(), apiDefinition.getId());
        }
        EventEntity event = createEvent(executionContext, environmentsIds, type, apiDefinition, eventProperties);
        if (apiDefinition != null) {
            createOrPatchLatestEvent(apiDefinition.getId(), event);
        }
        return event;
    }

    @Override
    public EventEntity createDebugApiEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        DebugApi debugApi,
        Map<String, String> properties
    ) {
        return createEvent(executionContext, environmentsIds, type, debugApi, properties);
    }

    @Override
    public EventEntity createDictionaryEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        Dictionary dictionary
    ) {
        Map<String, String> eventProperties = new HashMap<>();
        if (dictionary != null) {
            eventProperties.put(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary.getId());
        }
        EventEntity event = createEvent(executionContext, environmentsIds, type, dictionary, eventProperties);
        if (dictionary != null) {
            createOrPatchLatestEvent(dictionary.getId(), event);
        }
        return event;
    }

    @Override
    public EventEntity createDynamicDictionaryEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        String dictionaryId
    ) {
        Map<String, String> eventProperties = new HashMap<>();
        if (dictionaryId != null) {
            eventProperties.put(Event.EventProperties.DICTIONARY_ID.getValue(), dictionaryId);
        }
        EventEntity event = createEvent(executionContext, environmentsIds, type, null, eventProperties);
        createOrPatchLatestEvent(dictionaryId + "-dynamic", event);
        return event;
    }

    @Override
    public EventEntity createOrganizationEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        OrganizationEntity organization
    ) {
        Map<String, String> eventProperties = new HashMap<>();
        if (organization != null) {
            eventProperties.put(Event.EventProperties.ORGANIZATION_ID.getValue(), organization.getId());
        }
        EventEntity event = createEvent(executionContext, environmentsIds, type, organization, eventProperties);
        if (organization != null) {
            createOrPatchLatestEvent(organization.getId(), event);
        }
        return event;
    }

    private static Map<String, String> initializeEventProperties(final Map<String, String> properties) {
        Map<String, String> eventProperties = new HashMap<>();
        if (properties != null) {
            eventProperties = new HashMap<>(properties);
        }
        return eventProperties;
    }

    private EventEntity createEvent(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        EventType type,
        Object object,
        Map<String, String> properties
    ) {
        try {
            String payload = object != null ? objectMapper.writeValueAsString(object) : null;
            NewEventEntity event = NewEventEntity.builder().type(type).payload(payload).properties(properties).build();
            return createNewEventEntity(executionContext, environmentsIds, event);
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException(String.format("Failed to create event [%s]", type), e);
        }
    }

    protected EventEntity createNewEventEntity(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        NewEventEntity newEventEntity
    ) {
        String hostAddress = "";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
            LOGGER.debug("Create {} for server {}", newEventEntity, hostAddress);

            Event event = convert(newEventEntity);
            event.setId(UuidString.generateRandom());
            event.setEnvironments(environmentsIds);
            // Set origin
            event.getProperties().put(Event.EventProperties.ORIGIN.getValue(), hostAddress);
            // Set date fields
            event.setCreatedAt(new Date());
            event.setUpdatedAt(event.getCreatedAt());

            Event createdEvent = eventRepository.create(event);

            return convert(executionContext, createdEvent);
        } catch (UnknownHostException e) {
            LOGGER.error("An error occurs while getting the server IP address", e);
            throw new TechnicalManagementException("An error occurs while getting the server IP address", e);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for server {}", newEventEntity, hostAddress, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying create " + newEventEntity + " for server " + hostAddress,
                ex
            );
        }
    }

    @Override
    public void deleteApiEvents(final ExecutionContext executionContext, final String apiId) {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        search(executionContext, query)
            .forEach(event -> {
                String eventId = event.getId();
                try {
                    LOGGER.debug("Delete Event {}", eventId);
                    eventRepository.delete(eventId);
                } catch (TechnicalException ex) {
                    throw new TechnicalManagementException("An error occurs while trying to delete Event " + eventId, ex);
                }
            });
        try {
            LOGGER.debug("Delete Event Latest {}", apiId);
            eventLatestRepository.delete(apiId);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete Event Latest " + apiId, ex);
        }
    }

    @Override
    public Page<EventEntity> search(
        ExecutionContext executionContext,
        List<EventType> eventTypes,
        Map<String, Object> properties,
        Long from,
        Long to,
        int page,
        int size,
        final List<String> environmentsIds
    ) {
        EventCriteria.EventCriteriaBuilder builder = EventCriteria.builder();
        if (from != null) {
            builder.from(from);
        }
        if (to != null) {
            builder.to(to);
        }

        if (eventTypes != null) {
            eventTypes.forEach(eventType -> builder.type(io.gravitee.repository.management.model.EventType.valueOf(eventType.name())));
        }

        if (properties != null) {
            builder.properties(properties);
        }

        builder.environments(environmentsIds);

        Page<Event> pageEvent = eventRepository.search(builder.build(), new PageableBuilder().pageNumber(page).pageSize(size).build());

        List<EventEntity> content = pageEvent
            .getContent()
            .stream()
            .map(event -> convert(executionContext, event))
            .collect(Collectors.toList());

        return new Page<>(content, pageEvent.getPageNumber(), (int) pageEvent.getPageElements(), pageEvent.getTotalElements());
    }

    @Override
    public Collection<EventEntity> search(ExecutionContext executionContext, final EventQuery query) {
        LOGGER.debug("Search APIs by {}", query);
        return convert(executionContext, eventRepository.search(queryToCriteria(executionContext, query).build()));
    }

    private EventCriteria.EventCriteriaBuilder queryToCriteria(ExecutionContext executionContext, EventQuery query) {
        final EventCriteria.EventCriteriaBuilder builder = EventCriteria
            .builder()
            // FIXME: Move this environments filter to EventQuery
            .environment(executionContext.getEnvironmentId());
        if (query == null) {
            return builder;
        }
        builder.from(query.getFrom()).to(query.getTo());

        if (!isEmpty(query.getTypes())) {
            query
                .getTypes()
                .forEach(eventType -> builder.type(io.gravitee.repository.management.model.EventType.valueOf(eventType.name())));
        }

        if (!isEmpty(query.getProperties())) {
            builder.properties(query.getProperties());
        }

        if (!isBlank(query.getApi())) {
            builder.property(API_ID.getValue(), query.getApi());
        }

        if (!isBlank(query.getId())) {
            builder.property(ID.getValue(), query.getId());
        }

        return builder;
    }

    private Set<EventEntity> convert(ExecutionContext executionContext, List<Event> events) {
        return events.stream().map(event -> convert(executionContext, event)).collect(toSet());
    }

    private EventEntity convert(ExecutionContext executionContext, Event event) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId(event.getId());
        eventEntity.setType(io.gravitee.rest.api.model.EventType.valueOf(event.getType().toString()));
        eventEntity.setPayload(event.getPayload());
        eventEntity.setParentId(event.getParentId());
        eventEntity.setProperties(event.getProperties());
        eventEntity.setCreatedAt(event.getCreatedAt());
        eventEntity.setUpdatedAt(event.getUpdatedAt());
        eventEntity.setEnvironments(event.getEnvironments());

        if (event.getProperties() != null) {
            final String userId = event.getProperties().get(Event.EventProperties.USER.getValue());
            if (userId != null && !userId.isEmpty()) {
                try {
                    eventEntity.setUser(userService.findById(executionContext, userId));
                } catch (UserNotFoundException unfe) {
                    UserEntity user = new UserEntity();
                    user.setSource("system");
                    user.setId(userId);
                    eventEntity.setUser(user);
                }
            }
        }

        return eventEntity;
    }

    private Event convert(NewEventEntity newEventEntity) {
        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.valueOf(newEventEntity.getType().toString()));
        event.setPayload(newEventEntity.getPayload());
        event.setParentId(newEventEntity.getParentId());
        event.setProperties(new HashMap<>(newEventEntity.getProperties()));

        return event;
    }

    /**
     * Build gateway API event payload for given API.
     *
     * @param executionContext
     * @param api
     * @return Gateway API event payload
     * @throws JsonProcessingException
     */
    private Api buildApiEventPayload(ExecutionContext executionContext, Api api) {
        try {
            Api apiForGatewayEvent = new Api(api);
            if (api.getDefinitionVersion() == null || !api.getDefinitionVersion().equals(DefinitionVersion.V4)) {
                apiForGatewayEvent.setDefinition(objectMapper.writeValueAsString(buildGatewayApiDefinition(executionContext, api)));
            } else {
                apiForGatewayEvent.setDefinition(objectMapper.writeValueAsString(buildGatewayApiDefinitionV4(executionContext, api)));
            }
            return apiForGatewayEvent;
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException(String.format("Failed to build API [%s] definition for gateway event", api.getId()), e);
        }
    }

    /**
     * Build gateway API definition for given Api.
     *
     * It reads API plans from plan collections, and API flows from flow collection ;
     * And generates gateway API definition from management API definition (containing no plans or flows).
     *
     * @param executionContext
     * @param api
     * @return API definition
     * @throws JsonProcessingException
     */
    private io.gravitee.definition.model.Api buildGatewayApiDefinition(ExecutionContext executionContext, Api api)
        throws JsonProcessingException {
        var apiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class);

        Set<PlanEntity> plans = planService
            .findByApi(executionContext, api.getId())
            .stream()
            .filter(p -> p.getStatus() != io.gravitee.rest.api.model.PlanStatus.CLOSED)
            .collect(toSet());

        apiDefinition.setPlans(planConverter.toPlansDefinitions(plans));
        apiDefinition.setFlows(flowService.findByReference(FlowReferenceType.API, api.getId()));
        return apiDefinition;
    }

    /**
     * Build gateway API definition for given Api.
     *
     * It reads API plans from plan collections, and API flows from flow collection ;
     * And generates gateway API definition from management API definition (containing no plans or flows).
     *
     * @param executionContext
     * @param api
     * @return API definition
     * @throws JsonProcessingException
     */
    private io.gravitee.definition.model.v4.Api buildGatewayApiDefinitionV4(ExecutionContext executionContext, Api api)
        throws JsonProcessingException {
        var apiDefinitionV4 = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class);

        Set<io.gravitee.rest.api.model.v4.plan.PlanEntity> plans = planServiceV4
            .findByApi(executionContext, api.getId())
            .stream()
            .filter(p -> p.getPlanStatus() != PlanStatus.CLOSED)
            .collect(toSet());

        apiDefinitionV4.setPlans(planMapper.toDefinitions(plans));
        apiDefinitionV4.setFlows(flowServiceV4.findByReference(FlowReferenceType.API, api.getId()));
        return apiDefinitionV4;
    }

    private void createOrPatchLatestEvent(final String latestEventId, final EventEntity event) {
        Event latestEvent = convert(event);
        latestEvent.setId(latestEventId);
        if (latestEvent.getProperties() == null) {
            latestEvent.setProperties(new HashMap<>());
        }
        latestEvent.getProperties().put(Event.EventProperties.ID.getValue(), event.getId());
        try {
            LOGGER.debug("Create or Update latest event {}.", latestEventId);
            eventLatestRepository.createOrUpdate(latestEvent);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying create or patch " + latestEvent, ex);
        }
    }

    private Event convert(EventEntity eventEntity) {
        Event event = new Event();
        event.setId(eventEntity.getId());
        event.setParentId(eventEntity.getParentId());
        event.setEnvironments(eventEntity.getEnvironments());
        event.setType(io.gravitee.repository.management.model.EventType.valueOf(eventEntity.getType().toString()));
        event.setPayload(eventEntity.getPayload());
        event.setProperties(new HashMap<>(eventEntity.getProperties()));
        event.setCreatedAt(eventEntity.getCreatedAt());
        event.setUpdatedAt(eventEntity.getUpdatedAt());

        return event;
    }
}
