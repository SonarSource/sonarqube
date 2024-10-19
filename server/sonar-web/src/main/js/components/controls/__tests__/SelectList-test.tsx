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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import SelectList, { SelectListFilter } from '../SelectList';

const elements = ['foo', 'bar', 'baz'];
const selectedElements = [elements[0]];
const disabledElements = [elements[1]];

it('should render with extra', () => {
  renderSelectList({ renderElement: (element) => [element, 'with extra'] });

  expect(byText('with extra').getAll()).toHaveLength(3);
});

it('should cancel filter selection when search is active', async () => {
  const user = userEvent.setup();

  const spy = jest.fn().mockResolvedValue({});
  renderSelectList({ onSearch: spy });

  await user.click(ui.filterToggle(SelectListFilter.Unselected).get());

  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Unselected,
    page: undefined,
    pageSize: undefined,
  });

  const query = 'test';

  await user.type(ui.searchInput.get(), query);

  expect(spy).toHaveBeenCalledWith({
    query,
    filter: SelectListFilter.All,
    page: undefined,
    pageSize: undefined,
  });

  await user.clear(ui.searchInput.get());

  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Unselected,
    page: undefined,
    pageSize: undefined,
  });
});

it('should display pagination element properly and call search method with correct parameters', async () => {
  const user = userEvent.setup();
  const spy = jest.fn().mockResolvedValue({});
  renderSelectList({
    elementsTotalCount: 100,
    onSearch: spy,
    withPaging: true,
  });
  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 1,
    pageSize: 100,
  }); // Basic default call

  await user.click(ui.footerLoadMore.get());

  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 2,
    pageSize: 100,
  }); // Load more call
});

it('should allow to reload when needed', async () => {
  const user = userEvent.setup();
  const spy = jest.fn().mockResolvedValue({});

  renderSelectList({
    elementsTotalCount: 100,
    onSearch: spy,
    needToReload: true,
    withPaging: true,
  });

  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 1,
    pageSize: 100,
  }); // Basic default call

  await user.click(await ui.footerReload.find());

  expect(spy).toHaveBeenCalledWith({
    query: '',
    filter: SelectListFilter.Selected,
    page: 1,
    pageSize: 100,
  }); // Reload call
});

function renderSelectList(props: Partial<SelectList['props']> = {}) {
  return renderComponent(
    <SelectList
      disabledElements={disabledElements}
      elements={elements}
      onSearch={jest.fn()}
      onSelect={jest.fn(() => Promise.resolve())}
      onUnselect={jest.fn(() => Promise.resolve())}
      renderElement={(foo: string) => foo}
      selectedElements={selectedElements}
      loading={false}
      {...props}
    />,
  );
}

const ui = {
  filterToggle: (filter: SelectListFilter) =>
    byRole('radio', { name: filter === SelectListFilter.Unselected ? 'unselected' : filter }),

  searchInput: byRole('searchbox', { name: 'search_verb' }),

  footerLoadMore: byRole('button', { name: 'show_more' }),
  footerReload: byRole('button', { name: 'reload' }),
};
