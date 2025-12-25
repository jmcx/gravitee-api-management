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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { CreatePortalNextForm, PortalNextForm, PortalNextFormsResponse, UpdatePortalNextForm } from '../entities/management-api-v2';

@Injectable({ providedIn: 'root' })
export class UiPortalFormsService {
  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(): Observable<PortalNextFormsResponse> {
    const url = `${this.constants.env.v2BaseURL}/ui/forms`;
    return this.http.get<PortalNextFormsResponse>(url);
  }

  get(formId: string): Observable<PortalNextForm> {
    return this.http.get<PortalNextForm>(`${this.constants.env.v2BaseURL}/ui/forms/${formId}`);
  }

  create(payload: CreatePortalNextForm): Observable<PortalNextForm> {
    return this.http.post<PortalNextForm>(`${this.constants.env.v2BaseURL}/ui/forms`, payload);
  }

  update(formId: string, payload: UpdatePortalNextForm): Observable<PortalNextForm> {
    return this.http.put<PortalNextForm>(`${this.constants.env.v2BaseURL}/ui/forms/${formId}`, payload);
  }

  delete(formId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/ui/forms/${formId}`);
  }

  activate(formId: string): Observable<PortalNextForm> {
    return this.http.post<PortalNextForm>(`${this.constants.env.v2BaseURL}/ui/forms/${formId}/_activate`, null);
  }
}


