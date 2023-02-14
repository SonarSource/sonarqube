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

import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { byLabelText, byRole, byText } from 'testing-library-selector';
import PermissionsServiceMock from '../../../../../api/mocks/PermissionsServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier, Visibility } from '../../../../../types/component';
import { Permissions } from '../../../../../types/permissions';
import { Component } from '../../../../../types/types';
import { PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE, PERMISSIONS_ORDER_FOR_VIEW } from '../../../utils';
import { PermissionsProjectApp } from '../PermissionsProjectApp';

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
      expect(ui.permissionCheckbox('johndoe', permission).get()).toBeInTheDocument();
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
      ui.permissionCheckbox('sonar-users', Permissions.Browse).query()
    ).not.toBeInTheDocument();
    await act(async () => {
      await ui.turnProjectPrivate();
    });
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
    expect(ui.permissionCheckbox('sonar-users', Permissions.Browse).get()).toBeInTheDocument();

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

    expect(ui.permissionCheckbox('sonar-users', Permissions.Admin).get()).not.toBeChecked();

    await ui.togglePermission('sonar-users', Permissions.Admin);
    await ui.appLoaded();
    expect(ui.permissionCheckbox('sonar-users', Permissions.Admin).get()).toBeChecked();

    await ui.togglePermission('sonar-users', Permissions.Admin);
    await ui.appLoaded();
    expect(ui.permissionCheckbox('sonar-users', Permissions.Admin).get()).not.toBeChecked();
  });

  it('should add and remove permissions to/from a user', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    renderPermissionsProjectApp();
    await ui.appLoaded();

    expect(ui.permissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();

    await ui.togglePermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.permissionCheckbox('johndoe', Permissions.Scan).get()).toBeChecked();

    await ui.togglePermission('johndoe', Permissions.Scan);
    await ui.appLoaded();
    expect(ui.permissionCheckbox('johndoe', Permissions.Scan).get()).not.toBeChecked();
  });
});

function getPageObject(user: UserEvent) {
  const ui = {
    loading: byLabelText('loading'),
    permissionCheckbox: (target: string, permission: Permissions) =>
      byRole('checkbox', {
        name: `permission.assign_x_to_y.projects_role.${permission}.${target}`,
      }),
    visibilityRadio: (visibility: Visibility) =>
      byRole('radio', { name: `visibility.${visibility}` }),
    makePublicDisclaimer: byText(
      'projects_role.are_you_sure_to_turn_project_to_public.warning.TRK'
    ),
    confirmPublicBtn: byRole('button', { name: 'projects_role.turn_project_to_public.TRK' }),
    openModalBtn: byRole('button', { name: 'projects_role.apply_template' }),
    closeModalBtn: byRole('button', { name: 'close' }),
    templateSelect: byRole('combobox', { name: /template/ }),
    templateSuccessfullyApplied: byText('projects_role.apply_template.success'),
    confirmApplyTemplateBtn: byRole('button', { name: 'apply' }),
    tableHeaderFilter: (permission: Permissions) =>
      byRole('link', { name: `projects_role.${permission}` }),
    onlyUsersBtn: byRole('button', { name: 'users.page' }),
    onlyGroupsBtn: byRole('button', { name: 'user_groups.page' }),
    showAllBtn: byRole('button', { name: 'all' }),
    searchInput: byRole('searchbox', { name: 'search.search_for_users_or_groups' }),
  };

  return {
    ...ui,
    async appLoaded() {
      await waitFor(() => {
        expect(ui.loading.query()).not.toBeInTheDocument();
      });
    },
    async togglePermission(target: string, permission: Permissions) {
      await user.click(ui.permissionCheckbox(target, permission).get());
    },
    async turnProjectPrivate() {
      await user.click(ui.visibilityRadio(Visibility.Private).get());
    },
    async turnProjectPublic() {
      await user.click(ui.visibilityRadio(Visibility.Public).get());
    },
    async confirmTurnProjectPublic() {
      await user.click(ui.confirmPublicBtn.get());
    },
    async openTemplateModal() {
      await user.click(ui.openModalBtn.get());
    },
    async closeTemplateModal() {
      await user.click(ui.closeModalBtn.get());
    },
    async chooseTemplate(name: string) {
      await selectEvent.select(ui.templateSelect.get(), [name]);
      await user.click(ui.confirmApplyTemplateBtn.get());
    },
    async toggleFilterByPermission(permission: Permissions) {
      await user.click(ui.tableHeaderFilter(permission).get());
    },
    async showOnlyUsers() {
      await user.click(ui.onlyUsersBtn.get());
    },
    async showOnlyGroups() {
      await user.click(ui.onlyGroupsBtn.get());
    },
    async showAll() {
      await user.click(ui.showAllBtn.get());
    },
    async searchFor(name: string) {
      await user.type(ui.searchInput.get(), name);
    },
    async clearSearch() {
      await user.clear(ui.searchInput.get());
    },
  };
}

function renderPermissionsProjectApp(override?: Partial<Component>) {
  function App({ component }: { component: Component }) {
    const [realComponent, setRealComponent] = React.useState(component);
    return (
      <PermissionsProjectApp
        component={realComponent}
        onComponentChange={(changes: Partial<Component>) => {
          setRealComponent({ ...realComponent, ...changes });
        }}
      />
    );
  }

  return renderApp(
    '/',
    <App
      component={mockComponent({
        visibility: Visibility.Public,
        configuration: {
          canUpdateProjectVisibilityToPrivate: true,
          canApplyPermissionTemplate: true,
        },
        ...override,
      })}
    />
  );
}
