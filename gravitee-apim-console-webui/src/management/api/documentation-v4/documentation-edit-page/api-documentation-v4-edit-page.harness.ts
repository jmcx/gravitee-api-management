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
import { ComponentHarness } from '@angular/cdk/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatRadioGroupHarness } from '@angular/material/radio/testing';

import { ApiDocumentationV4VisibilityHarness } from '../components/api-documentation-v4-visibility/api-documentation-v4-visibility.harness';

export class ApiDocumentationV4EditPageHarness extends ComponentHarness {
  public static hostSelector = 'api-documentation-edit-page';

  private nextButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Next' }));
  private nameInputLocator = this.locatorFor(MatInputHarness);
  private visibilityHarness = this.locatorFor(ApiDocumentationV4VisibilityHarness);
  private sourceRadioHarness = this.locatorFor(MatRadioGroupHarness.with({ selector: '.stepper__content__source' }));

  async getNextButton() {
    return this.nextButtonLocator();
  }

  async getName(): Promise<string> {
    return this.nameInputLocator().then((input) => input.getValue());
  }

  async setName(name: string) {
    return this.nameInputLocator().then((input) => input.setValue(name));
  }

  async checkVisibility(visibility: 'PRIVATE' | 'PUBLIC') {
    const visibilityHarness = await this.visibilityHarness();
    const button =
      visibility === 'PRIVATE' ? await visibilityHarness.getPrivateRadioOption() : await visibilityHarness.getPublicRadioOption();
    return button.check();
  }

  async getVisibility() {
    return this.visibilityHarness().then((harness) => harness.getValue());
  }

  async getSourceRadioGroupHarness() {
    return await this.sourceRadioHarness();
  }
  async getSourceOptions() {
    return Promise.all(await this.sourceRadioHarness().then(async (radioGroup) => await radioGroup.getRadioButtons()));
  }
}
