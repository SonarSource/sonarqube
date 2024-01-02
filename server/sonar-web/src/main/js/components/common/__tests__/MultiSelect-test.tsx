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
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from 'testing-library-selector';
import MultiSelect from '../MultiSelect';

const ui = {
  checkbox: (name: string) => byRole('checkbox', { name }),
  search: byRole('searchbox', { name: 'search' }),
  noResult: byText('no_results_for_x.notfound'),
};

it('should handle selection', async () => {
  const user = userEvent.setup();
  const rerender = renderMultiSelect();
  expect(ui.checkbox('az').get()).toBeChecked();
  await user.click(ui.checkbox('az').get());
  expect(ui.checkbox('az').get()).not.toBeChecked();

  await user.type(ui.search.get(), 'create');
  await user.click(ui.checkbox('create_new_element: create').get());
  expect(ui.checkbox('create').get()).toBeChecked();

  // Custom label
  rerender({ renderLabel: (label) => `prefxed-${label}` });
  expect(ui.checkbox('prefxed-create').get()).toBeChecked();
});

it('should handle disable selection', () => {
  renderMultiSelect({ allowSelection: false });
  expect(ui.checkbox('az').get()).toBeChecked();
  expect(ui.checkbox('cx').get()).toHaveAttribute('aria-disabled', 'true');
});

it('should handle search', async () => {
  const user = userEvent.setup();
  const rerender = renderMultiSelect();
  expect(ui.checkbox('cx').get()).toBeInTheDocument();
  await user.type(ui.search.get(), 'az');
  expect(ui.checkbox('cx').query()).not.toBeInTheDocument();
  expect(ui.checkbox('az').get()).toBeInTheDocument();
  expect(ui.checkbox('az-new').get()).toBeInTheDocument();

  await user.clear(ui.search.get());
  await user.type(ui.search.get(), 'notfound');
  expect(ui.checkbox('create_new_element: notfound').get()).toBeInTheDocument();

  rerender({ allowNewElements: false });
  await user.clear(ui.search.get());
  await user.type(ui.search.get(), 'notfound');
  expect(ui.noResult.get()).toBeInTheDocument();
});

function renderMultiSelect(override?: Partial<MultiSelect['props']>) {
  const initial = ['cx', 'dw', 'ev', 'fu', 'gt', 'hs'];
  const initialSelected = ['az', 'by'];

  function Parent(props?: Partial<MultiSelect['props']>) {
    const [elements, setElements] = React.useState(initial);
    const [selected, setSelected] = React.useState(['az', 'by']);
    const onSearch = (query: string) => {
      if (query === 'notfound') {
        setElements([]);
        setSelected([]);
      } else if (query === '') {
        setElements(initial);
        setSelected(initialSelected);
      } else {
        setElements([...elements.filter((e) => e.indexOf(query) !== -1), `${query}-new`]);
        setSelected(selected.filter((e) => e.indexOf(query) !== -1));
      }
      return Promise.resolve();
    };

    const onSelect = (element: string) => {
      setSelected([...selected, element]);
      setElements(elements.filter((e) => e !== element));
    };

    const onUnselect = (element: string) => {
      setElements([...elements, element]);
      setSelected(selected.filter((e) => e !== element));
    };
    return (
      <MultiSelect
        selectedElements={selected}
        elements={elements}
        legend="multi select"
        onSearch={onSearch}
        onSelect={onSelect}
        onUnselect={onUnselect}
        placeholder="search"
        {...props}
      />
    );
  }

  const { rerender } = render(<Parent {...override} />);
  return function (reoverride?: Partial<MultiSelect['props']>) {
    rerender(<Parent {...override} {...reoverride} />);
  };
}
