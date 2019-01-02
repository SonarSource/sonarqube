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
import * as React from 'react';
import { shallow } from 'enzyme';
import ConciseIssueLocationsNavigator from '../ConciseIssueLocationsNavigator';

const location1: T.FlowLocation = {
  component: 'foo',
  componentName: 'src/foo.js',
  msg: 'Do not use foo',
  textRange: { startLine: 7, endLine: 7, startOffset: 5, endOffset: 8 }
};

const location2: T.FlowLocation = {
  component: 'foo',
  componentName: 'src/foo.js',
  msg: 'Do not use foo',
  textRange: { startLine: 8, endLine: 8, startOffset: 0, endOffset: 5 }
};

const location3: T.FlowLocation = {
  component: 'bar',
  componentName: 'src/bar.js',
  msg: 'Do not use bar',
  textRange: { startLine: 15, endLine: 16, startOffset: 4, endOffset: 6 }
};

it('should render secondary locations in the same file', () => {
  const issue = {
    component: 'foo',
    key: '',
    flows: [],
    secondaryLocations: [location1, location2]
  };
  expect(
    shallow(
      <ConciseIssueLocationsNavigator
        issue={issue}
        onLocationSelect={jest.fn()}
        scroll={jest.fn()}
        selectedFlowIndex={undefined}
        selectedLocationIndex={undefined}
      />
    )
  ).toMatchSnapshot();
});

it('should render flow locations in the same file', () => {
  const issue = {
    component: 'foo',
    key: '',
    flows: [[location1, location2]],
    secondaryLocations: []
  };
  expect(
    shallow(
      <ConciseIssueLocationsNavigator
        issue={issue}
        onLocationSelect={jest.fn()}
        scroll={jest.fn()}
        selectedFlowIndex={undefined}
        selectedLocationIndex={undefined}
      />
    )
  ).toMatchSnapshot();
});

it('should render selected flow locations in the same file', () => {
  const issue = {
    component: 'foo',
    key: '',
    flows: [[location1, location2]],
    secondaryLocations: [location1]
  };
  expect(
    shallow(
      <ConciseIssueLocationsNavigator
        issue={issue}
        onLocationSelect={jest.fn()}
        scroll={jest.fn()}
        selectedFlowIndex={0}
        selectedLocationIndex={undefined}
      />
    )
  ).toMatchSnapshot();
});

it('should render flow locations in different file', () => {
  const issue = {
    component: 'foo',
    key: '',
    flows: [[location1, location3]],
    secondaryLocations: []
  };
  expect(
    shallow(
      <ConciseIssueLocationsNavigator
        issue={issue}
        onLocationSelect={jest.fn()}
        scroll={jest.fn()}
        selectedFlowIndex={undefined}
        selectedLocationIndex={undefined}
      />
    )
  ).toMatchSnapshot();
});

it('should not render locations', () => {
  const issue = { component: 'foo', key: '', flows: [], secondaryLocations: [] };
  const wrapper = shallow(
    <ConciseIssueLocationsNavigator
      issue={issue}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedFlowIndex={undefined}
      selectedLocationIndex={undefined}
    />
  );
  expect(wrapper.type()).toBeNull();
});
