/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption } from '@angular/material/core';
import { MatSelect } from '@angular/material/select';

type JsonSchema = any;

export interface SubscriptionCustomFormData {
  value: Record<string, any> | undefined;
  isValid: boolean;
}

interface FieldVMBase {
  key: string;
  title: string;
  description?: string;
  required: boolean;
}

type FieldVM =
  | (FieldVMBase & { kind: 'string'; format?: string; enum?: { value: string; title: string }[] })
  | (FieldVMBase & { kind: 'number'; integer?: boolean; minimum?: number; maximum?: number })
  | (FieldVMBase & { kind: 'boolean' })
  | (FieldVMBase & { kind: 'multiEnum'; options: { value: string; title: string }[]; minItems?: number; maxItems?: number });

@Component({
  selector: 'app-subscription-custom-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCard,
    MatCardContent,
    MatFormField,
    MatInput,
    MatLabel,
    MatSelect,
    MatOption,
    MatCheckbox,
  ],
  templateUrl: './subscription-custom-form.component.html',
  styleUrl: './subscription-custom-form.component.scss',
})
export class SubscriptionCustomFormComponent implements OnChanges {
  @Input({ required: true })
  schemaJson: string | null = null;

  @Output()
  formDataChange = new EventEmitter<SubscriptionCustomFormData>();

  parseError: string | null = null;
  fields: FieldVM[] = [];

  form = new FormGroup({});

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['schemaJson']) {
      this.buildForm();
    }
  }

  private buildForm(): void {
    this.parseError = null;
    this.fields = [];
    this.form = new FormGroup({});

    if (!this.schemaJson) {
      this.emit();
      return;
    }

    let schema: JsonSchema;
    try {
      schema = JSON.parse(this.schemaJson);
    } catch (e: any) {
      this.parseError = e?.message ?? 'Invalid JSON schema';
      this.emit();
      return;
    }

    const required: string[] = Array.isArray(schema?.required) ? schema.required : [];
    const properties: Record<string, JsonSchema> = schema?.properties ?? {};

    Object.entries(properties).forEach(([key, prop]) => {
      const title = prop?.title ?? key;
      const description = prop?.description;
      const isRequired = required.includes(key);

      const field = this.toFieldVM({ key, title, description, required: isRequired }, prop);
      if (!field) {
        return;
      }
      this.fields.push(field);

      const control = this.toControl(field, prop);
      this.form.addControl(key, control);
    });

    this.form.valueChanges.subscribe(() => this.emit());
    this.emit();
  }

  private emit(): void {
    const isValid = this.parseError == null && this.form.valid;
    this.formDataChange.emit({ value: isValid ? this.form.getRawValue() : this.form.getRawValue(), isValid });
  }

  private toFieldVM(base: FieldVMBase, prop: JsonSchema): FieldVM | null {
    // enum: string enum OR string oneOf
    if (prop?.type === 'string') {
      const enumValues: string[] | undefined = Array.isArray(prop?.enum) ? prop.enum : undefined;
      const oneOf: any[] | undefined = Array.isArray(prop?.oneOf) ? prop.oneOf : undefined;
      if (enumValues) {
        return { ...base, kind: 'string', enum: enumValues.map((v) => ({ value: v, title: v })) };
      }
      if (oneOf) {
        return {
          ...base,
          kind: 'string',
          enum: oneOf.map((o) => ({ value: String(o.const), title: String(o.title ?? o.const) })),
        };
      }
      return { ...base, kind: 'string', format: prop?.format };
    }
    if (prop?.type === 'number' || prop?.type === 'integer') {
      return {
        ...base,
        kind: 'number',
        integer: prop?.type === 'integer',
        minimum: prop?.minimum,
        maximum: prop?.maximum,
      };
    }
    if (prop?.type === 'boolean') {
      return { ...base, kind: 'boolean' };
    }
    if (prop?.type === 'array') {
      const items = prop?.items ?? {};
      const options =
        Array.isArray(items?.enum)
          ? items.enum.map((v: any) => ({ value: String(v), title: String(v) }))
          : Array.isArray(items?.anyOf)
            ? items.anyOf.map((o: any) => ({ value: String(o.const), title: String(o.title ?? o.const) }))
            : [];
      if (!options.length) {
        return null;
      }
      return { ...base, kind: 'multiEnum', options, minItems: prop?.minItems, maxItems: prop?.maxItems };
    }
    return null;
  }

  private toControl(field: FieldVM, prop: JsonSchema): AbstractControl {
    const validators = [];
    if (field.required) {
      validators.push(Validators.required);
    }

    if (field.kind === 'string') {
      if (typeof prop?.minLength === 'number') {
        validators.push(Validators.minLength(prop.minLength));
      }
      if (typeof prop?.maxLength === 'number') {
        validators.push(Validators.maxLength(prop.maxLength));
      }
      if (typeof prop?.pattern === 'string') {
        validators.push(Validators.pattern(prop.pattern));
      }
      if (field.format === 'email') {
        validators.push(Validators.email);
      }
      return new FormControl<string>('', { nonNullable: true, validators });
    }

    if (field.kind === 'number') {
      if (typeof field.minimum === 'number') {
        validators.push(Validators.min(field.minimum));
      }
      if (typeof field.maximum === 'number') {
        validators.push(Validators.max(field.maximum));
      }
      return new FormControl<number | null>(null, { validators });
    }

    if (field.kind === 'boolean') {
      return new FormControl<boolean>(false, { nonNullable: true, validators });
    }

    if (field.kind === 'multiEnum') {
      if (typeof field.minItems === 'number') {
        validators.push(Validators.minLength(field.minItems));
      }
      if (typeof field.maxItems === 'number') {
        validators.push(Validators.maxLength(field.maxItems));
      }
      return new FormControl<string[]>([], { nonNullable: true, validators });
    }

    return new FormControl<any>(null);
  }
}


