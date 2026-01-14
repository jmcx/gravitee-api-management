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
package io.gravitee.gateway.services.sync.process.common.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SubscriptionMapper {

    private final ObjectMapper objectMapper;
    private static final String FORMS_ANSWERS_METADATA_KEY = "__formsAnswers";

    public Subscription to(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        try {
            Subscription subscription = new Subscription();
            subscription.setApi(subscriptionModel.getApi());
            subscription.setApplication(subscriptionModel.getApplication());
            subscription.setApplicationName(subscriptionModel.getApplicationName());
            subscription.setClientId(subscriptionModel.getClientId());
            subscription.setClientCertificate(subscriptionModel.getClientCertificate());
            subscription.setStartingAt(subscriptionModel.getStartingAt());
            subscription.setEndingAt(subscriptionModel.getEndingAt());
            subscription.setId(subscriptionModel.getId());
            subscription.setPlan(subscriptionModel.getPlan());
            if (subscriptionModel.getStatus() != null) {
                subscription.setStatus(subscriptionModel.getStatus().name());
            }
            if (subscriptionModel.getConsumerStatus() != null) {
                subscription.setConsumerStatus(Subscription.ConsumerStatus.valueOf(subscriptionModel.getConsumerStatus().name()));
            }
            if (subscriptionModel.getType() != null) {
                subscription.setType(Subscription.Type.valueOf(subscriptionModel.getType().name().toUpperCase()));
            }
            if (subscriptionModel.getConfiguration() != null) {
                subscription.setConfiguration(
                    objectMapper.readValue(subscriptionModel.getConfiguration(), SubscriptionConfiguration.class)
                );
            }
            // Keep legacy metadata untouched, but expose typed form answers to gateway EL through a dedicated variable.
            // We transport the raw JSON string via an internal metadata key at runtime (not exposed via Management/Portal APIs).
            java.util.Map<String, String> metadata = subscriptionModel.getMetadata() != null
                ? new java.util.HashMap<>(subscriptionModel.getMetadata())
                : new java.util.HashMap<>();
            if (subscriptionModel.getFormsAnswers() != null && !subscriptionModel.getFormsAnswers().isBlank()) {
                metadata.put(FORMS_ANSWERS_METADATA_KEY, subscriptionModel.getFormsAnswers());
            }
            subscription.setMetadata(metadata.isEmpty() ? null : metadata);
            subscription.setEnvironmentId(subscriptionModel.getEnvironmentId());
            return subscription;
        } catch (Exception e) {
            log.error("Unable to map subscription from model [{}].", subscriptionModel.getId(), e);
            return null;
        }
    }
}
