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
import { mockFlowLocation } from '../../../helpers/testMocks';
import { click } from '../../../helpers/testUtils';
import { FlowLocation } from '../../../types/types';
import CrossFileLocationsNavigator from '../CrossFileLocationNavigator';

const location1: FlowLocation = {
  component: 'foo',
  componentName: 'src/foo.js',
  msg: 'Do not use foo',
  textRange: { startLine: 7, endLine: 7, startOffset: 5, endOffset: 8 },
};

const location2: FlowLocation = {
  component: 'foo',
  componentName: 'src/foo.js',
  msg: 'Do not use foo',
  textRange: { startLine: 8, endLine: 8, startOffset: 0, endOffset: 5 },
};

const location3: FlowLocation = {
  component: 'bar',
  componentName: 'src/bar.js',
  msg: 'Do not use bar',
  textRange: { startLine: 15, endLine: 16, startOffset: 4, endOffset: 6 },
};

const location4: FlowLocation = {
  component: 'bar',
  componentName: 'src/bar.js',
  msg: 'Do not use bar',
  textRange: { startLine: 17, endLine: 18, startOffset: 7, endOffset: 9 },
};

it('should render with no locations', () => {
  expect(shallowRender({ locations: [] })).toMatchSnapshot();
});

it('should render locations with no component name', () => {
  expect(shallowRender({ locations: [mockFlowLocation({ componentName: '' })] })).toMatchSnapshot();
});

it('should render', () => {
  const wrapper = shallowRender();

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('SingleFileLocationNavigator').length).toBe(2);

  click(wrapper.find('.location-file-more'));
  expect(wrapper.find('SingleFileLocationNavigator').length).toBe(4);
});

it('should render all locations', () => {
  const wrapper = shallowRender({ locations: [location1, location2, location3] });

  expect(wrapper.find('SingleFileLocationNavigator').length).toBe(3);
});

it('should expand all locations', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('SingleFileLocationNavigator').length).toBe(2);

  wrapper.setProps({ selectedLocationIndex: 1 });
  expect(wrapper.find('SingleFileLocationNavigator').length).toBe(4);
});

function shallowRender(props: Partial<CrossFileLocationsNavigator['props']> = {}) {
  return shallow<CrossFileLocationsNavigator>(
    <CrossFileLocationsNavigator
      locations={[location1, location2, location3, location4]}
      onLocationSelect={jest.fn()}
      selectedLocationIndex={undefined}
      {...props}
    />
  );
}
