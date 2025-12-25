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
package io.gravitee.rest.api.management.v2.rest.resource.ui;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.portalnext.forms.PortalNextFormsService;
import io.gravitee.rest.api.service.portalnext.forms.model.PortalNextForm;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FormResource extends AbstractResource {

    @PathParam("formId")
    private String formId;

    @Inject
    private PortalNextFormsService portalNextFormsService;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.READ }) })
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        var executionContext = GraviteeContext.getExecutionContext();
        var form = portalNextFormsService.get(executionContext, formId).orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Form not found"));
        return Response.ok(form).build();
    }

    @PUT
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@Valid @NotNull UpdateFormRequest request) {
        var executionContext = GraviteeContext.getExecutionContext();
        var updated = portalNextFormsService.update(
            executionContext,
            formId,
            PortalNextForm.builder().name(request.getName()).description(request.getDescription()).schema(request.getSchema()).build()
        );
        return Response.ok(updated).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    public Response delete() {
        var executionContext = GraviteeContext.getExecutionContext();
        portalNextFormsService.delete(executionContext, formId);
        return Response.noContent().build();
    }

    @POST
    @Path("_activate")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    @Produces(MediaType.APPLICATION_JSON)
    public Response activate() {
        var executionContext = GraviteeContext.getExecutionContext();
        var active = portalNextFormsService.activate(executionContext, formId);
        return Response.ok(active).build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateFormRequest {
        private String name;
        private String description;
        private String schema;
    }
}


