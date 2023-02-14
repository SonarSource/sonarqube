/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import React from 'react';
import { byRole } from 'testing-library-selector';
import PermissionsServiceMock from '../../../../api/mocks/PermissionsServiceMock';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { Permissions } from '../../../../types/permissions';
import PermissionTemplatesApp from '../PermissionTemplatesApp';

const serviceMock = new PermissionsServiceMock();

beforeEach(() => {
  serviceMock.reset();
});

const ui = {
  templateLink1: byRole('link', { name: 'Permission Template 1' }),
  permissionCheckbox: (target: string, permission: Permissions) =>
    byRole('checkbox', {
      name: `permission.assign_x_to_y.projects_role.${permission}.${target}`,
    }),
  showMoreButton: byRole('button', { name: 'show_more' }),
};

it('grants/revokes permission from users or groups', async () => {
  const user = userEvent.setup();
  renderPermissionTemplatesApp();

  await user.click(await ui.templateLink1.find());

  // User
  expect(ui.permissionCheckbox('Admin Admin', Permissions.Browse).get()).not.toBeChecked();
  await user.click(ui.permissionCheckbox('Admin Admin', Permissions.Browse).get());
  expect(ui.permissionCheckbox('Admin Admin', Permissions.Browse).get()).toBeChecked();

  expect(ui.permissionCheckbox('Admin Admin', Permissions.Admin).get()).toBeChecked();
  await user.click(ui.permissionCheckbox('Admin Admin', Permissions.Admin).get());
  expect(ui.permissionCheckbox('Admin Admin', Permissions.Admin).get()).not.toBeChecked();

  // Group
  expect(ui.permissionCheckbox('Anyone', Permissions.Browse).get()).not.toBeChecked();
  await user.click(ui.permissionCheckbox('Anyone', Permissions.Browse).get());
  expect(ui.permissionCheckbox('Anyone', Permissions.Browse).get()).toBeChecked();

  expect(ui.permissionCheckbox('Anyone', Permissions.CodeViewer).get()).toBeChecked();
  await user.click(ui.permissionCheckbox('Anyone', Permissions.CodeViewer).get());
  expect(ui.permissionCheckbox('Anyone', Permissions.CodeViewer).get()).not.toBeChecked();

  // Handles error on permission change
  serviceMock.updatePermissionChangeAllowance(false);
  await user.click(ui.permissionCheckbox('Admin Admin', Permissions.Browse).get());
  expect(ui.permissionCheckbox('Admin Admin', Permissions.Browse).get()).toBeChecked();

  await user.click(ui.permissionCheckbox('Anyone', Permissions.CodeViewer).get());
  expect(ui.permissionCheckbox('Anyone', Permissions.CodeViewer).get()).not.toBeChecked();

  await user.click(ui.permissionCheckbox('Admin Admin', Permissions.Admin).get());
  expect(ui.permissionCheckbox('Admin Admin', Permissions.Admin).get()).not.toBeChecked();
});

it('loads more items on Show More', async () => {
  const user = userEvent.setup();
  renderPermissionTemplatesApp();

  await user.click(await ui.templateLink1.find());

  expect(ui.permissionCheckbox('White', Permissions.Browse).query()).not.toBeInTheDocument();
  await user.click(ui.showMoreButton.get());
  expect(ui.permissionCheckbox('White', Permissions.Browse).get()).toBeInTheDocument();
});

function renderPermissionTemplatesApp() {
  renderApp('admin/permission_templates', <PermissionTemplatesApp />, {
    appState: mockAppState({ qualifiers: [ComponentQualifier.Project] }),
  });
}
