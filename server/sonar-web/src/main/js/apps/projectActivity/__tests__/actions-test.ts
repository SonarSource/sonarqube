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
import * as actions from '../actions';
import { parseDate } from '../../../helpers/dates';

const ANALYSES = [
  {
    key: 'A1',
    date: parseDate('2016-10-27T16:33:50+0200'),
    events: [
      {
        key: 'E1',
        category: 'VERSION',
        name: '6.5-SNAPSHOT'
      }
    ]
  },
  {
    key: 'A2',
    date: parseDate('2016-10-27T12:21:15+0200'),
    events: []
  },
  {
    key: 'A3',
    date: parseDate('2016-10-26T12:17:29+0200'),
    events: [
      {
        key: 'E2',
        category: 'OTHER',
        name: 'foo'
      },
      {
        key: 'E3',
        category: 'OTHER',
        name: 'foo'
      }
    ]
  }
];

const newEvent = {
  key: 'Enew',
  name: 'Foo',
  category: 'Custom'
};

const emptyState = {
  analyses: [],
  analysesLoading: false,
  graphLoading: false,
  initialized: true,
  measuresHistory: [],
  measures: [],
  metrics: [],
  query: { category: '', graph: '', project: '', customMetrics: [] }
};

const state = { ...emptyState, analyses: ANALYSES };

it('should never throw when there is no analyses', () => {
  expect(actions.addCustomEvent('A1', newEvent)(emptyState).analyses).toHaveLength(0);
  expect(actions.deleteEvent('A1', 'Enew')(emptyState).analyses).toHaveLength(0);
  expect(actions.changeEvent('A1', newEvent)(emptyState).analyses).toHaveLength(0);
  expect(actions.deleteAnalysis('Anew')(emptyState).analyses).toHaveLength(0);
});

describe('addCustomEvent', () => {
  it('should correctly add a custom event', () => {
    expect(actions.addCustomEvent('A2', newEvent)(state).analyses[1]).toMatchSnapshot();
    expect(actions.addCustomEvent('A1', newEvent)(state).analyses[0].events).toContain(newEvent);
  });
});

describe('deleteEvent', () => {
  it('should correctly remove an event', () => {
    expect(actions.deleteEvent('A1', 'E1')(state).analyses[0]).toMatchSnapshot();
    expect(actions.deleteEvent('A2', 'E1')(state).analyses[1]).toMatchSnapshot();
    expect(actions.deleteEvent('A3', 'E2')(state).analyses[2]).toMatchSnapshot();
  });
});

describe('changeEvent', () => {
  it('should correctly update an event', () => {
    expect(
      actions.changeEvent('A1', { key: 'E1', name: 'changed', category: 'VERSION' })(state)
        .analyses[0]
    ).toMatchSnapshot();
    expect(
      actions.changeEvent('A2', { key: 'E2', name: 'foo', category: 'VERSION' })(state).analyses[1]
        .events
    ).toHaveLength(0);
  });
});

describe('deleteAnalysis', () => {
  it('should correctly delete an analyses', () => {
    expect(actions.deleteAnalysis('A1')(state).analyses).toMatchSnapshot();
    expect(actions.deleteAnalysis('A5')(state).analyses).toHaveLength(3);
    expect(actions.deleteAnalysis('A2')(state).analyses).toHaveLength(2);
  });
});
