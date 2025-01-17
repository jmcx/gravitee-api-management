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
import { Component, Inject, OnInit } from '@angular/core';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { FormControl, FormGroup } from '@angular/forms';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { Application } from '../../../../../entities/application/application';

@Component({
  selector: 'application-general',
  template: require('./application-general.component.html'),
  styles: [require('./application-general.component.scss')],
})
export class ApplicationGeneralComponent implements OnInit {
  public initialApplication: Application;
  public applicationForm: FormGroup;
  public isLoadingData = true;
  public initialApplicationGeneralFormsValue: unknown;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly applicationService: ApplicationService,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    this.applicationService
      .getById(this.ajsStateParams.applicationId)
      .pipe(
        tap((application) => {
          this.initialApplication = application;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;

        this.applicationForm = new FormGroup({
          details: new FormGroup({
            name: new FormControl(this.initialApplication.name),
            description: new FormControl(this.initialApplication.description),
            domain: new FormControl(this.initialApplication.domain),
            type: new FormControl(this.initialApplication.type),
          }),
          images: new FormGroup({
            picture: new FormControl(this.initialApplication.picture ? [this.initialApplication.picture] : undefined),
            background: new FormControl(this.initialApplication.background ? [this.initialApplication.background] : undefined),
          }),
          OAuth2Form: new FormGroup({
            client_id: new FormControl(
              this.initialApplication.settings?.app?.client_id ? this.initialApplication.settings.app.client_id : undefined,
            ),
          }),
        });

        this.initialApplicationGeneralFormsValue = this.applicationForm.getRawValue();
      });
  }

  onSubmit() {
    const imagesValue = this.applicationForm.getRawValue().images;

    const applicationToUpdate = {
      ...this.initialApplication,
      ...this.applicationForm.getRawValue().details,
      settings:
        this.initialApplication.type === 'SIMPLE'
          ? {
              app: {
                ...this.applicationForm.getRawValue().OAuth2Form,
              },
            }
          : this.initialApplication.settings,
      ...(imagesValue?.picture?.length ? { picture: imagesValue.picture[0].dataUrl } : { picture: null }),
      ...(imagesValue?.background?.length ? { background: imagesValue.background[0].dataUrl } : { background: null }),
    };

    this.applicationService
      .update(applicationToUpdate)
      .pipe(
        tap(() => this.snackBarService.success('Application details successfully updated!')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
