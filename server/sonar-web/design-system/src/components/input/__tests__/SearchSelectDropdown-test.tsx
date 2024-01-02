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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithContext } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { LabelValueSelectOption } from '../InputSelect';
import { SearchSelectDropdown } from '../SearchSelectDropdown';

const defaultOptions = [
  { label: 'label1', value: 'value1' },
  { label: 'different', value: 'diff1' },
];

const loadOptions = (
  query: string,
  cb: (options: Array<LabelValueSelectOption<string>>) => void,
) => {
  cb(defaultOptions.filter((o) => o.label.includes(query)));
};

it('should render select input and be able to search and select an option', async () => {
  const user = userEvent.setup();
  const onChange = jest.fn();
  renderSearchSelectDropdown({ onChange });
  expect(screen.getByText('not assigned')).toBeInTheDocument();
  await user.click(screen.getByRole('combobox'));
  expect(screen.getByText('label1')).toBeInTheDocument();
  expect(screen.getByText('different')).toBeInTheDocument();
  await user.type(screen.getByRole('searchbox', { name: 'label' }), 'label');
  expect(await screen.findByText('label')).toBeInTheDocument();
  expect(screen.queryByText('different')).not.toBeInTheDocument();
  await user.click(screen.getByText('label'));
  expect(onChange).toHaveBeenLastCalledWith(defaultOptions[0], {
    action: 'select-option',
    name: undefined,
    option: undefined,
  });
});

it('should handle key navigation', async () => {
  const user = userEvent.setup();
  renderSearchSelectDropdown();
  await user.tab();
  await user.keyboard('{Enter}');
  await user.type(screen.getByRole('searchbox', { name: 'label' }), 'label');
  expect(await screen.findByText('label')).toBeInTheDocument();
  expect(screen.queryByText('different')).not.toBeInTheDocument();
  await user.keyboard('{Escape}');
  expect(await screen.findByText('different')).toBeInTheDocument();
  await act(async () => {
    await user.keyboard('{Escape}');
  });
  expect(screen.queryByText('different')).not.toBeInTheDocument();
  await user.tab({ shift: true });
  await user.keyboard('{ArrowDown}');
  expect(await screen.findByText('label1')).toBeInTheDocument();
});

it('behaves correctly in disabled state', async () => {
  const user = userEvent.setup();
  renderSearchSelectDropdown({ isDisabled: true });
  await user.click(screen.getByRole('combobox'));
  expect(screen.queryByText('label1')).not.toBeInTheDocument();
  await user.tab();
  await user.keyboard('{Enter}');
  expect(screen.queryByText('label1')).not.toBeInTheDocument();
});

function renderSearchSelectDropdown(props: Partial<FCProps<typeof SearchSelectDropdown>> = {}) {
  return renderWithContext(
    <SearchSelectDropdown
      aria-label="label"
      controlLabel="not assigned"
      defaultOptions={defaultOptions}
      isDiscreet
      loadOptions={loadOptions}
      placeholder="search for things"
      {...props}
    />,
  );
}
