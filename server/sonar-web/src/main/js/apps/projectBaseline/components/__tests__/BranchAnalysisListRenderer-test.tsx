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
import { mockAnalysisEvent, mockParsedAnalysis } from '../../../../helpers/mocks/project-activity';
import BranchAnalysisListRenderer, {
  BranchAnalysisListRendererProps,
} from '../BranchAnalysisListRenderer';

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
  return { ...actual, toShortNotSoISOString: (date: string) => `ISO.${date}` };
});

const analyses = [
  mockParsedAnalysis({
    key: '4',
    date: new Date('2017-03-02T10:36:01Z'),
    projectVersion: '4.2',
  }),
  mockParsedAnalysis({
    key: '3',
    date: new Date('2017-03-02T09:36:01Z'),
    events: [mockAnalysisEvent()],
    projectVersion: '4.2',
  }),
  mockParsedAnalysis({
    key: '2',
    date: new Date('2017-03-02T08:36:01Z'),
    events: [
      mockAnalysisEvent(),
      mockAnalysisEvent({ category: 'VERSION', qualityGate: undefined }),
    ],
    projectVersion: '4.1',
  }),
  mockParsedAnalysis({ key: '1', projectVersion: '4.1' }),
];

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('empty');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ analyses, selectedAnalysisKey: '2' })).toMatchSnapshot('Analyses');
});

function shallowRender(props: Partial<BranchAnalysisListRendererProps> = {}) {
  return shallow(
    <BranchAnalysisListRenderer
      analyses={[]}
      handleRangeChange={jest.fn()}
      handleScroll={jest.fn()}
      loading={false}
      onSelectAnalysis={jest.fn()}
      range={30}
      registerBadgeNode={jest.fn()}
      registerScrollableNode={jest.fn()}
      selectedAnalysisKey=""
      shouldStick={jest.fn()}
      {...props}
    />
  );
}
