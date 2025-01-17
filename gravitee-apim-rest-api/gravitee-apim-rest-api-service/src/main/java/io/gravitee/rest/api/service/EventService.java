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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Titouan COMPIEGNE
 */
public interface EventService {
    EventEntity findById(ExecutionContext executionContext, String id);

    EventEntity createApiEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        String apiId,
        Map<String, String> properties
    );

    EventEntity createApiEvent(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        EventType type,
        Api api,
        Map<String, String> properties
    );

    EventEntity createDictionaryEvent(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        EventType type,
        Dictionary dictionary
    );

    EventEntity createDynamicDictionaryEvent(
        ExecutionContext executionContext,
        Set<String> environmentsIds,
        EventType type,
        String dictionaryId
    );

    EventEntity createDebugApiEvent(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        EventType type,
        DebugApi debugApi,
        Map<String, String> properties
    );

    EventEntity createOrganizationEvent(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        EventType type,
        OrganizationEntity organizationEntity
    );

    void deleteApiEvents(final ExecutionContext executionContext, String apiId);

    Page<EventEntity> search(
        ExecutionContext executionContext,
        List<EventType> eventTypes,
        Map<String, Object> properties,
        Long from,
        Long to,
        int page,
        int size,
        final List<String> environments
    );

    Collection<EventEntity> search(ExecutionContext executionContext, EventQuery query);
}
