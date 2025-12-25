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
package io.gravitee.rest.api.portal.rest.resource.param;

import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class PaginationParam {

    public static final String PAGE_QUERY_PARAM_NAME = "page";
    public static final String SIZE_QUERY_PARAM_NAME = "size";

    private static final String PAGE_QUERY_PARAM_DEFAULT = "1";
    private static final String SIZE_QUERY_PARAM_DEFAULT = "10";

    @DefaultValue(PAGE_QUERY_PARAM_DEFAULT)
    @QueryParam(PAGE_QUERY_PARAM_NAME)
    @Min(1)
    Integer page;

    @DefaultValue(SIZE_QUERY_PARAM_DEFAULT)
    @QueryParam(SIZE_QUERY_PARAM_NAME)
    @Min(-1)
    Integer size;

    public PaginationParam() {}

    public PaginationParam(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Integer page;
        private Integer size;

        private Builder() {}

        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public PaginationParam build() {
            return new PaginationParam(page, size);
        }
    }

    public boolean hasPagination() {
        return this.getSize() != null && !this.getSize().equals(-1);
    }
}
