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

import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import AuthenticationServiceMock from '../../../api/mocks/AuthenticationServiceMock';
import GroupsServiceMock from '../../../api/mocks/GroupsServiceMock';
import { Provider } from '../../../components/hooks/useManageProvider';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { Feature } from '../../../types/features';
import { TaskStatuses } from '../../../types/tasks';
import GroupsApp from '../GroupsApp';

const handler = new GroupsServiceMock();
const authenticationHandler = new AuthenticationServiceMock();

const ui = {
  createGroupButton: byRole('button', { name: 'groups.create_group' }),
  infoManageMode: byText(/groups\.page\.managed_description/),
  description: byText('user_groups.page.description'),
  allFilter: byRole('radio', { name: 'all' }),
  selectedFilter: byRole('radio', { name: 'selected' }),
  unselectedFilter: byRole('radio', { name: 'unselected' }),
  localAndManagedFilter: byRole('button', { name: 'all' }),
  managedFilter: byRole('button', { name: 'managed' }),
  localFilter: byRole('button', { name: 'local' }),
  searchInput: byRole('searchbox', { name: 'search.search_by_name' }),
  updateButton: byRole('button', { name: 'update_details' }),
  updateDialog: byRole('dialog', { name: 'groups.update_group' }),
  updateDialogButton: byRole('button', { name: 'update_verb' }),
  deleteButton: byRole('button', { name: 'delete' }),
  deleteDialog: byRole('dialog', { name: 'groups.delete_group' }),
  deleteDialogButton: byRole('button', { name: 'delete' }),
  showMore: byRole('button', { name: 'show_more' }),
  nameInput: byRole('textbox', { name: 'name field_required' }),
  descriptionInput: byRole('textbox', { name: 'description' }),
  createGroupDialogButton: byRole('button', { name: 'create' }),
  editGroupDialogButton: byRole('button', { name: 'groups.create_group' }),
  reloadButton: byRole('button', { name: 'reload' }),
  doneButton: byRole('button', { name: 'done' }),

  createGroupDialog: byRole('dialog', { name: 'groups.create_group' }),
  membersViewDialog: byRole('dialog', { name: 'users.list' }),
  membersDialog: byRole('dialog', { name: 'users.update' }),
  getMembers: () => within(ui.membersDialog.get()).getAllByRole('checkbox'),

  managedGroupRow: byRole('row', { name: 'managed-group 3' }),
  githubManagedGroupRow: byRole('row', { name: 'managed-group github 3' }),
  managedGroupEditMembersButton: byRole('button', { name: 'groups.users.edit.managed-group' }),
  managedGroupViewMembersButton: byRole('button', { name: 'groups.users.view.managed-group' }),

  memberAliceUser: byText('alice'),
  memberBobUser: byText('bob'),
  memberSearchInput: byRole('searchbox', { name: 'search_verb' }),

  managedEditButton: byRole('button', { name: 'groups.edit.managed-group' }),

  localGroupRow: byRole('row', { name: 'local-group 3' }),
  localGroupEditMembersButton: byRole('button', { name: 'groups.users.edit.local-group' }),
  localGroupRow2: byRole('row', { name: 'local-group 2 3 group 2 is loco!' }),
  editedLocalGroupRow: byRole('row', { name: 'local-group 3 3 group 3 rocks!' }),
  localEditButton: byRole('button', { name: 'groups.edit.local-group' }),
  localGroupRowWithLocalBadge: byRole('row', {
    name: 'local-group local 3',
  }),

  githubProvisioningPending: byText(/synchronization_pending/),
  githubProvisioningInProgress: byText(/synchronization_in_progress/),
  githubProvisioningSuccess: byText(/synchronization_successful/),
  githubProvisioningAlert: byText(/synchronization_failed_short/),
};

beforeEach(() => {
  handler.reset();
  authenticationHandler.reset();
});

