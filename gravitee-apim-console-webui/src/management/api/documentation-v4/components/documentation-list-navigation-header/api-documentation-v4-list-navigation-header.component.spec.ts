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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiDocumentationV4ListNavigationHeaderComponent } from './api-documentation-v4-list-navigation-header.component';

import { ApiDocumentationV4Module } from '../../api-documentation-v4.module';
import { ApiDocumentationV4BreadcrumbHarness } from '../api-documentation-v4-breadcrumb/api-documentation-v4-breadcrumb.harness';

describe('ApiDocumentationV4NavigationHeaderComponent', () => {
  let fixture: ComponentFixture<ApiDocumentationV4ListNavigationHeaderComponent>;
  let component: ApiDocumentationV4ListNavigationHeaderComponent;
  let harnessLoader: HarnessLoader;

  const init = async () => {
    await TestBed.configureTestingModule({
      declarations: [ApiDocumentationV4ListNavigationHeaderComponent],
      imports: [NoopAnimationsModule, ApiDocumentationV4Module, MatIconTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ApiDocumentationV4ListNavigationHeaderComponent);
    component = fixture.componentInstance;
    harnessLoader = await TestbedHarnessEnvironment.loader(fixture);
  };

  beforeEach(async () => await init());

  it('should show breadcrumb', async () => {
    const breadcrumb = await harnessLoader.getHarness(ApiDocumentationV4BreadcrumbHarness);
    expect(await breadcrumb.getContent()).toEqual('Home');
  });

  it('should emit event when clicking on add button', async () => {
    const spy = jest.spyOn(component.onAddFolder, 'emit');
    const button = await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Add new folder' }));
    await button.click();

    expect(spy).toHaveBeenCalled();
  });
});
