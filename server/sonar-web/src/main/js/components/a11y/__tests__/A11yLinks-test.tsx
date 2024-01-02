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
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byRole } from '../../../helpers/testSelector';
import { A11yContext } from '../A11yContext';
import A11yProvider from '../A11yProvider';
import A11ySkipLinks from '../A11ySkipLinks';

const ui = {
  links: byRole('link'),

  // specific to test
  addButton: byRole('button', { name: 'Add' }),
  removeButton: byRole('button', { name: 'Remove' }),
};

it('should render correctly', async () => {
  const user = userEvent.setup();

  renderA11ySkipLinks();

  expect(ui.links.queryAll()).toHaveLength(0);

  await user.click(ui.addButton.get());
  expect(ui.links.getAll()).toHaveLength(1);

  await user.click(ui.addButton.get());
  expect(ui.links.getAll()).toHaveLength(2);

  await user.click(ui.removeButton.get());
  expect(ui.links.getAll()).toHaveLength(1);
});

function renderA11ySkipLinks() {
  return renderComponent(
    <A11yProvider>
      <A11ySkipLinks />

      <LinkTester />
    </A11yProvider>,
    '/',
    {},
  );
}

let count = 0;

function LinkTester() {
  const { addA11ySkipLink, removeA11ySkipLink } = React.useContext(A11yContext);

  return (
    <>
      <button
        onClick={() => {
          count += 1;
          addA11ySkipLink({ key: `${count}`, label: `link #${count}` });
        }}
        type="button"
      >
        Add
      </button>
      <button
        onClick={() => {
          removeA11ySkipLink({ key: `${count}`, label: `link #${count}` });
          count -= 1;
        }}
        type="button"
      >
        Remove
      </button>
    </>
  );
}
