/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { subDays } from 'date-fns';
import { shallow } from 'enzyme';
import * as React from 'react';
import { toShortNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getProjectActivity } from '../../../../api/projectActivity';
import { mockAnalysis, mockAnalysisEvent } from '../../../../helpers/testMocks';
import BranchAnalysisList from '../BranchAnalysisList';

jest.mock('date-fns/start_of_day', () =>
  jest.fn(() => ({
    getTime: () => '1488322800000' // 2017-03-02
  }))
);

jest.mock('sonar-ui-common/helpers/dates', () => ({
  parseDate: jest.fn().mockReturnValue('2017-03-02'),
  toShortNotSoISOString: jest.fn().mockReturnValue('2017-03-02')
}));

jest.mock('../../../../api/projectActivity', () => ({
  getProjectActivity: jest.fn().mockResolvedValue({
    analyses: []
  })
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  (getProjectActivity as jest.Mock).mockResolvedValue({
    analyses: [
      mockAnalysis({
        key: '4',
        date: '2017-03-02T10:36:01',
        projectVersion: '4.2'
      }),
      mockAnalysis({
        key: '3',
        date: '2017-03-02T09:36:01',
        events: [mockAnalysisEvent()],
        projectVersion: '4.2'
      }),
      mockAnalysis({
        key: '2',
        date: '2017-03-02T08:36:01',
        events: [
          mockAnalysisEvent(),
          mockAnalysisEvent({ category: 'VERSION', qualityGate: undefined })
        ],
        projectVersion: '4.1'
      }),
      mockAnalysis({ key: '1', projectVersion: '4.1' })
    ]
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should reload analyses after range change', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleRangeChange({ value: 30 });

  expect(getProjectActivity).toBeCalledWith({
    branch: 'master',
    project: 'project1',
    from: toShortNotSoISOString(subDays(new Date(), 30))
  });
});

it('should register the badge nodes', () => {
  const wrapper = shallowRender();

  const element = document.createElement('div');

  wrapper.instance().registerBadgeNode('4.3')(element);

  expect(element.getAttribute('originOffsetTop')).not.toBeNull();
});

it('should handle scroll', () => {
  const wrapper = shallowRender();

  wrapper.instance().handleScroll({ currentTarget: { scrollTop: 12 } } as any);

  expect(wrapper.state('scroll')).toBe(12);
});

function shallowRender(props: Partial<BranchAnalysisList['props']> = {}) {
  return shallow<BranchAnalysisList>(
    <BranchAnalysisList
      analysis="analysis1"
      branch="master"
      component="project1"
      onSelectAnalysis={jest.fn()}
      {...props}
    />
  );
}
