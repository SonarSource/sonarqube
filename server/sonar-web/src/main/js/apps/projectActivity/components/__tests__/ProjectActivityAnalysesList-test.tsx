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
import { shallow } from 'enzyme';
import * as React from 'react';
import { DEFAULT_GRAPH } from '../../../../components/activity-graph/utils';
import { parseDate } from '../../../../helpers/dates';
import { mockParsedAnalysis } from '../../../../helpers/mocks/project-activity';
import { ComponentQualifier } from '../../../../types/component';
import ProjectActivityAnalysesList from '../ProjectActivityAnalysesList';

jest.mock('date-fns', () => {
  const actual = jest.requireActual('date-fns');
  return {
    ...actual,
    startOfDay: (date: Date) => {
      const startDay = new Date(date);
      startDay.setUTCHours(0, 0, 0, 0);
      return startDay;
    },
  };
});

jest.mock('../../../../helpers/dates', () => {
  const actual = jest.requireActual('../../../../helpers/dates');
  return { ...actual, toShortNotSoISOString: (date: string) => 'ISO.' + date };
});

const DATE = parseDate('2016-10-27T16:33:50+0000');

const DEFAULT_QUERY = {
  category: '',
  customMetrics: [],
  graph: DEFAULT_GRAPH,
  project: 'org.sonarsource.sonarqube:sonarqube',
};

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ project: { qualifier: ComponentQualifier.Application } })).toMatchSnapshot(
    'application'
  );
  expect(shallowRender({ analyses: [], initializing: true })).toMatchSnapshot('loading');
  expect(shallowRender({ analyses: [] })).toMatchSnapshot('no analyses');
});

it('should correctly filter analyses by category', () => {
  const wrapper = shallowRender();
  wrapper.setProps({ query: { ...DEFAULT_QUERY, category: 'QUALITY_GATE' } });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly filter analyses by date range', () => {
  const wrapper = shallowRender();
  wrapper.setProps({
    query: {
      ...DEFAULT_QUERY,
      from: DATE,
      to: DATE,
    },
  });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly update the selected date', () => {
  const selectedDate = new Date();
  const updateQuery = jest.fn();
  const wrapper = shallowRender({ updateQuery });
  wrapper.instance().updateSelectedDate(selectedDate);
  expect(updateQuery).toHaveBeenCalledWith({ selectedDate });
});

it('should correctly reset scroll if filters change', () => {
  const wrapper = shallowRender();
  const scrollContainer = document.createElement('ul');
  scrollContainer.scrollTop = 100;

  // Saves us a call to mount().
  wrapper.instance().scrollContainer = scrollContainer;

  wrapper.setProps({ query: { ...DEFAULT_QUERY, category: 'OTHER' } });
  expect(scrollContainer.scrollTop).toBe(0);
});

function shallowRender(props: Partial<ProjectActivityAnalysesList['props']> = {}) {
  return shallow<ProjectActivityAnalysesList>(
    <ProjectActivityAnalysesList
      addCustomEvent={jest.fn().mockResolvedValue(undefined)}
      addVersion={jest.fn().mockResolvedValue(undefined)}
      analyses={[
        mockParsedAnalysis({
          key: 'A1',
          date: DATE,
          events: [{ key: 'E1', category: 'VERSION', name: '6.5-SNAPSHOT' }],
        }),
        mockParsedAnalysis({ key: 'A2', date: parseDate('2016-10-27T12:21:15+0000') }),
        mockParsedAnalysis({
          key: 'A3',
          date: parseDate('2016-10-26T12:17:29+0000'),
          events: [
            { key: 'E2', category: 'VERSION', name: '6.4' },
            { key: 'E3', category: 'OTHER', name: 'foo' },
          ],
        }),
        mockParsedAnalysis({
          key: 'A4',
          date: parseDate('2016-10-24T16:33:50+0000'),
          events: [{ key: 'E1', category: 'QUALITY_GATE', name: 'Quality gate changed to red...' }],
        }),
      ]}
      analysesLoading={false}
      canAdmin={false}
      changeEvent={jest.fn().mockResolvedValue(undefined)}
      deleteAnalysis={jest.fn().mockResolvedValue(undefined)}
      deleteEvent={jest.fn().mockResolvedValue(undefined)}
      initializing={false}
      leakPeriodDate={parseDate('2016-10-27T12:21:15+0000')}
      project={{ qualifier: ComponentQualifier.Project }}
      query={DEFAULT_QUERY}
      updateQuery={jest.fn()}
      {...props}
    />
  );
}
