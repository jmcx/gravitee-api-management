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
import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioMonacoEditorModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { EMPTY, of } from 'rxjs';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { NewPortalBadgeComponent } from '../components/portal-badge/new-portal-badge/new-portal-badge.component';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { UiPortalFormsService } from '../../services-ngx/ui-forms.service';
import { PortalNextForm } from '../../entities/management-api-v2';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

const DEFAULT_EXAMPLE_SCHEMA = `{
  "type": "object",
  "title": "Subscription form",
  "properties": {
    "fullName": {
      "type": "string",
      "title": "Full name",
      "minLength": 3,
      "maxLength": 50
    },
    "email": {
      "type": "string",
      "title": "Email",
      "format": "email"
    },
    "employees": {
      "type": "integer",
      "title": "Number of employees",
      "minimum": 1,
      "maximum": 100000
    },
    "isProduction": {
      "type": "boolean",
      "title": "Production usage",
      "default": false
    },
    "region": {
      "type": "string",
      "title": "Region",
      "enum": ["EU", "US", "APAC"],
      "default": "EU"
    },
    "color": {
      "type": "string",
      "title": "Preferred color",
      "oneOf": [
        { "const": "#FF0000", "title": "Red" },
        { "const": "#00FF00", "title": "Green" },
        { "const": "#0000FF", "title": "Blue" }
      ]
    },
    "features": {
      "type": "array",
      "title": "Requested features",
      "minItems": 1,
      "items": {
        "x-gravitee-dictionary": "subscription_features"
      }
    }
  },
  "required": ["fullName", "email"]
}`;

@Component({
  selector: 'portal-forms',
  templateUrl: './portal-forms.component.html',
  styleUrls: ['./portal-forms.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    GioMonacoEditorModule,
    GioSaveBarModule,
    PortalHeaderComponent,
    NewPortalBadgeComponent,
  ],
  standalone: true,
})
export class PortalFormsComponent implements OnInit {
  forms: WritableSignal<PortalNextForm[]> = signal([]);
  selectedFormId: WritableSignal<string | null> = signal(null);
  isReadOnly: boolean = true;

  formGroup = new FormGroup({
    name: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl<string>(''),
    schema: new FormControl<string>(DEFAULT_EXAMPLE_SCHEMA, { nonNullable: true, validators: [Validators.required] }),
  });

  schemaJsonError = signal<string | null>(null);
  private destroyRef = inject(DestroyRef);

  private formValue = toSignal(this.formGroup.valueChanges.pipe(map(() => this.formGroup.getRawValue())), {
    initialValue: this.formGroup.getRawValue(),
  });

  constructor(
    private readonly uiPortalFormsService: UiPortalFormsService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isReadOnly = !this.permissionService.hasAnyMatching(['environment-settings-u']);
    if (this.isReadOnly) {
      this.formGroup.disable();
    }
    this.reload();
  }

  reload(): void {
    this.uiPortalFormsService
      .list()
      .pipe(
        map((resp) => resp.data ?? []),
        tap((forms) => {
          this.forms.set(forms);
          // auto-select active form if nothing selected
          const currentSelected = this.selectedFormId();
          if (!currentSelected) {
            const active = forms.find((f) => f.active);
            if (active) {
              this.selectForm(active.id);
            }
          }
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  selectForm(formId: string): void {
    const form = this.forms().find((f) => f.id === formId);
    if (!form) {
      return;
    }
    this.selectedFormId.set(formId);
    this.formGroup.reset({
      name: form.name,
      description: form.description ?? '',
      schema: form.schema,
    });
    this.schemaJsonError.set(null);
  }

  newForm(): void {
    this.selectedFormId.set(null);
    this.formGroup.reset({
      name: 'New form',
      description: '',
      schema: DEFAULT_EXAMPLE_SCHEMA,
    });
    this.schemaJsonError.set(null);
  }

  activate(formId: string): void {
    if (this.isReadOnly) {
      return;
    }
    this.uiPortalFormsService
      .activate(formId)
      .pipe(
        tap(() => this.snackBarService.success('Active form updated')),
        tap(() => this.reload()),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  delete(formId: string): void {
    if (this.isReadOnly) {
      return;
    }
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Delete form',
          content: 'This will permanently delete the form.',
          confirmButton: 'Delete',
        },
      })
      .afterClosed()
      .pipe(
        switchMap((confirmed) => {
          if (!confirmed) {
            return EMPTY;
          }
          return this.uiPortalFormsService.delete(formId).pipe(
            tap(() => this.snackBarService.success('Form deleted')),
            tap(() => {
              if (this.selectedFormId() === formId) {
                this.selectedFormId.set(null);
                this.newForm();
              }
              this.reload();
            }),
          );
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  save(): void {
    if (this.isReadOnly) {
      return;
    }
    this.schemaJsonError.set(null);
    const payload = this.formGroup.getRawValue();

    // quick client-side JSON validation (backend also validates)
    try {
      JSON.parse(payload.schema);
    } catch (e: any) {
      this.schemaJsonError.set(e?.message ?? 'Invalid JSON');
      return;
    }

    const selectedId = this.selectedFormId();
    const call$ = selectedId
      ? this.uiPortalFormsService.update(selectedId, payload)
      : this.uiPortalFormsService.create(payload);

    call$
      .pipe(
        tap((saved) => {
          this.snackBarService.success('Form saved');
          this.selectedFormId.set(saved.id);
        }),
        tap(() => this.reload()),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  hasUnsavedChanges(): boolean {
    const selectedId = this.selectedFormId();
    if (!selectedId) {
      // new form: treat as dirty if name/schema differs from default
      const current = this.formGroup.getRawValue();
      return current.name !== 'New form' || current.description !== '' || current.schema !== DEFAULT_EXAMPLE_SCHEMA;
    }
    const selected = this.forms().find((f) => f.id === selectedId);
    if (!selected) {
      return false;
    }
    const current = this.formGroup.getRawValue();
    return (
      current.name !== selected.name ||
      (current.description ?? '') !== (selected.description ?? '') ||
      current.schema !== selected.schema
    );
  }
}


