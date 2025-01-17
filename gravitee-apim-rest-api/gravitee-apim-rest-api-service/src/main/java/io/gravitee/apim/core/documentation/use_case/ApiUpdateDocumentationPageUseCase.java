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
package io.gravitee.apim.core.documentation.use_case;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.domain_service.ApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.HomepageDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.documentation.query_service.PageQueryService;
import java.util.Date;
import java.util.Objects;
import lombok.Builder;

public class ApiUpdateDocumentationPageUseCase {

    private final UpdateApiDocumentationDomainService updateApiDocumentationDomainService;
    private final ApiDocumentationDomainService apiDocumentationDomainService;
    private final HomepageDomainService homepageDomainService;
    private final ApiCrudService apiCrudService;
    private final PageCrudService pageCrudService;
    private final PageQueryService pageQueryService;

    public ApiUpdateDocumentationPageUseCase(
        UpdateApiDocumentationDomainService updateApiDocumentationDomainService,
        ApiDocumentationDomainService apiDocumentationDomainService,
        HomepageDomainService homepageDomainService,
        ApiCrudService apiCrudService,
        PageCrudService pageCrudService,
        PageQueryService pageQueryService
    ) {
        this.updateApiDocumentationDomainService = updateApiDocumentationDomainService;
        this.apiDocumentationDomainService = apiDocumentationDomainService;
        this.homepageDomainService = homepageDomainService;
        this.apiCrudService = apiCrudService;
        this.pageCrudService = pageCrudService;
        this.pageQueryService = pageQueryService;
    }

    public Output execute(Input input) {
        this.apiCrudService.get(input.apiId);

        var oldPage = this.pageCrudService.get(input.pageId);
        this.apiDocumentationDomainService.validatePageAssociatedToApi(oldPage, input.apiId);

        Page.PageBuilder newPage = oldPage.toBuilder();

        if (!Objects.equals(oldPage.getName(), input.name)) {
            this.apiDocumentationDomainService.validateNameIsUnique(input.apiId, oldPage.getParentId(), input.name, oldPage.getType());
            newPage.name(input.name);
        }

        if (oldPage.isMarkdown() && !Objects.equals(oldPage.getContent(), input.content)) {
            this.apiDocumentationDomainService.validateContentIsSafe(input.content);
            newPage.content(input.content);
        }

        newPage.updatedAt(new Date());
        newPage.visibility(input.visibility);
        newPage.homepage(input.homepage);
        newPage.order(input.order);

        var updatedPage = this.updateApiDocumentationDomainService.updatePage(newPage.build(), oldPage, input.auditInfo);

        if (updatedPage.isMarkdown() && updatedPage.isHomepage() && !oldPage.isHomepage()) {
            this.homepageDomainService.setPreviousHomepageToFalse(input.apiId, updatedPage.getId());
        }

        if (updatedPage.getOrder() != oldPage.getOrder()) {
            this.updatePageOrders(oldPage.getOrder(), updatedPage, input.auditInfo);
        }

        updatedPage = updatedPage.withHidden(this.apiDocumentationDomainService.pageIsHidden(updatedPage));

        return new Output(updatedPage);
    }

    @Builder
    public record Input(
        String apiId,
        String pageId,
        String name,
        int order,
        Page.Visibility visibility,
        String content,
        boolean homepage,
        AuditInfo auditInfo
    ) {}

    public record Output(Page page) {}

    private void updatePageOrders(int oldOrder, Page updatedPage, AuditInfo auditInfo) {
        var newOrder = updatedPage.getOrder();
        var shouldMoveDown = newOrder < oldOrder;
        var orderIncrement = shouldMoveDown ? 1 : -1;

        this.pageQueryService.searchByApiIdAndParentId(updatedPage.getReferenceId(), updatedPage.getParentId())
            .stream()
            .filter(page -> !Objects.equals(page.getId(), updatedPage.getId()))
            .filter(page ->
                shouldMoveDown
                    ? this.toBeMovedDown(oldOrder, newOrder, page.getOrder())
                    : this.toBeMovedUp(oldOrder, newOrder, page.getOrder())
            )
            .forEach(page -> {
                var updatedOrder = page.getOrder() + orderIncrement;
                this.updateApiDocumentationDomainService.updatePage(page.toBuilder().order(updatedOrder).build(), page, auditInfo);
            });
    }

    private boolean toBeMovedUp(int oldOrder, int newOrder, int pageOrder) {
        return oldOrder < pageOrder && pageOrder <= newOrder;
    }

    private boolean toBeMovedDown(int oldOrder, int newOrder, int pageOrder) {
        return newOrder <= pageOrder && pageOrder < oldOrder;
    }
}
