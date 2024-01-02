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
import { mockIssue } from '../../../helpers/testMocks';
import { FlowLocation } from '../../../types/types';
import LocationsList from '../LocationsList';

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

it('should render locations in the same file', () => {
  const locations = [location1, location2];
  expect(shallowRender({ locations })).toMatchSnapshot();
});

it('should not render locations', () => {
  const wrapper = shallowRender({ locations: [] });
  expect(wrapper.type()).toBeNull();
});

function shallowRender(overrides: Partial<LocationsList['props']> = {}) {
  return shallow<LocationsList>(
    <LocationsList
      locations={mockIssue().secondaryLocations}
      componentKey="foo"
      onLocationSelect={jest.fn()}
      selectedLocationIndex={undefined}
      {...overrides}
    />
  );
}
