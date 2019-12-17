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
import MultiSelect from '../../common/MultiSelect';
import AddGraphMetricPopup, { AddGraphMetricPopupProps } from '../AddGraphMetricPopup';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly whith 6+ selected elements', () => {
  const selectedElements = ['1', '2', '3', '4', '5', '6'];
  expect(shallowRender({ selectedElements })).toMatchSnapshot();
});

it('should render correctly with type filter', () => {
  const metricsTypeFilter = ['filter1', 'filter2'];
  expect(shallowRender({ metricsTypeFilter })).toMatchSnapshot();
});

it('should prevent selection of unknown element', () => {
  const elements = ['1', '2', '3'];
  const onSelect = jest.fn();
  const wrapper = shallowRender({ elements, onSelect });
  wrapper
    .find(MultiSelect)
    .props()
    .onSelect('unknown');

  expect(onSelect).not.toHaveBeenCalled();
});

function shallowRender(overrides: Partial<AddGraphMetricPopupProps> = {}) {
  return shallow(
    <AddGraphMetricPopup
      elements={[]}
      filterSelected={jest.fn()}
      onSearch={jest.fn()}
      onSelect={jest.fn()}
      onUnselect={jest.fn()}
      renderLabel={element => element}
      selectedElements={[]}
      {...overrides}
    />
  );
}
