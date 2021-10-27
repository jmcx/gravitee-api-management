/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cloneDeep, sortBy, toString } from 'lodash';

import { GioTableWrapperFilters } from './gio-table-wrapper.component';

export const gioTableFilterCollection = <T extends unknown>(collection: T[], filters: Partial<GioTableWrapperFilters>): T[] => {
  let sortedCollection: T[] = cloneDeep(collection);

  if (filters?.pagination) {
    sortedCollection = sortedCollection.slice(
      (filters.pagination.index - 1) * filters.pagination.size,
      filters.pagination.index * filters.pagination.size,
    );
  }

  if (filters?.searchTerm) {
    sortedCollection = sortedCollection.filter((element) => {
      return Object.values(element).some((value) => toString(value).toLowerCase().includes(filters.searchTerm.toLowerCase()));
    });
  }

  if (filters?.sort) {
    sortedCollection = sortBy(sortedCollection, filters.sort.active);

    if (filters.sort.direction === 'desc') {
      sortedCollection = sortedCollection.reverse();
    }
  }

  return sortedCollection;
};
