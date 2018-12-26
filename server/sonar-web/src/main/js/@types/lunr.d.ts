/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
declare module 'lunr' {
  export interface Lunr {
    add(doc: any): void;

    field(field: string, options?: { boost?: number }): void;

    ref(field: string): void;

    use(fn: Function): void;

    metadataWhitelist?: string[];
  }

  export interface LunrBuilder {
    pipeline: any;
    metadataWhitelist: string[];
  }

  export interface LunrIndex {
    search(query: string): LunrMatch[];
  }

  export interface LunrInit {
    (this: Lunr): void;
  }

  export interface LunrMatch {
    ref: string;
    score: number;
    matchData: { metadata: any };
  }

  export interface LunrToken {
    str: string;
    metadata: any;
  }

  function lunr(initializer: LunrInit): LunrIndex;

  export default lunr;
}
