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
const ApiCreationStep5Component: ng.IComponentOptions = {
  require: {
    parent: '^apiCreationV2ComponentAjs',
  },
  template: require('./api-creation-step5.html'),
  controller: [
    'Constants',
    function (Constants) {
      if (Constants.env.settings.documentation && Constants.env.settings.documentation.url) {
        this.url = Constants.env.settings.documentation.url;
      } else {
        this.url = 'https://docs.gravitee.io';
      }
    },
  ],
};

export default ApiCreationStep5Component;
