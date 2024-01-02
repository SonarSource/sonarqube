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
import React from 'react';
import { byRole } from 'testing-library-selector';
import PermissionTemplateServiceMock from '../../../../api/mocks/PermissionTemplateServiceMock';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import PermissionTemplatesApp from '../PermissionTemplatesApp';

const serviceMock = new PermissionTemplateServiceMock();

beforeEach(() => {
  serviceMock.reset();
});

const ui = {
  templateLink1: byRole('link', { name: 'Permission Template 1' }),
  adminUserBrowseCheckboxChecked: byRole('checkbox', {
    name: `checked permission 'projects_role.user' for user 'Admin Admin'`,
  }),
  adminUserBrowseCheckboxUnchecked: byRole('checkbox', {
    name: `unchecked permission 'projects_role.user' for user 'Admin Admin'`,
  }),
  adminUserAdministerCheckboxChecked: byRole('checkbox', {
    name: `checked permission 'projects_role.admin' for user 'Admin Admin'`,
  }),
  adminUserAdministerCheckboxUnchecked: byRole('checkbox', {
    name: `unchecked permission 'projects_role.admin' for user 'Admin Admin'`,
  }),

  anyoneGroupBrowseCheckboxChecked: byRole('checkbox', {
    name: `checked permission 'projects_role.user' for group 'Anyone'`,
  }),
  anyoneGroupBrowseCheckboxUnchecked: byRole('checkbox', {
    name: `unchecked permission 'projects_role.user' for group 'Anyone'`,
  }),

  anyoneGroupCodeviewCheckboxChecked: byRole('checkbox', {
    name: `checked permission 'projects_role.codeviewer' for group 'Anyone'`,
  }),
  anyoneGroupCodeviewCheckboxUnchecked: byRole('checkbox', {
    name: `unchecked permission 'projects_role.codeviewer' for group 'Anyone'`,
  }),

  showMoreButton: byRole('button', { name: 'show_more' }),
  whiteUserBrowseCheckbox: byRole('checkbox', {
    name: `unchecked permission 'projects_role.user' for user 'White'`,
  }),
};

it('grants/revokes permission from users or groups', async () => {
  const user = userEvent.setup();
  renderPermissionTemplatesApp();

  await user.click(await ui.templateLink1.find());

  // User
  expect(ui.adminUserBrowseCheckboxUnchecked.get()).not.toBeChecked();
  await user.click(ui.adminUserBrowseCheckboxUnchecked.get());
  expect(ui.adminUserBrowseCheckboxChecked.get()).toBeChecked();

  expect(ui.adminUserAdministerCheckboxChecked.get()).toBeChecked();
  await user.click(ui.adminUserAdministerCheckboxChecked.get());
  expect(ui.adminUserAdministerCheckboxUnchecked.get()).not.toBeChecked();

  // Group
  expect(ui.anyoneGroupBrowseCheckboxUnchecked.get()).not.toBeChecked();
  await user.click(ui.anyoneGroupBrowseCheckboxUnchecked.get());
  expect(ui.anyoneGroupBrowseCheckboxChecked.get()).toBeChecked();

  expect(ui.anyoneGroupCodeviewCheckboxChecked.get()).toBeChecked();
  await user.click(ui.anyoneGroupCodeviewCheckboxChecked.get());
  expect(ui.anyoneGroupCodeviewCheckboxUnchecked.get()).not.toBeChecked();

  // Handles error on permission change
  serviceMock.updatePermissionChangeAllowance(false);
  await user.click(ui.adminUserBrowseCheckboxChecked.get());
  expect(ui.adminUserBrowseCheckboxChecked.get()).toBeChecked();

  await user.click(ui.anyoneGroupCodeviewCheckboxUnchecked.get());
  expect(ui.anyoneGroupCodeviewCheckboxUnchecked.get()).not.toBeChecked();

  await user.click(ui.adminUserBrowseCheckboxChecked.get());
  expect(ui.adminUserBrowseCheckboxChecked.get()).toBeChecked();

  await user.click(ui.adminUserAdministerCheckboxUnchecked.get());
  expect(ui.adminUserAdministerCheckboxUnchecked.get()).not.toBeChecked();
});

it('loads more items on Show More', async () => {
  const user = userEvent.setup();
  renderPermissionTemplatesApp();

  await user.click(await ui.templateLink1.find());

  expect(ui.whiteUserBrowseCheckbox.query()).not.toBeInTheDocument();
  await user.click(ui.showMoreButton.get());
  expect(ui.whiteUserBrowseCheckbox.get()).toBeInTheDocument();
});

function renderPermissionTemplatesApp() {
  renderApp('admin/permission_templates', <PermissionTemplatesApp />, {
    appState: mockAppState({ qualifiers: ['TRK'] }),
  });
}
