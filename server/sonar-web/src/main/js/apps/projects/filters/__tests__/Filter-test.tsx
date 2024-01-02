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
import { mockEvent } from '../../../../helpers/testUtils';
import Filter from '../Filter';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('renders header and footer', () => {
  expect(shallowRender({ header: <header />, footer: <footer /> })).toMatchSnapshot();
});

it('renders no results', () => {
  expect(shallowRender({ options: [] })).toMatchSnapshot();
});

it('highlights under', () => {
  expect(shallowRender({ highlightUnder: 1 })).toMatchSnapshot();
});

it('renders selected', () => {
  expect(shallowRender({ value: 2 })).toMatchSnapshot();
});

it('hightlights under selected', () => {
  expect(shallowRender({ highlightUnder: 1, value: 2 })).toMatchSnapshot();
});

it('renders multiple selected', () => {
  expect(shallowRender({ value: [1, 2] })).toMatchSnapshot();
});

it('renders facet bar chart', () => {
  expect(
    shallowRender({
      getFacetValueForOption: (facet: any, option: any) => facet[option],
      facet: { a: 17, b: 15, c: 24 },
      maxFacetValue: 24,
      options: ['a', 'b', 'c'],
    })
  ).toMatchSnapshot();
});

it('should handle click when value is single', () => {
  const onQueryChange = jest.fn();
  const wrapper = shallowRender({ onQueryChange, value: 'option1' });

  // select
  wrapper.instance().handleClick(mockEvent({ currentTarget: { dataset: { key: 'option2' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: 'option2' });

  onQueryChange.mockClear();

  // deselect
  wrapper.instance().handleClick(mockEvent({ currentTarget: { dataset: { key: 'option1' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: null });
});

it('should handle click when value is array', () => {
  const onQueryChange = jest.fn();
  const wrapper = shallowRender({ onQueryChange, value: ['option1', 'option2'] });

  // select one
  wrapper.instance().handleClick(mockEvent({ currentTarget: { dataset: { key: 'option2' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: 'option2' });

  onQueryChange.mockClear();

  // select other
  wrapper.instance().handleClick(mockEvent({ currentTarget: { dataset: { key: 'option3' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: 'option3' });

  onQueryChange.mockClear();

  // select additional
  wrapper
    .instance()
    .handleClick(mockEvent({ ctrlKey: true, currentTarget: { dataset: { key: 'option3' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: 'option1,option2,option3' });

  onQueryChange.mockClear();

  // deselect one
  wrapper
    .instance()
    .handleClick(mockEvent({ metaKey: true, currentTarget: { dataset: { key: 'option2' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: 'option1' });
});

it('should handle click when value is array with one value', () => {
  const onQueryChange = jest.fn();
  const wrapper = shallowRender({ onQueryChange, value: ['option1'] });

  // deselect one
  wrapper
    .instance()
    .handleClick(mockEvent({ ctrlKey: true, currentTarget: { dataset: { key: 'option1' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: null });

  onQueryChange.mockClear();

  // deselect one
  wrapper.instance().handleClick(mockEvent({ currentTarget: { dataset: { key: 'option1' } } }));
  expect(onQueryChange).toHaveBeenCalledWith({ foo: null });
});

function shallowRender(overrides: Partial<Filter['props']> = {}) {
  return shallow<Filter>(
    <Filter
      onQueryChange={jest.fn()}
      options={[1, 2, 3]}
      property="foo"
      renderOption={(option) => option}
      renderAccessibleLabel={(option) => option.toString()}
      {...overrides}
    />
  );
}