describe('in non managed mode', () => {
  beforeEach(() => {
    handler.setIsManaged(false);
  });

  it('should render all groups', async () => {
    renderGroupsApp();

    expect(await ui.localGroupRow.find()).toBeInTheDocument();
    expect(ui.managedGroupRow.get()).toBeInTheDocument();
    expect(ui.localGroupRowWithLocalBadge.query()).not.toBeInTheDocument();
  });

  it('should be able to create a group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    expect(await ui.description.find()).toBeInTheDocument();

    await user.click(ui.createGroupButton.get());

    expect(await ui.createGroupDialog.find()).toBeInTheDocument();

    await user.type(ui.nameInput.get(), 'local-group 2');
    await user.type(ui.descriptionInput.get(), 'group 2 is loco!');
    await user.click(ui.createGroupDialogButton.get());

    expect(await ui.localGroupRow2.find()).toBeInTheDocument();
  });

  it('should be able to delete a group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    await user.click(await ui.localEditButton.find());
    await user.click(await ui.deleteButton.find());

    expect(await ui.deleteDialog.find()).toBeInTheDocument();

    await user.click(ui.deleteDialogButton.get());

    expect(await ui.managedGroupRow.find()).toBeInTheDocument();
    expect(ui.localGroupRow.query()).not.toBeInTheDocument();
  });

  it('should be able to edit a group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    await user.click(await ui.localEditButton.find());
    await user.click(await ui.updateButton.find());

    expect(ui.updateDialog.get()).toBeInTheDocument();

    await user.clear(ui.nameInput.get());
    await user.type(ui.nameInput.get(), 'local-group 3');
    await user.clear(ui.descriptionInput.get());
    await user.type(ui.descriptionInput.get(), 'group 3 rocks!');

    expect(ui.updateDialog.get()).toBeInTheDocument();

    await user.click(ui.updateDialogButton.get());

    expect(await ui.managedGroupRow.find()).toBeInTheDocument();
    expect(await ui.editedLocalGroupRow.find()).toBeInTheDocument();
  });

  it('should be able to edit the members of a group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    expect(await ui.localGroupRow.find()).toBeInTheDocument();
    expect(await ui.localGroupEditMembersButton.find()).toBeInTheDocument();

    await user.click(ui.localGroupEditMembersButton.get());

    expect(await ui.membersDialog.find()).toBeInTheDocument();

    expect(ui.getMembers()).toHaveLength(2);

    await user.click(ui.allFilter.get());
    expect(ui.getMembers()).toHaveLength(3);

    await user.click(ui.unselectedFilter.get());
    expect(ui.reloadButton.query()).not.toBeInTheDocument();
    await user.click(ui.getMembers()[0]);
    expect(await ui.reloadButton.find()).toBeInTheDocument();

    await user.click(ui.selectedFilter.get());
    expect(ui.getMembers()).toHaveLength(3);
    expect(ui.reloadButton.query()).not.toBeInTheDocument();
    await user.click(ui.getMembers()[0]);
    expect(await ui.reloadButton.find()).toBeInTheDocument();
    await user.click(ui.reloadButton.get());
    expect(ui.getMembers()).toHaveLength(2);

    await user.click(ui.doneButton.get());
    expect(ui.membersDialog.query()).not.toBeInTheDocument();
  });

  it('should be able search a group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    expect(await ui.localGroupRow.find()).toBeInTheDocument();
    expect(ui.managedGroupRow.get()).toBeInTheDocument();

    await user.type(await ui.searchInput.find(), 'local');

    expect(await ui.localGroupRow.find()).toBeInTheDocument();
    expect(ui.managedGroupRow.query()).not.toBeInTheDocument();
  });

  it('should be able load more group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    expect(await ui.localGroupRow.find()).toBeInTheDocument();
    expect(await screen.findAllByRole('row')).toHaveLength(3);

    await user.click(await ui.showMore.find());

    expect(await screen.findAllByRole('row')).toHaveLength(5);
  });
});

