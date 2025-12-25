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
package io.gravitee.rest.api.service.portalnext.forms.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.portalnext.forms.model.PortalNextForm;
import jakarta.ws.rs.BadRequestException;
import java.util.Map;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNextFormsServiceImplTest {

    @Mock
    private ParameterService parameterService;

    @Mock
    private DictionaryService dictionaryService;

    private PortalNextFormsServiceImpl cut;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutionContext executionContext = new ExecutionContext("DEFAULT", "DEFAULT");

    @BeforeEach
    void setUp() {
        cut = new PortalNextFormsServiceImpl(parameterService, objectMapper, dictionaryService);
    }

    @Test
    void should_create_form_and_persist_config() {
        when(parameterService.find(eq(executionContext), eq(Key.PORTAL_NEXT_FORMS), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("{\"forms\":[],\"activeFormId\":null}");

        var created = cut.create(
            executionContext,
            PortalNextForm.builder()
                .name("My form")
                .description("desc")
                .schema("{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\",\"format\":\"email\"}}}")
                .build()
        );

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();

        verify(parameterService).save(eq(executionContext), eq(Key.PORTAL_NEXT_FORMS), any(String.class), eq(ParameterReferenceType.ENVIRONMENT));
    }

    @Test
    void should_reject_invalid_schema_json() {
        when(parameterService.find(eq(executionContext), eq(Key.PORTAL_NEXT_FORMS), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn("{\"forms\":[],\"activeFormId\":null}");

        assertThrows(
            BadRequestException.class,
            () ->
                cut.create(
                    executionContext,
                    PortalNextForm.builder().name("My form").schema("{not-json").build()
                )
        );
    }

    @Test
    void list_should_mark_active_form() {
        when(parameterService.find(eq(executionContext), eq(Key.PORTAL_NEXT_FORMS), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(
                "{\"activeFormId\":\"f2\",\"forms\":[{\"id\":\"f1\",\"name\":\"A\",\"schema\":\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{}}\"},{\"id\":\"f2\",\"name\":\"B\",\"schema\":\"{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{}}\"}]}"
            );

        List<PortalNextForm> forms = cut.list(executionContext);
        assertThat(forms).hasSize(2);
        assertThat(forms.stream().filter(f -> "f2".equals(f.getId())).findFirst().orElseThrow().getActive()).isTrue();
        assertThat(forms.stream().filter(f -> "f1".equals(f.getId())).findFirst().orElseThrow().getActive()).isFalse();
    }

    @Test
    void should_accept_schema_with_x_gravitee_dictionary_and_resolve_it_on_get_active() {
        when(parameterService.find(eq(executionContext), eq(Key.PORTAL_NEXT_FORMS), eq(ParameterReferenceType.ENVIRONMENT)))
            .thenReturn(
                """
                {"activeFormId":"f1","forms":[{"id":"f1","name":"A","schema":"{\\"type\\":\\"object\\",\\"properties\\":{\\"features\\":{\\"type\\":\\"array\\",\\"title\\":\\"Requested features\\",\\"minItems\\":1,\\"items\\":{\\"x-gravitee-dictionary\\":\\"subscription_features\\"}}}}"}]}
                """
            );

        DictionaryEntity dictionary = DictionaryEntity
            .builder()
            .id("subscriptionfeatures")
            .name("subscription_features")
            .properties(Map.of("analytics", "Analytics", "webhooks", "Webhooks"))
            .build();
        when(dictionaryService.findAll(eq(executionContext))).thenReturn(Set.of(dictionary));

        var active = cut.getActive(executionContext).orElseThrow();
        assertThat(active.getActive()).isTrue();
        assertThat(active.getSchema()).contains("\"anyOf\"");
        assertThat(active.getSchema()).contains("\"const\":\"analytics\"");
        assertThat(active.getSchema()).contains("\"title\":\"Analytics\"");
        assertThat(active.getSchema()).doesNotContain("x-gravitee-dictionary");
    }
}


