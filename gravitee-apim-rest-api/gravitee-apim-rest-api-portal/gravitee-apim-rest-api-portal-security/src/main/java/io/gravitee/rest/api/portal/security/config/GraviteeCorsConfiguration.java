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
package io.gravitee.rest.api.portal.security.config;

import static io.gravitee.rest.api.security.csrf.CookieCsrfSignedTokenRepository.DEFAULT_CSRF_HEADER_NAME;
import static io.gravitee.rest.api.security.filter.RecaptchaFilter.DEFAULT_RECAPTCHA_HEADER_NAME;
import static java.util.Arrays.asList;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;

public class GraviteeCorsConfiguration extends CorsConfiguration implements EventListener<Key, Parameter> {

    private final ParameterService parameterService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final String environmentId;

    public GraviteeCorsConfiguration(
        final ParameterService parameterService,
        final InstallationAccessQueryService installationAccessQueryService,
        final EventManager eventManager,
        final String environmentId
    ) {
        this.parameterService = parameterService;
        this.installationAccessQueryService = installationAccessQueryService;
        this.environmentId = environmentId;

        eventManager.subscribeForEvents(this, Key.class);

        this.setAllowCredentials(true);
        this.setAllowedOriginPatterns(getPropertiesAsList(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, "*"));
        this.setAllowedHeaders(
                getPropertiesAsList(
                    Key.PORTAL_HTTP_CORS_ALLOW_HEADERS,
                    "Cache-Control, Pragma, Origin, Authorization, Content-Type, X-Requested-With, " +
                    DEFAULT_CSRF_HEADER_NAME +
                    ", " +
                    DEFAULT_RECAPTCHA_HEADER_NAME
                )
            );
        this.setAllowedMethods(getPropertiesAsList(Key.PORTAL_HTTP_CORS_ALLOW_METHODS, "OPTIONS, GET, POST, PUT, DELETE, PATCH"));
        this.setExposedHeaders(getPropertiesAsList(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS, DEFAULT_CSRF_HEADER_NAME));
        this.setMaxAge(
                Long.valueOf(
                    parameterService.find(
                        GraviteeContext.getExecutionContext(),
                        Key.PORTAL_HTTP_CORS_MAX_AGE,
                        environmentId,
                        ParameterReferenceType.ENVIRONMENT
                    )
                )
            );
    }

    @Override
    public void onEvent(Event<Key, Parameter> event) {
        if (environmentId.equals(event.content().getReferenceId())) {
            switch (event.type()) {
                case PORTAL_HTTP_CORS_ALLOW_ORIGIN -> this.setAllowedOriginPatterns(semicolonStringToList(event.content().getValue()));
                case PORTAL_HTTP_CORS_ALLOW_HEADERS -> this.setAllowedHeaders(semicolonStringToList(event.content().getValue()));
                case PORTAL_HTTP_CORS_ALLOW_METHODS -> this.setAllowedMethods(semicolonStringToList(event.content().getValue()));
                case PORTAL_HTTP_CORS_EXPOSED_HEADERS -> this.setExposedHeaders(semicolonStringToList(event.content().getValue()));
                case PORTAL_HTTP_CORS_MAX_AGE -> this.setMaxAge(Long.parseLong(event.content().getValue()));
            }
        }
    }

    private List<String> getPropertiesAsList(final Key propertyKey, final String defaultValue) {
        String property = parameterService.find(
            GraviteeContext.getExecutionContext(),
            propertyKey,
            environmentId,
            ParameterReferenceType.ENVIRONMENT
        );
        if (property == null) {
            property = defaultValue;
        }
        return semicolonStringToList(property);
    }

    private List<String> semicolonStringToList(String listStr) {
        return asList(listStr.replaceAll("\\s+", "").split(";"));
    }

    @Override
    public @NonNull CorsConfiguration setAllowedOriginPatterns(@Nullable List<String> allowedOriginPatterns) {
        List<String> builtAllowedOrigins = new ArrayList<>();
        if (allowedOriginPatterns != null) {
            builtAllowedOrigins.addAll(allowedOriginPatterns);
        }
        List<String> consoleUrls = installationAccessQueryService.getPortalUrls(GraviteeContext.getCurrentEnvironment());
        if (consoleUrls != null) {
            builtAllowedOrigins.addAll(consoleUrls);
        }
        return super.setAllowedOriginPatterns(builtAllowedOrigins);
    }
}