describe('in manage mode', () => {
  beforeEach(() => {
    handler.setIsManaged(true);
  });

  it('should not be able to create a group', async () => {
    renderGroupsApp();
    expect(await ui.createGroupButton.find()).toBeDisabled();
    expect(ui.infoManageMode.get()).toBeInTheDocument();
  });

  it('should ONLY be able to delete a local group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    expect(await ui.localGroupRowWithLocalBadge.find()).toBeInTheDocument();

    await user.click(await ui.localFilter.find());
    await user.click(await ui.localEditButton.find());

    expect(ui.updateButton.query()).not.toBeInTheDocument();

    await user.click(await ui.deleteButton.find());

    expect(await ui.deleteDialog.find()).toBeInTheDocument();

    await user.click(ui.deleteDialogButton.get());

    await waitFor(() => {
      expect(ui.localGroupRowWithLocalBadge.query()).not.toBeInTheDocument();
    });
  });

  it('should not be able to delete or edit a managed group', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    expect(await ui.managedGroupRow.find()).toBeInTheDocument();
    expect(ui.managedEditButton.query()).not.toBeInTheDocument();

    expect(ui.managedGroupEditMembersButton.query()).not.toBeInTheDocument();

    await user.click(ui.managedGroupViewMembersButton.get());
    expect(await ui.membersViewDialog.find()).toBeInTheDocument();

    expect(ui.memberAliceUser.get()).toBeInTheDocument();
    expect(ui.memberBobUser.get()).toBeInTheDocument();

    await user.type(ui.memberSearchInput.get(), 'b');

    expect(await ui.memberBobUser.find()).toBeInTheDocument();
    expect(ui.memberAliceUser.query()).not.toBeInTheDocument();
  });

  it('should render list of all groups', async () => {
    renderGroupsApp();

    expect(await ui.localAndManagedFilter.find()).toBeInTheDocument();

    expect(await ui.localGroupRowWithLocalBadge.find()).toBeInTheDocument();

    expect(await ui.managedGroupRow.find()).toBeInTheDocument();
  });

  it('should render list of managed groups', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    await user.click(await ui.managedFilter.find());

    expect(await ui.managedGroupRow.find()).toBeInTheDocument();
    expect(ui.localGroupRow.query()).not.toBeInTheDocument();
  });

  it('should render list of local groups', async () => {
    const user = userEvent.setup();
    renderGroupsApp();

    await user.click(await ui.localFilter.find());

    expect(await ui.localGroupRowWithLocalBadge.find()).toBeInTheDocument();
    expect(ui.managedGroupRow.query()).not.toBeInTheDocument();
  });

  describe('Github Provisioning', () => {
    beforeEach(() => {
      authenticationHandler.handleActivateGithubProvisioning();
      handler.setProvider(Provider.Github);
    });

    it('should display a success status when the synchronisation is a success', async () => {
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Success,
        executedAt: '2022-02-03T11:45:35+0200',
      });
      renderGroupsApp([Feature.GithubProvisioning]);

      expect(await ui.githubProvisioningSuccess.find()).toBeInTheDocument();
    });

    it('should display a success status even when another task is pending', async () => {
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Pending,
        executedAt: '2022-02-03T11:55:35+0200',
      });
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Success,
        executedAt: '2022-02-03T11:45:35+0200',
      });
      renderGroupsApp([Feature.GithubProvisioning]);

      expect(await ui.githubProvisioningSuccess.find()).toBeInTheDocument();
      expect(ui.githubProvisioningPending.query()).not.toBeInTheDocument();
    });

    it('should display an error alert when the synchronisation failed', async () => {
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: "T'es mauvais Jacques",
      });
      renderGroupsApp([Feature.GithubProvisioning]);

      expect(await ui.githubProvisioningAlert.find()).toBeInTheDocument();
      expect(screen.queryByText("T'es mauvais Jacques")).not.toBeInTheDocument();
      expect(ui.githubProvisioningSuccess.query()).not.toBeInTheDocument();
    });

    it('should display an error alert even when another task is in progress', async () => {
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.InProgress,
        executedAt: '2022-02-03T11:55:35+0200',
      });
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: "T'es mauvais Jacques",
      });
      renderGroupsApp([Feature.GithubProvisioning]);

      expect(await ui.githubProvisioningAlert.find()).toBeInTheDocument();
      expect(screen.queryByText("T'es mauvais Jacques")).not.toBeInTheDocument();
      expect(ui.githubProvisioningSuccess.query()).not.toBeInTheDocument();
      expect(ui.githubProvisioningInProgress.query()).not.toBeInTheDocument();
    });

    it('should render a github icon for github groups', async () => {
      handler.setProvider(Provider.Github);
      const user = userEvent.setup();
      renderGroupsApp();

      await user.click(await ui.managedFilter.find());

      expect(
        within(await ui.githubManagedGroupRow.find()).getByRole('img', { name: 'github' }),
      ).toBeInTheDocument();
    });
  });
});

function renderGroupsApp(featureList: Feature[] = []) {
  return renderApp('admin/groups', <GroupsApp />, { featureList });
}
