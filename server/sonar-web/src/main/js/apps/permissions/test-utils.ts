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
import { act, waitFor } from '@testing-library/react';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import selectEvent from 'react-select-event';
import { byRole, byText } from '../../helpers/testSelector';
import { Visibility } from '../../types/component';
import { Permissions } from '../../types/permissions';

export function getPageObject(user: UserEvent) {
  const ui = {
    loading: byText('loading'),
    pageTitle: byRole('heading', {
      name: /permissions.page/,
    }),
    projectPermissionCheckbox: (target: string, permission: Permissions) =>
      byRole('checkbox', {
        name: `permission.assign_x_to_y.projects_role.${permission}.${target}`,
      }),
    globalPermissionCheckbox: (target: string, permission: Permissions) =>
      byRole('checkbox', {
        name: `permission.assign_x_to_y.global_permissions.${permission}.${target}`,
      }),
    visibilityRadio: (visibility: Visibility) =>
      byRole('radio', { name: `visibility.${visibility}` }),
    githubLogo: byRole('img', { name: 'project_permission.github_managed' }),
    githubExplanations: byText('roles.page.description.github'),
    confirmRemovePermissionDialog: byRole('dialog', {
      name: 'project_permission.remove_only_confirmation_title',
    }),
    nonGHProjectWarning: byText('project_permission.local_project_with_github_provisioning'),
    makePublicDisclaimer: byText(
      'projects_role.are_you_sure_to_turn_project_to_public.warning.TRK',
    ),
    confirmPublicBtn: byRole('button', { name: 'projects_role.turn_project_to_public.TRK' }),
    applyTemplateBtn: byRole('button', { name: 'projects_role.apply_template' }),
    closeModalBtn: byRole('button', { name: 'close' }),
    templateSelect: byRole('combobox', { name: /template/ }),
    templateSuccessfullyApplied: byText('projects_role.apply_template.success'),
    confirmApplyTemplateBtn: byRole('button', { name: 'apply' }),
    tableHeaderFilter: (permission: Permissions) =>
      byRole('button', { name: `projects_role.${permission}` }),
    onlyUsersBtn: byRole('radio', { name: 'users.page' }),
    onlyGroupsBtn: byRole('radio', { name: 'user_groups.page' }),
    showAllBtn: byRole('radio', { name: 'all' }),
    searchInput: byRole('searchbox', { name: 'search.search_for_users_or_groups' }),
    loadMoreBtn: byRole('button', { name: 'show_more' }),
  };

  return {
    ...ui,
    async appLoaded() {
      await waitFor(() => {
        expect(ui.loading.query()).not.toBeInTheDocument();
      });
    },
    async toggleProjectPermission(target: string, permission: Permissions) {
      await act(() => user.click(ui.projectPermissionCheckbox(target, permission).get()));
    },
    async toggleGlobalPermission(target: string, permission: Permissions) {
      await user.click(ui.globalPermissionCheckbox(target, permission).get());
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
      await user.click(ui.applyTemplateBtn.get());
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
    async clickLoadMore() {
      await user.click(ui.loadMoreBtn.get());
    },
  };
}

export function flattenPermissionsList(
  list: Array<
    | Permissions
    | {
        category: string;
        permissions: Permissions[];
      }
  >,
) {
  function isPermissions(
    p:
      | Permissions
      | {
          category: string;
          permissions: Permissions[];
        },
  ): p is Permissions {
    return typeof p === 'string';
  }

  return list.reduce((acc, item) => {
    if (isPermissions(item)) {
      acc.push(item);
    } else {
      acc.push(...item.permissions);
    }
    return acc;
  }, [] as Permissions[]);
}
