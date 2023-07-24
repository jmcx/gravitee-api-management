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
export class ImagesUtils {
  static fakeImage15x15: string =
    'iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAA71pVKAAAAAXNSR0IArs4c6QAAALVJREFUOE+lk7EOxCAIhmHSxMGkWzv7/k/iIzRu0s2kS+PmhV64a1qNdy2TAf4PRUDvfRmGAZRSUDNEvLhzzpBSAgwhFD4458BaWwUcneu6wjzPwAUxxliMMbujBxAh523b9hZP0wTHQO0G5/iyLF8xX60FqPl3MRGVcRw/zzontoCXykIQAUM5qdaLppghHCQiYAD35GzdyixiwM+Vb7/5724//ufbE/ZotmWrtNbdpZAE2aoXEEvno4RnzD0AAAAASUVORK5CYII=';
  static fakeImage150x35: string =
    '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';

  static async blobToBase64(blob: Blob) {
    return btoa(String.fromCharCode(...new Uint8Array(await blob.arrayBuffer())));
  }
}