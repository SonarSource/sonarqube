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
import {
  Analysis,
  AnalysisEvent,
  HistoryItem,
  MeasureHistory,
  ParsedAnalysis,
} from '../../types/project-activity';
import { parseDate } from '../dates';

export function mockAnalysis(overrides: Partial<Analysis> = {}): Analysis {
  return {
    date: '2017-03-01T09:36:01+0100',
    events: [],
    key: 'foo',
    projectVersion: '1.0',
    ...overrides,
  };
}

export function mockParsedAnalysis(overrides: Partial<ParsedAnalysis> = {}): ParsedAnalysis {
  return {
    date: new Date('2017-03-01T09:37:01+0100'),
    events: [],
    key: 'foo',
    projectVersion: '1.0',
    ...overrides,
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
          branch: 'master',
        },
        {
          key: 'bar',
          name: 'Bar',
          branch: 'feature/bar',
        },
      ],
    },
    ...overrides,
  };
}

export function mockMeasureHistory(overrides: Partial<MeasureHistory> = {}): MeasureHistory {
  return {
    metric: 'code_smells',
    history: [
      mockHistoryItem(),
      mockHistoryItem({ date: parseDate('2018-10-27T12:21:15+0200'), value: '1749' }),
      mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200'), value: '500' }),
    ],
    ...overrides,
  };
}

export function mockHistoryItem(overrides: Partial<HistoryItem> = {}): HistoryItem {
  return {
    date: parseDate('2016-10-26T12:17:29+0200'),
    value: '2286',
    ...overrides,
  };
}
