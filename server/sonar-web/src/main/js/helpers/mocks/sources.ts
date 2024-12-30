/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { ComponentQualifier } from '~sonar-aligned/types/component';
import {
  DuplicatedFile,
  Duplication,
  DuplicationBlock,
  SnippetsByComponent,
  SourceLine,
  SourceViewerFile,
} from '../../types/types';

export function mockSourceViewerFile(
  name = 'foo/bar.ts',
  project = 'project',
  override?: Partial<SourceViewerFile>,
): SourceViewerFile {
  return {
    measures: {
      coverage: '85.2',
      duplicationDensity: '1.0',
      issues: '12',
      lines: '56',
    },
    project,
    projectName: 'MyProject',
    q: ComponentQualifier.File,
    uuid: 'foo-bar',
    key: `${project}:${name}`,
    path: name,
    name,
    longName: name,
    fav: false,
    canMarkAsFavorite: true,
    ...override,
  };
}

export function mockSourceLine(overrides: Partial<SourceLine> = {}): SourceLine {
  return {
    line: 16,
    code: '<span class="k">import</span> java.util.<span class="sym-9 sym">ArrayList</span>;',
    coverageStatus: 'covered',
    coveredConditions: 2,
    scmRevision: '80f564becc0c0a1c9abaa006eca83a4fd278c3f0',
    scmAuthor: 'simon.brandhof@sonarsource.com',
    scmDate: '2018-12-11T10:48:39+0100',
    duplicated: false,
    isNew: true,
    ...overrides,
  };
}

export function mockSnippetsByComponent(
  file = 'main.js',
  project = 'project',
  lines: number[] = [16],
): SnippetsByComponent {
  const sources = lines.reduce((lines: { [key: number]: SourceLine }, line) => {
    lines[line] = mockSourceLine({ line });
    return lines;
  }, {});
  return {
    component: mockSourceViewerFile(file, project),
    sources,
  };
}

export function mockDuplicatedFile(overrides: Partial<DuplicatedFile> = {}): DuplicatedFile {
  return {
    key: 'file1.java',
    name: overrides.key || 'file1.java',
    project: 'foo',
    projectName: 'Foo',
    ...overrides,
  };
}

export function mockDuplication(overrides: Partial<Duplication> = {}): Duplication {
  return {
    blocks: [mockDuplicationBlock()],
    ...overrides,
  };
}

export function mockDuplicationBlock(overrides: Partial<DuplicationBlock> = {}): DuplicationBlock {
  return {
    from: 12,
    size: 5,
    ...overrides,
  };
}
