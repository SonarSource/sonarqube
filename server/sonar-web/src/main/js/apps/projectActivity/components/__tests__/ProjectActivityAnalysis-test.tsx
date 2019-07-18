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
import { shallow } from 'enzyme';
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockAnalysisEvent, mockParsedAnalysis } from '../../../../helpers/testMocks';
import ProjectActivityAnalysis from '../ProjectActivityAnalysis';

jest.mock('sonar-ui-common/helpers/dates', () => ({
  parseDate: () => ({
    valueOf: () => 1546333200000,
    toISOString: () => '2019-01-01T09:00:00.000Z'
  })
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({ analysis: mockParsedAnalysis({ events: [mockAnalysisEvent()] }) })
  ).toMatchSnapshot();
  expect(
    shallowRender({ analysis: mockParsedAnalysis({ buildString: '1.0.234' }) })
  ).toMatchSnapshot();
});

it('should show the correct admin options', () => {
  const wrapper = shallowRender({
    canAdmin: true,
    canCreateVersion: true,
    canDeleteAnalyses: true
  });
  const instance = wrapper.instance();

  expect(wrapper).toMatchSnapshot();

  instance.setState({ addEventForm: true });
  waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  instance.setState({ addEventForm: false });

  instance.setState({ removeAnalysisForm: true });
  waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  instance.setState({ removeAnalysisForm: false });

  instance.setState({ addVersionForm: true });
  waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  instance.setState({ addVersionForm: false });
});

it('should not allow the first item to be deleted', () => {
  expect(
    shallowRender({
      canAdmin: true,
      canCreateVersion: true,
      canDeleteAnalyses: true,
      isFirst: true
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<ProjectActivityAnalysis['props']> = {}) {
  return shallow(
    <ProjectActivityAnalysis
      addCustomEvent={jest.fn()}
      addVersion={jest.fn()}
      analysis={mockParsedAnalysis()}
      canCreateVersion={false}
      changeEvent={jest.fn()}
      deleteAnalysis={jest.fn()}
      deleteEvent={jest.fn()}
      isFirst={false}
      selected={false}
      updateSelectedDate={jest.fn()}
      {...props}
    />
  );
}
