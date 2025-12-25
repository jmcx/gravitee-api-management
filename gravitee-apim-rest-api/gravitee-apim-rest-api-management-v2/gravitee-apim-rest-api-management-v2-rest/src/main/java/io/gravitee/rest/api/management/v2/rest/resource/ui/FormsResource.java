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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FormsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private PortalNextFormsService portalNextFormsService;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.READ }) })
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        var executionContext = GraviteeContext.getExecutionContext();
        List<PortalNextForm> forms = portalNextFormsService.list(executionContext);
        return Response.ok(new FormsResponse(forms)).build();
    }

    @POST
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { RolePermissionAction.UPDATE }) })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Valid @NotNull CreateFormRequest request) {
        var executionContext = GraviteeContext.getExecutionContext();
        var created = portalNextFormsService.create(
            executionContext,
            PortalNextForm.builder().name(request.getName()).description(request.getDescription()).schema(request.getSchema()).build()
        );
        return Response.created(this.getLocationHeader(created.getId())).entity(created).build();
    }

    @Path("{formId}")
    public FormResource getFormResource() {
        return resourceContext.getResource(FormResource.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormsResponse {
        private List<PortalNextForm> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateFormRequest {
        private String name;
        private String description;
        private String schema;
    }
}


