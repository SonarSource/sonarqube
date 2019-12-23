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
import { shallow } from 'enzyme';
import * as React from 'react';
import RadioToggle from 'sonar-ui-common/components/controls/RadioToggle';
import Select from 'sonar-ui-common/components/controls/Select';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { HotspotStatusFilter } from '../../../../types/security-hotspots';
import { AssigneeFilterOption, FilterBar, FilterBarProps } from '../FilterBar';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('anonymous');
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot('logged-in');
});

it('should trigger onChange for status', () => {
  const onChangeFilters = jest.fn();
  const wrapper = shallowRender({ onChangeFilters });

  const { onChange } = wrapper.find(Select).props();

  if (!onChange) {
    return fail("Select's onChange should be defined");
  }
  onChange({ value: HotspotStatusFilter.SAFE });
  expect(onChangeFilters).toBeCalledWith({ status: HotspotStatusFilter.SAFE });
});

it('should trigger onChange for self-assigned toggle', () => {
  const onChangeFilters = jest.fn();
  const wrapper = shallowRender({ currentUser: mockLoggedInUser(), onChangeFilters });

  const { onCheck } = wrapper.find(RadioToggle).props();

  if (!onCheck) {
    return fail("RadioToggle's onCheck should be defined");
  }
  onCheck(AssigneeFilterOption.ALL);
  expect(onChangeFilters).toBeCalledWith({ assignedToMe: false });
});

function shallowRender(props: Partial<FilterBarProps> = {}) {
  return shallow(
    <FilterBar
      currentUser={mockCurrentUser()}
      onChangeFilters={jest.fn()}
      filters={{ assignedToMe: false, status: HotspotStatusFilter.TO_REVIEW }}
      {...props}
    />
  );
}
