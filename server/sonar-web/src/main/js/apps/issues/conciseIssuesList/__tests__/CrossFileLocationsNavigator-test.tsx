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
import CrossFileLocationsNavigator from '../CrossFileLocationsNavigator';
import { click } from '../../../../helpers/testUtils';

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

it('should render', () => {
  const wrapper = shallow(
    <CrossFileLocationsNavigator
      issue={{ key: 'abcd' }}
      locations={[location1, location2, location3]}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedLocationIndex={undefined}
    />
  );
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(2);

  click(wrapper.find('.concise-issue-location-file-more'));
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(3);
});

it('should render all locations', () => {
  const wrapper = shallow(
    <CrossFileLocationsNavigator
      issue={{ key: 'abcd' }}
      locations={[location1, location2]}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedLocationIndex={undefined}
    />
  );
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(2);
});

it('should expand all locations', () => {
  const wrapper = shallow(
    <CrossFileLocationsNavigator
      issue={{ key: 'abcd' }}
      locations={[location1, location2, location3]}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedLocationIndex={undefined}
    />
  );
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(2);

  wrapper.setProps({ selectedLocationIndex: 1 });
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(3);
});

it('should collapse locations when issue changes', () => {
  const wrapper = shallow(
    <CrossFileLocationsNavigator
      issue={{ key: 'abcd' }}
      locations={[location1, location2, location3]}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedLocationIndex={undefined}
    />
  );
  wrapper.setProps({ selectedLocationIndex: 1 });
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(3);

  wrapper.setProps({ issue: { key: 'def' }, selectedLocationIndex: undefined });
  expect(wrapper.find('ConciseIssueLocationsNavigatorLocation').length).toBe(2);
});
