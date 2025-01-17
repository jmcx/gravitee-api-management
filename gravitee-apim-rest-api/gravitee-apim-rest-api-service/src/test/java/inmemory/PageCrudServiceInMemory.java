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
package inmemory;

import com.google.common.collect.ImmutableList;
import io.gravitee.apim.core.documentation.crud_service.PageCrudService;
import io.gravitee.apim.core.documentation.model.*;
import io.gravitee.apim.infra.adapter.PageAdapter;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PageCrudServiceInMemory implements InMemoryAlternative<Page>, PageCrudService {

    List<Page> pages = new ArrayList<>();

    @Override
    public Page createDocumentationPage(Page page) {
        pages.add(page);
        return page;
    }

    @Override
    public Page updateDocumentationPage(Page pageToUpdate) {
        // Remove page from DB
        pages = pages.stream().filter(p -> !p.getId().equals(pageToUpdate.getId())).collect(Collectors.toList());
        pages.add(pageToUpdate);

        return pageToUpdate;
    }

    @Override
    public Page get(String id) {
        return this.findById(id).orElseThrow(() -> new PageNotFoundException(id));
    }

    @Override
    public Optional<Page> findById(String id) {
        return pages.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    @Override
    public void initWith(List<Page> items) {
        this.reset();
        this.pages.addAll(items);
    }

    @Override
    public void reset() {
        pages = new ArrayList<>();
    }

    @Override
    public List<Page> storage() {
        return ImmutableList.copyOf(pages);
    }
}
