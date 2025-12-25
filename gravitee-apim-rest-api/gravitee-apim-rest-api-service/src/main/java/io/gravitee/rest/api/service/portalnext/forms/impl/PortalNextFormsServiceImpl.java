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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.portalnext.forms.PortalNextFormsService;
import io.gravitee.rest.api.service.portalnext.forms.model.PortalNextForm;
import io.gravitee.rest.api.service.portalnext.forms.model.PortalNextFormsConfig;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PortalNextFormsServiceImpl implements PortalNextFormsService {

    private final ParameterService parameterService;
    private final ObjectMapper objectMapper;
    private final DictionaryService dictionaryService;

    public PortalNextFormsServiceImpl(
        ParameterService parameterService,
        ObjectMapper objectMapper,
        DictionaryService dictionaryService
    ) {
        this.parameterService = parameterService;
        this.objectMapper = objectMapper;
        this.dictionaryService = dictionaryService;
    }

    @Override
    public List<PortalNextForm> list(ExecutionContext executionContext) {
        var config = readConfig(executionContext);
        return config
            .getForms()
            .stream()
            .sorted(Comparator.comparing(PortalNextForm::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .map(f -> f.toBuilder().active(Objects.equals(config.getActiveFormId(), f.getId())).build())
            .toList();
    }

    @Override
    public Optional<PortalNextForm> get(ExecutionContext executionContext, String formId) {
        var config = readConfig(executionContext);
        return config
            .getForms()
            .stream()
            .filter(f -> Objects.equals(f.getId(), formId))
            .findFirst()
            .map(f -> f.toBuilder().active(Objects.equals(config.getActiveFormId(), f.getId())).build());
    }

    @Override
    public Optional<PortalNextForm> getActive(ExecutionContext executionContext) {
        var config = readConfig(executionContext);
        if (config.getActiveFormId() == null) {
            return Optional.empty();
        }
        return config
            .getForms()
            .stream()
            .filter(f -> Objects.equals(f.getId(), config.getActiveFormId()))
            .findFirst()
            .map(f ->
                f
                    .toBuilder()
                    .active(true)
                    // Portal endpoint should receive the resolved schema.
                    .schema(resolveSchemaForPortal(executionContext, f.getSchema()))
                    .build()
            );
    }

    @Override
    public PortalNextForm create(ExecutionContext executionContext, PortalNextForm form) {
        validateIncomingForm(form, true);
        validateSchema(form.getSchema());

        var config = readConfig(executionContext);
        final ZonedDateTime now = TimeProvider.now();
        var created = form
            .toBuilder()
            .id(UUID.randomUUID().toString())
            .createdAt(now)
            .updatedAt(now)
            .active(false)
            .build();
        config.getForms().add(created);
        writeConfig(executionContext, config);
        return created;
    }

    @Override
    public PortalNextForm update(ExecutionContext executionContext, String formId, PortalNextForm form) {
        validateIncomingForm(form, false);
        validateSchema(form.getSchema());

        var config = readConfig(executionContext);
        var existing = config
            .getForms()
            .stream()
            .filter(f -> Objects.equals(f.getId(), formId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Form not found"));

        final ZonedDateTime now = TimeProvider.now();
        var updated = existing
            .toBuilder()
            .name(form.getName())
            .description(form.getDescription())
            .schema(form.getSchema())
            .updatedAt(now)
            .active(Objects.equals(config.getActiveFormId(), formId))
            .build();

        config.getForms().removeIf(f -> Objects.equals(f.getId(), formId));
        config.getForms().add(updated);
        writeConfig(executionContext, config);
        return updated;
    }

    @Override
    public void delete(ExecutionContext executionContext, String formId) {
        var config = readConfig(executionContext);
        boolean removed = config.getForms().removeIf(f -> Objects.equals(f.getId(), formId));
        if (!removed) {
            throw new NotFoundException("Form not found");
        }
        if (Objects.equals(config.getActiveFormId(), formId)) {
            config.setActiveFormId(null);
        }
        writeConfig(executionContext, config);
    }

    @Override
    public PortalNextForm activate(ExecutionContext executionContext, String formId) {
        var config = readConfig(executionContext);
        var form = config
            .getForms()
            .stream()
            .filter(f -> Objects.equals(f.getId(), formId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Form not found"));

        config.setActiveFormId(formId);
        writeConfig(executionContext, config);
        return form.toBuilder().active(true).build();
    }

    private PortalNextFormsConfig readConfig(ExecutionContext executionContext) {
        String raw = parameterService.find(executionContext, Key.PORTAL_NEXT_FORMS, ParameterReferenceType.ENVIRONMENT);
        if (raw == null || raw.isBlank()) {
            return PortalNextFormsConfig.builder().build();
        }
        try {
            var parsed = objectMapper.readValue(raw, PortalNextFormsConfig.class);
            var forms = parsed.getForms() == null ? new ArrayList<PortalNextForm>() : new ArrayList<>(parsed.getForms());
            return PortalNextFormsConfig.builder().forms(forms).activeFormId(parsed.getActiveFormId()).build();
        } catch (Exception e) {
            log.warn("Invalid portal.next.forms JSON stored in parameters; returning empty config", e);
            return PortalNextFormsConfig.builder().build();
        }
    }

    private void writeConfig(ExecutionContext executionContext, PortalNextFormsConfig config) {
        try {
            String raw = objectMapper.writeValueAsString(config);
            parameterService.save(executionContext, Key.PORTAL_NEXT_FORMS, raw, ParameterReferenceType.ENVIRONMENT);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Unable to serialize forms config", e);
        }
    }

    private void validateIncomingForm(PortalNextForm form, boolean isCreate) {
        if (form == null) {
            throw new BadRequestException("Form payload is required");
        }
        if (!isCreate && form.getId() != null) {
            // id comes from the path, ignore or reject to avoid confusion
            throw new BadRequestException("Form id must not be provided in payload");
        }
        if (form.getName() == null || form.getName().isBlank()) {
            throw new BadRequestException("Form name is required");
        }
        if (form.getSchema() == null || form.getSchema().isBlank()) {
            throw new BadRequestException("Form schema is required");
        }
    }

    /**
     * Best-effort validation against the restricted meta-schema described in Forms-feature.md.
     * We validate that JSON is an object schema whose properties are constrained to supported primitive/enum/array patterns.
     */
    private void validateSchema(String rawSchema) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(rawSchema);
        } catch (Exception e) {
            throw new BadRequestException("Schema must be valid JSON", e);
        }
        if (root == null || !root.isObject()) {
            throw new BadRequestException("Schema must be a JSON object");
        }
        var type = text(root.get("type"));
        if (!"object".equals(type)) {
            throw new BadRequestException("Schema root type must be 'object'");
        }
        JsonNode properties = root.get("properties");
        if (properties == null || !properties.isObject()) {
            throw new BadRequestException("Schema must define an object 'properties'");
        }
        properties
            .fields()
            .forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldSchema = entry.getValue();
                try {
                    validateFieldSchema(fieldSchema);
                } catch (BadRequestException e) {
                    throw new BadRequestException("Invalid schema for property '" + fieldName + "': " + e.getMessage(), e);
                }
            });
    }

    private void validateFieldSchema(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            throw new BadRequestException("Field schema must be an object");
        }

        // enum variants
        if (schema.has("enum")) {
            // enum is supported only for strings (single select)
            if (!"string".equals(text(schema.get("type")))) {
                throw new BadRequestException("enum is only supported for type 'string'");
            }
            if (!schema.get("enum").isArray()) {
                throw new BadRequestException("enum must be an array");
            }
            return;
        }
        if (schema.has("oneOf")) {
            // oneOf for single select enum with titles
            if (!"string".equals(text(schema.get("type")))) {
                throw new BadRequestException("oneOf is only supported for type 'string'");
            }
            validateConstTitleArray(schema.get("oneOf"));
            return;
        }
        if ("array".equals(text(schema.get("type")))) {
            JsonNode items = schema.get("items");
            if (items == null || !items.isObject()) {
                throw new BadRequestException("array.items is required and must be an object");
            }
            if (items.has("x-gravitee-dictionary")) {
                String dictKey = text(items.get("x-gravitee-dictionary"));
                if (dictKey == null || dictKey.isBlank()) {
                    throw new BadRequestException("array.items.x-gravitee-dictionary must be a non-empty string");
                }
                return;
            }
            if (items.has("enum")) {
                if (!items.get("enum").isArray()) {
                    throw new BadRequestException("array.items.enum must be an array");
                }
                return;
            }
            if (items.has("anyOf")) {
                validateConstTitleArray(items.get("anyOf"));
                return;
            }
            throw new BadRequestException("array.items must define either enum or anyOf");
        }

        String type = text(schema.get("type"));
        if (type == null) {
            throw new BadRequestException("type is required");
        }
        switch (type) {
            case "string" -> {
                // supported formats: email, uri, date, date-time
                String format = text(schema.get("format"));
                if (format != null && !List.of("email", "uri", "date", "date-time").contains(format)) {
                    throw new BadRequestException("Unsupported string format: " + format);
                }
            }
            case "number", "integer", "boolean" -> {
                // ok
            }
            default -> throw new BadRequestException("Unsupported type: " + type);
        }
    }

    private void validateConstTitleArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new BadRequestException("must be an array");
        }
        node.forEach(entry -> {
            if (!entry.isObject()) {
                throw new BadRequestException("entries must be objects");
            }
            if (!entry.has("const")) {
                throw new BadRequestException("entries must define 'const'");
            }
            if (!entry.has("title")) {
                throw new BadRequestException("entries must define 'title'");
            }
        });
    }

    private String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isTextual() ? node.asText() : null;
    }

    /**
     * Resolve custom schema extensions into a schema consumable by Portal Next UI.
     *
     * Currently supported:
     * - array.items.x-gravitee-dictionary: dictionary key (environment-scoped)
     *   Expanded into: array.items.anyOf [{const, title}, ...]
     */
    private String resolveSchemaForPortal(ExecutionContext executionContext, String rawSchema) {
        if (rawSchema == null || rawSchema.isBlank()) {
            return rawSchema;
        }

        final JsonNode root;
        try {
            root = objectMapper.readTree(rawSchema);
        } catch (Exception e) {
            // Stored schema should already be validated at write time; don't break the portal if it isn't.
            log.warn("Unable to parse stored form schema JSON; returning raw schema", e);
            return rawSchema;
        }
        if (root == null || !root.isObject()) {
            return rawSchema;
        }

        JsonNode properties = root.get("properties");
        if (properties == null || !properties.isObject()) {
            return rawSchema;
        }

        ObjectNode rootObj = (ObjectNode) root;
        ObjectNode propsObj = (ObjectNode) properties;

        propsObj
            .fields()
            .forEachRemaining(entry -> {
                JsonNode fieldSchema = entry.getValue();
                if (fieldSchema == null || !fieldSchema.isObject()) {
                    return;
                }
                ObjectNode fieldObj = (ObjectNode) fieldSchema;
                if (!"array".equals(text(fieldObj.get("type")))) {
                    return;
                }
                JsonNode itemsNode = fieldObj.get("items");
                if (itemsNode == null || !itemsNode.isObject()) {
                    return;
                }
                ObjectNode itemsObj = (ObjectNode) itemsNode;
                String dictKey = text(itemsObj.get("x-gravitee-dictionary"));
                if (dictKey == null || dictKey.isBlank()) {
                    return;
                }

                ArrayNode anyOf = itemsObj.arrayNode();
                try {
                    Map<String, String> options = loadDictionaryOptions(executionContext, dictKey);
                    options.forEach((k, v) -> {
                        ObjectNode opt = itemsObj.objectNode();
                        opt.put("const", k);
                        opt.put("title", v);
                        anyOf.add(opt);
                    });
                } catch (Exception e) {
                    log.warn("Unable to resolve dictionary '{}' for form schema; returning empty options", dictKey, e);
                }

                itemsObj.set("anyOf", anyOf);
                itemsObj.remove("x-gravitee-dictionary");
            });

        try {
            return objectMapper.writeValueAsString(rootObj);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize resolved form schema; returning raw schema", e);
            return rawSchema;
        }
    }

    private Map<String, String> loadDictionaryOptions(ExecutionContext executionContext, String dictionaryKey) {
        // Best-effort: dictionary API is environment-scoped already.
        // Use a stable order for deterministic schemas.
        // 1) Try direct lookup by id (most stable identifier).
        try {
            DictionaryEntity dict = dictionaryService.findById(executionContext, dictionaryKey);
            if (dict != null && dict.getProperties() != null) {
                return new TreeMap<>(dict.getProperties());
            }
        } catch (Exception ignore) {
            // fall back
        }

        // 2) Fall back to match by name/key in the list, then fetch full entity by id (to ensure properties are loaded).
        var all = dictionaryService.findAll(executionContext);
        if (all == null || all.isEmpty()) {
            return Map.of();
        }

        var match = all
            .stream()
            .filter(d ->
                Objects.equals(d.getId(), dictionaryKey) ||
                Objects.equals(d.getKey(), dictionaryKey) ||
                Objects.equals(d.getName(), dictionaryKey)
            )
            .findFirst();

        if (match.isEmpty()) {
            return Map.of();
        }

        var entity = match.get();
        if (entity.getProperties() != null) {
            return new TreeMap<>(entity.getProperties());
        }

        try {
            DictionaryEntity full = dictionaryService.findById(executionContext, entity.getId());
            if (full != null && full.getProperties() != null) {
                return new TreeMap<>(full.getProperties());
            }
        } catch (Exception ignore) {
            // ignore
        }

        return Map.of();
    }
}


