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
import { CreateApiMember, Member, MembersResponse, UpdateApiMember } from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class ApiMemberV2Service {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getMembers(api: string): Observable<MembersResponse> {
    return this.http.get<MembersResponse>(`${this.constants.env.v2BaseURL}/apis/${api}/members`);
  }

  addMember(api: string, membership: CreateApiMember): Observable<Member> {
    return this.http.post<Member>(`${this.constants.env.v2BaseURL}/apis/${api}/members`, membership);
  }

  updateMember(api: string, membership: UpdateApiMember): Observable<Member> {
    return this.http.put<Member>(`${this.constants.env.v2BaseURL}/apis/${api}/members/${membership.memberId}`, membership);
  }

  deleteMember(api: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/apis/${api}/members/${memberId}`);
  }

  // TODO add this when needed
  // transferOwnership(api: string, ownership: ApiMembership): Observable<any> {
  //   return this.http.post(`${this.constants.env.v2BaseURL}/apis/${api}/members/transfer_ownership`, ownership);
  // }
}