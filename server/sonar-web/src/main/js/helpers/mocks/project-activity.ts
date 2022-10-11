/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import { Analysis, AnalysisEvent, ParsedAnalysis } from '../../types/project-activity';

export function mockAnalysis(overrides: Partial<Analysis> = {}): Analysis {
  return {
    date: '2017-03-01T09:36:01+0100',
    events: [],
    key: 'foo',
    projectVersion: '1.0',
    ...overrides
  };
}

export function mockParsedAnalysis(overrides: Partial<ParsedAnalysis> = {}): ParsedAnalysis {
  return {
    date: new Date('2017-03-01T09:37:01+0100'),
    events: [],
    key: 'foo',
    projectVersion: '1.0',
    ...overrides
  };
}

export function mockAnalysisEvent(overrides: Partial<AnalysisEvent> = {}): AnalysisEvent {
  return {
    category: 'QUALITY_GATE',
    key: 'E11',
    description: 'Lorem ipsum dolor sit amet',
    name: 'Lorem ipsum',
    qualityGate: {
      status: 'ERROR',
      stillFailing: true,
      failing: [
        {
          key: 'foo',
          name: 'Foo',
          branch: 'master'
        },
        {
          key: 'bar',
          name: 'Bar',
          branch: 'feature/bar'
        }
      ]
    },
    ...overrides
  };
}
