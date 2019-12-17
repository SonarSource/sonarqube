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
import Select from 'sonar-ui-common/components/controls/Select';
import { HotspotStatusFilters } from '../../../../types/security-hotspots';
import FilterBar, { FilterBarProps } from '../FilterBar';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should trigger onChange', () => {
  const onChangeStatus = jest.fn();
  const wrapper = shallowRender({ onChangeStatus });

  const { onChange } = wrapper.find(Select).props();

  if (!onChange) {
    return fail("Select's onChange should be defined");
  }
  onChange({ value: HotspotStatusFilters.SAFE });
  expect(onChangeStatus).toBeCalledWith(HotspotStatusFilters.SAFE);
});

function shallowRender(props: Partial<FilterBarProps> = {}) {
  return shallow(
    <FilterBar
      onChangeStatus={jest.fn()}
      statusFilter={HotspotStatusFilters.TO_REVIEW}
      {...props}
    />
  );
}
