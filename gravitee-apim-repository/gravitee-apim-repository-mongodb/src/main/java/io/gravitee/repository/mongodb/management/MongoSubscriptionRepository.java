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
package io.gravitee.repository.mongodb.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import io.gravitee.repository.mongodb.management.internal.plan.SubscriptionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoSubscriptionRepository implements SubscriptionRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoSubscriptionRepository.class);
    /**
     * Spring Data Mongo rejects map keys containing '.' (and keys starting with '$') unless a dot replacement is configured.
     * We store escaped keys in Mongo and unescape them when returning to the rest of the platform, so external APIs keep using
     * metadata keys like "forms.id" and "forms.answer.*".
     */
    private static final String DOT_REPLACEMENT = "__dot__";
    private static final String DOLLAR_REPLACEMENT_PREFIX = "__dollar__";

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private SubscriptionMongoRepository internalSubscriptionRepository;

    @Override
    public Page<Subscription> search(SubscriptionCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        return internalSubscriptionRepository.search(criteria, sortable, pageable).map(this::map);
    }

    @Override
    public List<Subscription> search(SubscriptionCriteria criteria, Sortable sortable) throws TechnicalException {
        Page<SubscriptionMongo> subscriptionsMongo = internalSubscriptionRepository.search(criteria, sortable, null);
        return subscriptionsMongo.getContent().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public List<Subscription> search(final SubscriptionCriteria criteria) throws TechnicalException {
        return search(criteria, null);
    }

    @Override
    public Set<String> findReferenceIdsOrderByNumberOfSubscriptions(SubscriptionCriteria criteria, Order order) {
        return internalSubscriptionRepository.findReferenceIdsOrderByNumberOfSubscriptions(criteria, order);
    }

    @Override
    public Optional<Subscription> findById(String subscription) throws TechnicalException {
        SubscriptionMongo subscriptionMongo = internalSubscriptionRepository.findById(subscription).orElse(null);
        return Optional.ofNullable(map(subscriptionMongo));
    }

    @Override
    public Subscription create(Subscription subscription) throws TechnicalException {
        SubscriptionMongo subscriptionMongo = mapper.map(subscription);
        subscriptionMongo.setMetadata(escapeMetadataKeys(subscriptionMongo.getMetadata()));
        subscriptionMongo = internalSubscriptionRepository.insert(subscriptionMongo);
        return map(subscriptionMongo);
    }

    @Override
    public Subscription update(Subscription subscription) throws TechnicalException {
        if (subscription == null || subscription.getId() == null) {
            throw new IllegalStateException("Subscription to update must have an id");
        }

        SubscriptionMongo subscriptionMongo = internalSubscriptionRepository.findById(subscription.getId()).orElse(null);

        if (subscriptionMongo == null) {
            throw new IllegalStateException(String.format("No subscription found with id [%s]", subscription.getId()));
        }

        subscriptionMongo = mapper.map(subscription);
        subscriptionMongo.setMetadata(escapeMetadataKeys(subscriptionMongo.getMetadata()));
        subscriptionMongo = internalSubscriptionRepository.save(subscriptionMongo);
        return map(subscriptionMongo);
    }

    @Override
    public void delete(String plan) throws TechnicalException {
        internalSubscriptionRepository.deleteById(plan);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("Delete subscriptions by environment [{}]", environmentId);
        try {
            final var subscriptionMongos = internalSubscriptionRepository
                .deleteByEnvironmentId(environmentId)
                .stream()
                .map(SubscriptionMongo::getId)
                .toList();
            LOGGER.debug("Delete subscriptions by environment [{}] - Done", environmentId);
            return subscriptionMongos;
        } catch (Exception e) {
            LOGGER.error("Failed to delete subscriptions by environment [{}]", environmentId, e);
            throw new TechnicalException("Failed to delete subscriptions by environment");
        }
    }

    @Override
    public Set<Subscription> findAll() throws TechnicalException {
        return internalSubscriptionRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public List<Subscription> findByIdIn(Collection<String> ids) throws TechnicalException {
        try {
            Iterable<SubscriptionMongo> subscriptions = internalSubscriptionRepository.findAllById(ids);
            return StreamSupport.stream(subscriptions.spliterator(), false).map(this::map).collect(Collectors.toList());
        } catch (Exception e) {
            throw new TechnicalException("An error occurred trying to find subscriptions by id list", e);
        }
    }

    private Subscription map(SubscriptionMongo subscriptionMongo) {
        if (subscriptionMongo == null) {
            return null;
        }
        Subscription subscription = mapper.map(subscriptionMongo);
        subscription.setMetadata(unescapeMetadataKeys(subscription.getMetadata()));
        return subscription;
    }

    private static java.util.Map<String, String> escapeMetadataKeys(java.util.Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }
        final java.util.Map<String, String> escaped = new HashMap<>(metadata.size());
        for (var entry : metadata.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key == null) {
                continue;
            }
            String outKey = key.replace(".", DOT_REPLACEMENT);
            if (outKey.startsWith("$")) {
                outKey = DOLLAR_REPLACEMENT_PREFIX + outKey.substring(1);
            }
            escaped.put(outKey, value);
        }
        return escaped;
    }

    private static java.util.Map<String, String> unescapeMetadataKeys(java.util.Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }
        final java.util.Map<String, String> unescaped = new HashMap<>(metadata.size());
        for (var entry : metadata.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key == null) {
                continue;
            }
            String outKey = key;
            if (outKey.startsWith(DOLLAR_REPLACEMENT_PREFIX)) {
                outKey = "$" + outKey.substring(DOLLAR_REPLACEMENT_PREFIX.length());
            }
            outKey = outKey.replace(DOT_REPLACEMENT, ".");
            unescaped.put(outKey, value);
        }
        return unescaped;
    }
}
