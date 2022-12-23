/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { render, screen, waitFor } from '@testing-library/react';
import * as React from 'react';
import { mockPermissionGroup } from '../../../../../helpers/mocks/permissions';
import { Permissions } from '../../../../../types/permissions';
import GroupHolder, { ANYONE } from '../GroupHolder';

it('should disable PermissionCell checkboxes when waiting for the promise to return', async () => {
  renderComponent();

  const checkbox = screen.getAllByRole('checkbox')[0];
  expect(checkbox).not.toHaveClass('disabled');
  checkbox.click();

  await waitFor(() => {
    expect(checkbox).toHaveClass('disabled');
  });

  await waitFor(() => {
    expect(checkbox).not.toHaveClass('disabled');
  });
});

it('should disable all PermissionCell checkboxes for group "Anyone" for a private project', () => {
  renderComponent({ isComponentPrivate: true });

  const checkboxes = screen.getAllByRole('checkbox');

  ['Foo', 'Bar', 'Admin'].forEach((permission, idx) => {
    expect(checkboxes[idx]).toHaveAttribute(
      'aria-label',
      `disabled permission '${permission}' for group 'Anyone'`
    );
  });
});

it('should disable the "admin" PermissionCell checkbox for group "Anyone" for a public project', () => {
  renderComponent();

  expect(
    screen.getByLabelText("unchecked permission 'Foo' for group 'Anyone'")
  ).toBeInTheDocument();

  expect(
    screen.getByLabelText("unchecked permission 'Bar' for group 'Anyone'")
  ).toBeInTheDocument();

  expect(
    screen.getByLabelText("disabled permission 'Admin' for group 'Anyone'")
  ).toBeInTheDocument();
});

const renderComponent = (props: Partial<GroupHolder['props']> = {}) =>
  render(
    <table>
      <tbody>
        <GroupHolder
          group={mockPermissionGroup({ id: 'foobar', name: ANYONE })}
          onToggle={jest.fn().mockResolvedValue(null)}
          permissions={[
            {
              category: 'baz',
              permissions: [
                { key: 'foo', name: 'Foo', description: '' },
                { key: 'bar', name: 'Bar', description: '' },
              ],
            },
            { key: Permissions.Admin, name: 'Admin', description: '' },
          ]}
          selectedPermission="bar"
          {...props}
        />
      </tbody>
    </table>
  );
