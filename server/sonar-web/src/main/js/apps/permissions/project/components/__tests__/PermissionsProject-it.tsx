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

import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PermissionsServiceMock from '../../../../../api/mocks/PermissionsServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockPermissionGroup, mockPermissionUser } from '../../../../../helpers/mocks/permissions';
import {
  PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
  PERMISSIONS_ORDER_FOR_VIEW,
} from '../../../../../helpers/permissions';
import { renderAppWithComponentContext } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier, Visibility } from '../../../../../types/component';
import { Permissions } from '../../../../../types/permissions';
import { Component, PermissionGroup, PermissionUser } from '../../../../../types/types';
import { projectPermissionsRoutes } from '../../../routes';
import { getPageObject } from '../../../test-utils';

let serviceMock: PermissionsServiceMock;
beforeAll(() => {
  serviceMock = new PermissionsServiceMock();
});

afterEach(() => {
  serviceMock.reset();
});

describe('rendering', () => {
  it.each([
    [ComponentQualifier.Project, 'roles.page.description2', PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE],
    [ComponentQualifier.Portfolio, 'roles.page.description_portfolio', PERMISSIONS_ORDER_FOR_VIEW],
    [
      ComponentQualifier.Application,
      'roles.page.description_application',
      PERMISSIONS_ORDER_FOR_VIEW,
    ],
  ])('should render correctly for %s', async (qualifier, description, permissions) => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp({ qualifier, visibility: Visibility.Private });
    await ui.appLoaded();

    expect(screen.getByText(description)).toBeInTheDocument();
    permissions.forEach((permission) => {
      expect(ui.projectPermissionCheckbox('johndoe', permission).get()).toBeInTheDocument();
    });
  });
});

describe('filtering', () => {
  it('should allow to filter permission holders', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
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

  it('should allow to show only permission holders with a specific permission', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    expect(screen.getAllByRole('row').length).toBe(7);
    await ui.toggleFilterByPermission(Permissions.Admin);
    expect(screen.getAllByRole('row').length).toBe(2);
    await ui.toggleFilterByPermission(Permissions.Admin);
    expect(screen.getAllByRole('row').length).toBe(7);
  });
});

describe('assigning/revoking permissions', () => {
  it('should allow to apply a permission template', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    await ui.openTemplateModal();
    expect(ui.confirmApplyTemplateBtn.get()).toBeDisabled();
    await ui.chooseTemplate('Permission Template 2');
    expect(ui.templateSuccessfullyApplied.get()).toBeInTheDocument();
    await ui.closeTemplateModal();
    expect(ui.templateSuccessfullyApplied.query()).not.toBeInTheDocument();
  });

  it('should allow to turn a public project private (and vice-versa)', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(
      ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).query()
    ).not.toBeInTheDocument();
    await act(async () => {
      await ui.turnProjectPrivate();
    });
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
    expect(
      ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()
    ).toBeInTheDocument();

    await ui.turnProjectPublic();
    expect(ui.makePublicDisclaimer.get()).toBeInTheDocument();
    await act(async () => {
      await ui.confirmTurnProjectPublic();
    });
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
  });

  it('should add and remove permissions to/from a group', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Admin).get()).not.toBeChecked();

    await ui.toggleProjectPermission('sonar-users', Permissions.Admin);
    await ui.appLoaded();
    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Admin).get()).toBeChecked();

    await ui.toggleProjectPermission('sonar-users', Permissions.Admin);
    await ui.appLoaded();
    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Admin).get()).not.toBeChecked();
  });

  it('should add and remove permissions to/from a user', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    expect(ui.projectPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();

    await ui.toggleProjectPermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.projectPermissionCheckbox('johndoe', Permissions.Scan).get()).toBeChecked();

    await ui.toggleProjectPermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.projectPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
  });

  it('should handle errors correctly', async () => {
    serviceMock.setIsAllowedToChangePermissions(false);
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    expect(ui.projectPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
    await ui.toggleProjectPermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.projectPermissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
  });
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
  renderPermissionsProjectApp();
  await ui.appLoaded();

  expect(screen.getAllByRole('row').length).toBe(11);
  await ui.clickLoadMore();
  expect(screen.getAllByRole('row').length).toBe(21);
});

function renderPermissionsProjectApp(override?: Partial<Component>) {
  return renderAppWithComponentContext(
    'project_roles',
    projectPermissionsRoutes,
    {},
    {
      component: mockComponent({
        visibility: Visibility.Public,
        configuration: {
          canUpdateProjectVisibilityToPrivate: true,
          canApplyPermissionTemplate: true,
        },
        ...override,
      }),
    }
  );
}
