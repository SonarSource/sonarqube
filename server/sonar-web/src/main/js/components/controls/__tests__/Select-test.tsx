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
import {
  components,
  GroupTypeBase,
  InputProps,
  OptionTypeBase,
  Props as ReactSelectProps,
} from 'react-select';
import { LoadingIndicatorProps } from 'react-select/src/components/indicators';
import { MultiValueRemoveProps } from 'react-select/src/components/MultiValue';
import { mockReactSelectIndicatorProps } from '../../../helpers/mocks/react-select';
import Select, {
  clearIndicator,
  CreatableSelect,
  dropdownIndicator,
  loadingIndicator,
  multiValueRemove,
  SearchSelect,
} from '../Select';

describe('Select', () => {
  it('should render correctly', () => {
    expect(shallowRender()).toMatchSnapshot('default');
  });

  it('should render complex select component', () => {
    const inputRenderer = (props: InputProps) => (
      <components.Input {...props} className={`little-spacer-top ${props.className}`} />
    );

    expect(
      shallowRender({
        isClearable: true,
        isLoading: true,
        components: {
          Input: inputRenderer,
        },
      })
    ).toMatchSnapshot('other props');
  });

  it('should render clearIndicator correctly', () => {
    expect(clearIndicator(mockReactSelectIndicatorProps({ value: '' }))).toMatchSnapshot();
  });

  it('should render dropdownIndicator correctly', () => {
    expect(dropdownIndicator(mockReactSelectIndicatorProps({ value: '' }))).toMatchSnapshot();
  });

  it('should render loadingIndicator correctly', () => {
    expect(
      loadingIndicator({ innerProps: { className: 'additional-class' } } as LoadingIndicatorProps<
        {},
        false
      >)
    ).toMatchSnapshot();
  });

  it('should render multiValueRemove correctly', () => {
    expect(multiValueRemove({ innerProps: {} } as MultiValueRemoveProps<{}>)).toMatchSnapshot();
  });

  function shallowRender<
    Option extends OptionTypeBase,
    IsMulti extends boolean = false,
    Group extends GroupTypeBase<Option> = GroupTypeBase<Option>
  >(props: Partial<ReactSelectProps<Option, IsMulti, Group>> = {}) {
    return shallow<ReactSelectProps<Option, IsMulti, Group>>(<Select {...props} />);
  }
});

it.each([
  ['CreatableSelect', CreatableSelect],
  ['SearchSelect', SearchSelect],
])('should render %s correctly', (_name, Component) => {
  expect(
    shallow(<Component />)
      .dive()
      .dive()
  ).toMatchSnapshot();
});
