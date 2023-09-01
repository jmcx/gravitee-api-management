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
package io.gravitee.repository.elasticsearch.v4.log.adapter;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.gravitee.repository.elasticsearch.log.adapter.SearchConnectionLogQueryAdapter;
import io.gravitee.repository.log.v4.model.ConnectionLogQuery;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SearchConnectionLogQueryAdapterTest {

    @ParameterizedTest
    @MethodSource("noFilter")
    void should_build_query_without_filter(ConnectionLogQuery.Filter filter) {
        var result = SearchConnectionLogQueryAdapter.adapt(ConnectionLogQuery.builder().page(1).size(20).filter(filter).build());

        assertThatJson(result)
            .isEqualTo(
                """
                        {
                          "from": 0,
                          "size": 20,
                          "sort": {
                            "@timestamp": { "order": "desc", "format": "strict_date_optional_time_nanos"}
                          }
                        }
                        """
            );
    }

    @Test
    void should_build_query_with_filters() {
        var result = SearchConnectionLogQueryAdapter.adapt(
            ConnectionLogQuery
                .builder()
                .page(1)
                .size(20)
                .filter(ConnectionLogQuery.Filter.builder().appId("f1608475-dd77-4603-a084-75dd775603e9").build())
                .build()
        );

        assertThatJson(result)
            .isEqualTo(
                """
                        {
                          "from": 0,
                          "size": 20,
                          "query": {
                            "term": { "api-id": "f1608475-dd77-4603-a084-75dd775603e9" }
                          },
                          "sort": {
                            "@timestamp": { "order": "desc", "format": "strict_date_optional_time_nanos"}
                          }
                        }
                        """
            );
    }

    @Test
    void should_build_query_asking_another_page() {
        var result = SearchConnectionLogQueryAdapter.adapt(ConnectionLogQuery.builder().page(3).size(10).build());

        assertThatJson(result)
            .isEqualTo(
                """
                        {
                          "from": 20,
                          "size": 10,
                          "sort": {
                            "@timestamp": { "order": "desc", "format": "strict_date_optional_time_nanos"}
                          }
                        }
                        """
            );
    }

    private static Stream<Arguments> noFilter() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(ConnectionLogQuery.Filter.builder().build()));
    }
}