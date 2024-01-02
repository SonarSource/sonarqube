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
import { GroupTypeBase } from 'react-select';
import { mockReactSelectOptionProps } from '../../../../helpers/mocks/react-select';
import { click } from '../../../../helpers/testUtils';
import ProjectsSortingSelect, { Option } from '../ProjectsSortingSelect';

it('should render correctly for overall view', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly for leak view', () => {
  expect(
    shallowRender({ defaultOption: 'analysis_date', selectedSort: 'new_coverage', view: 'leak' })
  ).toMatchSnapshot();
});

it('should handle the descending sort direction', () => {
  expect(shallowRender({ selectedSort: '-vulnerability' })).toMatchSnapshot();
});

it('should render option correctly', () => {
  const wrapper = shallowRender();
  const SortOption = wrapper.instance().projectsSortingSelectOption;
  expect(
    shallow(
      <SortOption
        {...mockReactSelectOptionProps<Option, false, GroupTypeBase<Option>>({
          label: 'foo',
          value: 'foo',
          short: 'fo',
        })}
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <SortOption
        {...mockReactSelectOptionProps<Option, false, GroupTypeBase<Option>>({
          label: 'foo',
          value: 'foo',
        })}
      />
    )
  ).toMatchSnapshot();
});

it('changes sorting', () => {
  const onChange = jest.fn();
  const instance = shallowRender({
    selectedSort: '-vulnerability',
    onChange,
  }).instance() as ProjectsSortingSelect;
  instance.handleSortChange({ label: 'size', value: 'size' });
  expect(onChange).toHaveBeenCalledWith('size', true);
});

it('reverses sorting', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ selectedSort: '-size', onChange });
  click(wrapper.find('ButtonIcon'));
  expect(onChange).toHaveBeenCalledWith('size', false);

  const node = document.createElement('div');
  node.focus = jest.fn();
  wrapper.instance().sortOrderButtonNode = node;

  click(wrapper.find('ButtonIcon'));

  expect(node.focus).toHaveBeenCalled();
});

function shallowRender(overrides: Partial<ProjectsSortingSelect['props']> = {}) {
  return shallow<ProjectsSortingSelect>(
    <ProjectsSortingSelect
      defaultOption="name"
      onChange={jest.fn()}
      selectedSort="name"
      view="overall"
      {...overrides}
    />
  );
}
