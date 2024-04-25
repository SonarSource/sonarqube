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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { without } from 'lodash';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import PermissionsServiceMock from '../../../../../api/mocks/PermissionsServiceMock';
import { mockPermissionGroup, mockPermissionUser } from '../../../../../helpers/mocks/permissions';
import { PERMISSIONS_ORDER_GLOBAL } from '../../../../../helpers/permissions';
import { mockAppState } from '../../../../../helpers/testMocks';
import { renderAppRoutes } from '../../../../../helpers/testReactTestingUtils';
import { AppState } from '../../../../../types/appstate';
import { Permissions } from '../../../../../types/permissions';
import { PermissionGroup, PermissionUser } from '../../../../../types/types';
import { globalPermissionsRoutes } from '../../../routes';
import { flattenPermissionsList, getPageObject } from '../../../test-utils';

let serviceMock: PermissionsServiceMock;
beforeAll(() => {
  serviceMock = new PermissionsServiceMock();
});

afterEach(() => {
  serviceMock.reset();
});

describe('rendering', () => {
  it('should render correctly without applications and portfolios', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsGlobalApp();
    await ui.appLoaded();

    without(
      flattenPermissionsList(PERMISSIONS_ORDER_GLOBAL),
      Permissions.ApplicationCreation,
      Permissions.PortfolioCreation,
    ).forEach((permission) => {
      expect(ui.globalPermissionCheckbox('johndoe', permission).get()).toBeInTheDocument();
    });
  });

  it.each([
    [
      ComponentQualifier.Portfolio,
      without(flattenPermissionsList(PERMISSIONS_ORDER_GLOBAL), Permissions.ApplicationCreation),
    ],
    [
      ComponentQualifier.Application,
      without(flattenPermissionsList(PERMISSIONS_ORDER_GLOBAL), Permissions.PortfolioCreation),
    ],
  ])('should render correctly when %s are enabled', async (qualifier, permissions) => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsGlobalApp(mockAppState({ qualifiers: [qualifier] }));
    await ui.appLoaded();

    permissions.forEach((permission) => {
      expect(ui.globalPermissionCheckbox('johndoe', permission).get()).toBeInTheDocument();
    });
  });
});

describe('assigning/revoking permissions', () => {
  it('should add and remove permissions to/from a group', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsGlobalApp();
    await ui.appLoaded();

    expect(ui.globalPermissionCheckbox('sonar-users', Permissions.Admin).get()).not.toBeChecked();

    await ui.toggleGlobalPermission('sonar-users', Permissions.Admin);
    await ui.appLoaded();
    expect(ui.globalPermissionCheckbox('sonar-users', Permissions.Admin).get()).toBeChecked();

    await ui.toggleGlobalPermission('sonar-users', Permissions.Admin);
    await ui.appLoaded();
    expect(ui.globalPermissionCheckbox('sonar-users', Permissions.Admin).get()).not.toBeChecked();
  });

  it('should add and remove permissions to/from a user', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsGlobalApp();
    await ui.appLoaded();

    expect(ui.globalPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();

    await ui.toggleGlobalPermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.globalPermissionCheckbox('johndoe', Permissions.Scan).get()).toBeChecked();

    await ui.toggleGlobalPermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.globalPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
  });

  it('should handle errors correctly', async () => {
    serviceMock.setIsAllowedToChangePermissions(false);
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsGlobalApp();
    await ui.appLoaded();

    expect(ui.globalPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
    await ui.toggleGlobalPermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.globalPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
  });
});

it('should allow to filter permission holders', async () => {
  const user = userEvent.setup();
  const ui = getPageObject(user);
  renderPermissionsGlobalApp();
  await ui.appLoaded();

  expect(screen.getByText('sonar-users')).toBeInTheDocument();
  expect(screen.getByText('johndoe')).toBeInTheDocument();

  await ui.showOnlyUsers();
  expect(screen.queryByText('sonar-users')).not.toBeInTheDocument();
  expect(screen.getByText('johndoe')).toBeInTheDocument();

  await ui.showOnlyGroups();
  expect(screen.getByText('sonar-users')).toBeInTheDocument();
  expect(screen.queryByText('johndoe')).not.toBeInTheDocument();

  await ui.showAll();
  expect(screen.getByText('sonar-users')).toBeInTheDocument();
  expect(screen.getByText('johndoe')).toBeInTheDocument();

  await ui.searchFor('sonar-adm');
  expect(screen.getByText('sonar-admins')).toBeInTheDocument();
  expect(screen.queryByText('sonar-users')).not.toBeInTheDocument();
  expect(screen.queryByText('johndoe')).not.toBeInTheDocument();

  await ui.clearSearch();
  expect(screen.getByText('sonar-users')).toBeInTheDocument();
  expect(screen.getByText('johndoe')).toBeInTheDocument();
});

it('should correctly handle pagination', async () => {
  const groups: PermissionGroup[] = [];
  const users: PermissionUser[] = [];
  Array.from(Array(20).keys()).forEach((i) => {
    groups.push(mockPermissionGroup({ name: `Group ${i}` }));
    users.push(mockPermissionUser({ login: `user-${i}` }));
  });
  serviceMock.setGroups(groups);
  serviceMock.setUsers(users);

  const user = userEvent.setup();
  const ui = getPageObject(user);
  renderPermissionsGlobalApp();
  await ui.appLoaded();

  expect(screen.getAllByRole('row').length).toBe(11);
  await ui.clickLoadMore();
  expect(screen.getAllByRole('row').length).toBe(21);
});

function renderPermissionsGlobalApp(appState?: AppState) {
  return renderAppRoutes('permissions', globalPermissionsRoutes, { appState });
}
