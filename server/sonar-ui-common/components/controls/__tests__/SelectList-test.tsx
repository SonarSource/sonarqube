/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { waitAndUpdate } from '../../../helpers/testUtils';
import SelectList, { SelectListFilter } from '../SelectList';

const elements = ['foo', 'bar', 'baz'];
const selectedElements = [elements[0]];
const disabledElements = [elements[1]];

it('should display properly with basics features', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.instance().mounted).toBe(true);

  expect(wrapper).toMatchSnapshot();

  wrapper.instance().componentWillUnmount();
  expect(wrapper.instance().mounted).toBe(false);
});

it('should display properly with advanced features', async () => {
  const wrapper = shallowRender({
    allowBulkSelection: true,
    elementsTotalCount: 125,
    pageSize: 10,
    readOnly: true,
    withPaging: true,
  });
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

it('should display a loader when searching', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);

  wrapper.instance().search({});
  expect(wrapper.state().loading).toBe(true);
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loading).toBe(false);
});

it('should cancel filter selection when search is active', async () => {
  const spy = jest.fn().mockResolvedValue({});
  const wrapper = shallowRender({ onSearch: spy });
  wrapper.instance().changeFilter(SelectListFilter.Unselected);
  await waitAndUpdate(wrapper);

  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Unselected,
    page: undefined,
    pageSize: undefined,
  });
  expect(wrapper).toMatchSnapshot();

  const query = 'test';
  wrapper.instance().handleQueryChange(query);
  expect(spy).toHaveBeenCalledWith({
    query,
    filter: SelectListFilter.All,
    page: undefined,
    pageSize: undefined,
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper.instance().handleQueryChange('');
  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Unselected,
    page: undefined,
    pageSize: undefined,
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should display pagination element properly and call search method with correct parameters', () => {
  const spy = jest.fn().mockResolvedValue({});
  const wrapper = shallowRender({ elementsTotalCount: 100, onSearch: spy, withPaging: true });
  expect(wrapper).toMatchSnapshot();
  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 1,
    pageSize: 100,
  }); // Basic default call

  wrapper.instance().onLoadMore();
  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 2,
    pageSize: 100,
  }); // Load more call

  wrapper.instance().onReload();
  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 1,
    pageSize: 100,
  }); // Reload call

  wrapper.setProps({ needToReload: true });
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<SelectList['props']> = {}) {
  return shallow<SelectList>(
    <SelectList
      disabledElements={disabledElements}
      elements={elements}
      onSearch={jest.fn(() => Promise.resolve())}
      onSelect={jest.fn(() => Promise.resolve())}
      onUnselect={jest.fn(() => Promise.resolve())}
      renderElement={(foo: string) => foo}
      selectedElements={selectedElements}
      {...props}
    />
  );
}
