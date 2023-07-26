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

import { act, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import AuthenticationServiceMock from '../../../api/mocks/AuthenticationServiceMock';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import UserTokensMock from '../../../api/mocks/UserTokensMock';
import UsersServiceMock from '../../../api/mocks/UsersServiceMock';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../helpers/testSelector';
import { Feature } from '../../../types/features';
import { TaskStatuses } from '../../../types/tasks';
import { ChangePasswordResults, CurrentUser } from '../../../types/users';
import UsersApp from '../UsersApp';

jest.mock('../../../api/user-tokens');

const userHandler = new UsersServiceMock();
const tokenHandler = new UserTokensMock();
const componentsHandler = new ComponentsServiceMock();
const settingsHandler = new SettingsServiceMock();
const authenticationHandler = new AuthenticationServiceMock();

const ui = {
  createUserButton: byRole('button', { name: 'users.create_user' }),
  allFilter: byRole('button', { name: 'all' }),
  selectedFilter: byRole('button', { name: 'selected' }),
  unselectedFilter: byRole('button', { name: 'unselected' }),
  managedFilter: byRole('button', { name: 'managed' }),
  localFilter: byRole('button', { name: 'local' }),
  showMore: byRole('button', { name: 'show_more' }),
  aliceUpdateGroupButton: byRole('button', { name: 'users.update_users_groups.alice.merveille' }),
  aliceUpdateButton: byRole('button', { name: 'users.manage_user.alice.merveille' }),
  alicedDeactivateButton: byRole('button', { name: 'users.deactivate' }),
  bobUpdateGroupButton: byRole('button', { name: 'users.update_users_groups.bob.marley' }),
  bobUpdateButton: byRole('button', { name: 'users.manage_user.bob.marley' }),
  scmAddButton: byRole('button', { name: 'add_verb' }),
  createUserDialogButton: byRole('button', { name: 'create' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  reloadButton: byRole('button', { name: 'reload' }),
  doneButton: byRole('button', { name: 'done' }),
  changeButton: byRole('button', { name: 'change_verb' }),
  revokeButton: (name: string) => byRole('button', { name: `users.tokens.revoke_label.${name}` }),
  generateButton: byRole('button', { name: 'users.generate' }),
  sureButton: byRole('button', { name: 'users.tokens.sure' }),
  updateButton: byRole('button', { name: 'update_verb' }),
  deleteSCMButton: (value?: string) =>
    byRole('button', {
      name: `remove_x.users.create_user.scm_account_${value ? `x.${value}` : 'new'}`,
    }),
  userRows: byRole('row', {
    name: (accessibleName) => /^[A-Z]+ /.test(accessibleName),
  }),
  aliceRow: byRole('row', {
    name: (accessibleName) => accessibleName.startsWith('AM Alice Merveille alice.merveille '),
  }),
  aliceRowWithLocalBadge: byRole('row', {
    name: (accessibleName) =>
      accessibleName.startsWith(
        'AM Alice Merveille alice.merveille alice.merveille@wonderland.com local '
      ),
  }),
  bobRow: byRole('row', {
    name: (accessibleName) => accessibleName.startsWith('BM Bob Marley bob.marley '),
  }),
  charlieRow: byRole('row', {
    name: (accessibleName) => accessibleName.startsWith('CC Charlie Cox charlie.cox'),
  }),
  denisRow: byRole('row', {
    name: (accessibleName) => accessibleName.startsWith('DV Denis Villeneuve denis.villeneuve '),
  }),
  evaRow: byRole('row', {
    name: (accessibleName) => accessibleName.startsWith('EG Eva Green eva.green '),
  }),
  franckRow: byRole('row', {
    name: (accessibleName) => accessibleName.startsWith('FG Franck Grillo franck.grillo '),
  }),
  jackRow: byRole('row', { name: /Jack/ }),

  dialogGroups: byRole('dialog', { name: 'users.update_groups' }),
  getGroups: () => within(ui.dialogGroups.get()).getAllByRole('checkbox'),
  dialogTokens: byRole('dialog', { name: 'users.tokens' }),
  dialogPasswords: byRole('dialog', { name: 'my_profile.password.title' }),
  dialogUpdateUser: byRole('dialog', { name: 'users.update_user' }),
  dialogCreateUser: byRole('dialog', { name: 'users.create_user' }),
  dialogDeactivateUser: byRole('dialog', { name: 'users.deactivate_user' }),

  infoManageMode: byText(/users\.page\.managed_description/),
  description: byText('users.page.description'),
  deleteUserAlert: byText('delete-user-warning'),

  searchInput: byRole('searchbox', { name: 'search.search_by_login_or_name' }),
  activityFilter: byRole('combobox', { name: 'users.activity_filter.label' }),
  loginInput: byRole('textbox', { name: /login/ }),
  userNameInput: byRole('textbox', { name: /name/ }),
  emailInput: byRole('textbox', { name: /email/ }),
  passwordInput: byLabelText(/password/),
  dialogSCMInputs: byRole('textbox', { name: /users.create_user.scm_account/ }),
  dialogSCMInput: (value?: string) =>
    byRole('textbox', { name: `users.create_user.scm_account_${value ? `x.${value}` : 'new'}` }),
  oldPassword: byLabelText('my_profile.password.old', { selector: 'input', exact: false }),
  newPassword: byLabelText('my_profile.password.new', { selector: 'input', exact: false }),
  confirmPassword: byLabelText('my_profile.password.confirm', { selector: 'input', exact: false }),
  tokenNameInput: byRole('textbox', { name: 'users.tokens.name' }),
  deleteUserCheckbox: byRole('checkbox', { name: 'users.delete_user' }),
  githubProvisioningPending: byText(/synchronization_pending/),
  githubProvisioningInProgress: byText(/synchronization_in_progress/),
  githubProvisioningSuccess: byText(/synchronization_successful/),
  githubProvisioningAlert: byText(/synchronization_failed_short/),
};

beforeEach(() => {
  tokenHandler.reset();
  userHandler.reset();
  componentsHandler.reset();
  settingsHandler.reset();
  authenticationHandler.reset();
});

describe('different filters combinations', () => {
  beforeEach(() => {
    jest.useFakeTimers({
      advanceTimers: true,
      now: new Date('2023-07-05T07:08:59Z'),
    });
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should display all users with default filters', async () => {
    renderUsersApp();

    await act(async () => expect(await ui.userRows.findAll()).toHaveLength(6));
  });

  it('should display users filtered with text search', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => user.type(await ui.searchInput.find(), 'ar'));

    expect(await ui.userRows.findAll()).toHaveLength(2);
    expect(ui.bobRow.get()).toBeInTheDocument();
    expect(ui.charlieRow.get()).toBeInTheDocument();
  });

  it('should display local active SonarLint users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => user.click(await ui.localFilter.find()));
    await act(async () => {
      await selectEvent.select(
        ui.activityFilter.get(),
        'users.activity_filter.active_sonarlint_users'
      );
    });

    expect(await ui.userRows.findAll()).toHaveLength(1);
    expect(ui.charlieRow.get()).toBeInTheDocument();
  });

  it('should display managed active SonarQube users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => user.click(await ui.managedFilter.find()));
    await act(async () => {
      await selectEvent.select(
        ui.activityFilter.get(),
        'users.activity_filter.active_sonarqube_users'
      );
    });

    expect(await ui.userRows.findAll()).toHaveLength(1);
    expect(ui.denisRow.get()).toBeInTheDocument();
  });

  it('should display all inactive users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => user.click(await ui.allFilter.find()));
    await act(async () => {
      await selectEvent.select(ui.activityFilter.get(), 'users.activity_filter.inactive_users');
    });

    expect(await ui.userRows.findAll()).toHaveLength(2);
    expect(ui.evaRow.get()).toBeInTheDocument();
    expect(ui.franckRow.get()).toBeInTheDocument();
  });
});

describe('in non managed mode', () => {
  beforeEach(() => {
    userHandler.setIsManaged(false);
  });

  it('should allow the creation of user', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => expect(await ui.description.find()).toBeInTheDocument());
    expect(ui.createUserButton.get()).toBeEnabled();
    await user.click(ui.createUserButton.get());

    expect(await ui.dialogCreateUser.find()).toBeInTheDocument();

    await user.type(ui.loginInput.get(), 'Login');
    await user.type(ui.userNameInput.get(), 'Jack');
    await user.type(ui.passwordInput.get(), 'Password');
    // Add SCM account
    expect(ui.dialogSCMInputs.queryAll()).toHaveLength(0);
    await user.click(ui.scmAddButton.get());
    expect(ui.dialogSCMInputs.getAll()).toHaveLength(1);
    await user.type(ui.dialogSCMInput().get(), 'SCM');
    expect(ui.dialogSCMInput('SCM').get()).toBeInTheDocument();
    // Clear input to get an error on save
    await user.clear(ui.dialogSCMInput('SCM').get());
    await act(() => user.click(ui.createUserDialogButton.get()));
    expect(ui.dialogCreateUser.get()).toBeInTheDocument();
    expect(
      await within(ui.dialogCreateUser.get()).findByText('Error: Empty SCM')
    ).toBeInTheDocument();
    // Remove SCM account
    await user.click(ui.deleteSCMButton().get());
    expect(ui.dialogSCMInputs.queryAll()).toHaveLength(0);

    await act(() => user.click(ui.createUserDialogButton.get()));
    expect(ui.jackRow.get()).toBeInTheDocument();
    expect(ui.dialogCreateUser.query()).not.toBeInTheDocument();
  });

  it('should render all users', async () => {
    renderUsersApp();

    await act(async () => expect(await ui.aliceRow.find()).toBeInTheDocument());
    expect(ui.bobRow.get()).toBeInTheDocument();
    expect(ui.aliceRowWithLocalBadge.query()).not.toBeInTheDocument();
  });

  it('should be able load more users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => expect(await ui.aliceRow.find()).toBeInTheDocument());
    expect(ui.bobRow.get()).toBeInTheDocument();
    expect(ui.userRows.getAll()).toHaveLength(6);

    await act(async () => {
      await user.click(await ui.showMore.find());
    });

    expect(ui.userRows.getAll()).toHaveLength(8);
  });

  it('should be able to edit the groups of a user', async () => {
    const user = userEvent.setup();
    renderUsersApp();
    expect(await within(await ui.aliceRow.find()).findByText('2')).toBeInTheDocument();

    await act(async () => user.click(await ui.aliceUpdateGroupButton.find()));
    expect(await ui.dialogGroups.find()).toBeInTheDocument();

    expect(ui.getGroups()).toHaveLength(2);

    await act(async () => user.click(await ui.allFilter.find()));
    expect(ui.getGroups()).toHaveLength(3);

    await act(() => user.click(ui.unselectedFilter.get()));
    expect(ui.reloadButton.query()).not.toBeInTheDocument();
    await act(() => user.click(ui.getGroups()[0]));
    expect(await ui.reloadButton.find()).toBeInTheDocument();

    await act(() => user.click(ui.selectedFilter.get()));
    expect(ui.getGroups()).toHaveLength(3);

    await act(() => user.click(ui.doneButton.get()));
    expect(ui.dialogGroups.query()).not.toBeInTheDocument();
    expect(await within(await ui.aliceRow.find()).findByText('3')).toBeInTheDocument();

    await act(async () => user.click(await ui.aliceUpdateGroupButton.find()));

    await user.click(ui.selectedFilter.get());

    await act(() => user.click(ui.getGroups()[1]));
    expect(await ui.reloadButton.find()).toBeInTheDocument();
    await act(() => user.click(ui.reloadButton.get()));
    expect(ui.getGroups()).toHaveLength(2);

    await act(() => user.type(within(ui.dialogGroups.get()).getByRole('searchbox'), '3'));

    expect(ui.getGroups()).toHaveLength(1);

    await act(() => user.click(ui.doneButton.get()));
    expect(ui.dialogGroups.query()).not.toBeInTheDocument();
    expect(await within(await ui.aliceRow.find()).findByText('2')).toBeInTheDocument();
  });

  it('should update user', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => user.click(await ui.aliceUpdateButton.find()));
    await user.click(
      await within(ui.aliceRow.get()).findByRole('button', { name: 'update_details' })
    );
    expect(await ui.dialogUpdateUser.find()).toBeInTheDocument();

    expect(ui.userNameInput.get()).toHaveValue('Alice Merveille');
    expect(ui.emailInput.get()).toHaveValue('alice.merveille@wonderland.com');
    await user.type(ui.userNameInput.get(), '1');
    await user.clear(ui.emailInput.get());
    await user.type(ui.emailInput.get(), 'test@test.com');
    await act(() => user.click(ui.updateButton.get()));
    expect(ui.dialogUpdateUser.query()).not.toBeInTheDocument();
    expect(await screen.findByText('Alice Merveille1')).toBeInTheDocument();
    expect(await screen.findByText('test@test.com')).toBeInTheDocument();
  });

  it('should deactivate user', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => user.click(await ui.aliceUpdateButton.find()));
    await user.click(
      await within(ui.aliceRow.get()).findByRole('button', { name: 'users.deactivate' })
    );
    expect(await ui.dialogDeactivateUser.find()).toBeInTheDocument();
    expect(ui.deleteUserAlert.query()).not.toBeInTheDocument();
    await user.click(ui.deleteUserCheckbox.get());
    expect(await ui.deleteUserAlert.find()).toBeInTheDocument();

    await act(() =>
      user.click(
        within(ui.dialogDeactivateUser.get()).getByRole('button', { name: 'users.deactivate' })
      )
    );
    expect(ui.aliceRow.query()).not.toBeInTheDocument();
  });

  it('should change a password', async () => {
    const user = userEvent.setup();
    const currentUser = mockLoggedInUser({ login: 'alice.merveille' });
    renderUsersApp([], currentUser);

    await act(async () => user.click(await ui.aliceUpdateButton.find()));
    await user.click(
      await within(ui.aliceRow.get()).findByRole('button', { name: 'my_profile.password.title' })
    );
    expect(await ui.dialogPasswords.find()).toBeInTheDocument();

    expect(await ui.oldPassword.find()).toBeInTheDocument();

    expect(ui.changeButton.get()).toBeDisabled();

    await user.type(ui.oldPassword.get(), '123');
    await user.type(ui.newPassword.get(), '1234');
    await user.type(ui.confirmPassword.get(), '1234');

    expect(ui.changeButton.get()).toBeEnabled();
    expect(
      screen.queryByText(`user.${ChangePasswordResults.OldPasswordIncorrect}`)
    ).not.toBeInTheDocument();
    await user.click(ui.changeButton.get());
    expect(
      await within(ui.dialogPasswords.get()).findByText(
        `user.${ChangePasswordResults.OldPasswordIncorrect}`
      )
    ).toBeInTheDocument();

    await user.clear(ui.oldPassword.get());
    await user.clear(ui.newPassword.get());
    await user.clear(ui.confirmPassword.get());
    await user.type(ui.oldPassword.get(), 'test');
    await user.type(ui.newPassword.get(), 'test');
    await user.type(ui.confirmPassword.get(), 'test');

    expect(
      screen.queryByText(`user.${ChangePasswordResults.NewPasswordSameAsOld}`)
    ).not.toBeInTheDocument();
    await user.click(ui.changeButton.get());
    expect(
      await screen.findByText(`user.${ChangePasswordResults.NewPasswordSameAsOld}`)
    ).toBeInTheDocument();

    await user.clear(ui.newPassword.get());
    await user.clear(ui.confirmPassword.get());
    await user.type(ui.newPassword.get(), 'test2');
    await user.type(ui.confirmPassword.get(), 'test2');

    await user.click(ui.changeButton.get());

    expect(ui.dialogPasswords.query()).not.toBeInTheDocument();
  });
});

describe('in manage mode', () => {
  beforeEach(() => {
    userHandler.setIsManaged(true);
  });

  it('should not be able to create a user"', async () => {
    renderUsersApp();

    await act(async () => expect(await ui.infoManageMode.find()).toBeInTheDocument());
    expect(ui.createUserButton.get()).toBeDisabled();
  });

  it("should not be able to add/remove a user's group", async () => {
    renderUsersApp();

    await act(async () => expect(await ui.aliceRowWithLocalBadge.find()).toBeInTheDocument());
    expect(ui.aliceUpdateGroupButton.query()).not.toBeInTheDocument();
    expect(ui.bobRow.get()).toBeInTheDocument();
    expect(ui.bobUpdateGroupButton.query()).not.toBeInTheDocument();
  });

  it('should not be able to update / change password / deactivate a managed user', async () => {
    renderUsersApp();

    await act(async () => expect(await ui.bobRow.find()).toBeInTheDocument());
    expect(ui.bobUpdateButton.query()).not.toBeInTheDocument();
  });

  it('should ONLY be able to deactivate a local user', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => expect(await ui.aliceRowWithLocalBadge.find()).toBeInTheDocument());
    await user.click(ui.aliceUpdateButton.get());
    expect(await ui.alicedDeactivateButton.find()).toBeInTheDocument();
  });

  it('should render list of all users', async () => {
    renderUsersApp();

    await act(async () => expect(await ui.allFilter.find()).toBeInTheDocument());

    expect(ui.aliceRowWithLocalBadge.get()).toBeInTheDocument();
    expect(ui.bobRow.get()).toBeInTheDocument();
  });

  it('should render list of managed users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => expect(await ui.aliceRowWithLocalBadge.find()).toBeInTheDocument());

    await act(async () => user.click(await ui.managedFilter.find()));

    expect(await ui.bobRow.find()).toBeInTheDocument();
    expect(ui.aliceRowWithLocalBadge.query()).not.toBeInTheDocument();
  });

  it('should render list of local users', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () => {
      await user.click(await ui.localFilter.find());
    });

    expect(ui.bobRow.query()).not.toBeInTheDocument();
    expect(ui.aliceRowWithLocalBadge.get()).toBeInTheDocument();
  });

  it('should be able to change tokens of a user', async () => {
    const user = userEvent.setup();
    renderUsersApp();

    await act(async () =>
      user.click(
        await within(await ui.aliceRow.find()).findByRole('button', {
          name: 'users.update_tokens_for_x.Alice Merveille',
        })
      )
    );
    expect(await ui.dialogTokens.find()).toBeInTheDocument();

    const getTokensList = () => within(ui.dialogTokens.get()).getAllByRole('row');

    expect(getTokensList()).toHaveLength(3);

    await user.type(ui.tokenNameInput.get(), 'test');
    await user.click(ui.generateButton.get());

    // Not deleted because there is already token with name test
    expect(screen.queryByText('users.tokens.new_token_created.test')).not.toBeInTheDocument();
    expect(getTokensList()).toHaveLength(3);

    expect(ui.sureButton.query()).not.toBeInTheDocument();
    await user.click(ui.revokeButton('test').get());
    expect(await ui.sureButton.find()).toBeInTheDocument();
    await act(() => user.click(ui.sureButton.get()));

    expect(getTokensList()).toHaveLength(2);

    await act(() => user.click(ui.generateButton.get()));
    expect(getTokensList()).toHaveLength(3);
    expect(await screen.findByText('users.tokens.new_token_created.test')).toBeInTheDocument();

    await user.click(ui.doneButton.get());
    expect(ui.dialogTokens.query()).not.toBeInTheDocument();
  });

  describe('Github Provisioning', () => {
    beforeEach(() => {
      authenticationHandler.handleActivateGithubProvisioning();
    });

    it('should display a success status when the synchronisation is a success', async () => {
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Success,
        executedAt: '2022-02-03T11:45:35+0200',
      });
      renderUsersApp([Feature.GithubProvisioning]);
      await act(async () => expect(await ui.githubProvisioningSuccess.find()).toBeInTheDocument());
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
      renderUsersApp([Feature.GithubProvisioning]);
      await act(async () => expect(await ui.githubProvisioningSuccess.find()).toBeInTheDocument());
      expect(ui.githubProvisioningPending.query()).not.toBeInTheDocument();
    });

    it('should display an error alert when the synchronisation failed', async () => {
      authenticationHandler.addProvisioningTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: 'Error Message',
      });
      renderUsersApp([Feature.GithubProvisioning]);
      await act(async () => expect(await ui.githubProvisioningAlert.find()).toBeInTheDocument());
      expect(screen.queryByText('Error Message')).not.toBeInTheDocument();
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
        errorMessage: 'Error Message',
      });
      renderUsersApp([Feature.GithubProvisioning]);
      await act(async () => expect(await ui.githubProvisioningAlert.find()).toBeInTheDocument());
      expect(screen.queryByText('Error Message')).not.toBeInTheDocument();
      expect(ui.githubProvisioningSuccess.query()).not.toBeInTheDocument();
      expect(ui.githubProvisioningInProgress.query()).not.toBeInTheDocument();
    });
  });
});

it('should render external identity Providers', async () => {
  renderUsersApp();

  await act(async () => expect(await ui.charlieRow.find()).toHaveTextContent(/ExternalTest/));
  // logRoles(document.body);
  expect(await ui.denisRow.find()).toHaveTextContent(/test2: UnknownExternalProvider/);
});

it('accessibility', async () => {
  userHandler.setIsManaged(false);
  const user = userEvent.setup();
  renderUsersApp();

  // user list page should be accessible
  expect(await ui.aliceRow.find()).toBeInTheDocument();
  await expect(document.body).toHaveNoA11yViolations();

  // user creation dialog should be accessible
  await user.click(await ui.createUserButton.find());
  expect(await ui.dialogCreateUser.find()).toBeInTheDocument();
  await expect(ui.dialogCreateUser.get()).toHaveNoA11yViolations();
  await user.click(ui.cancelButton.get());

  // users group membership dialog should be accessible
  user.click(await ui.aliceUpdateGroupButton.find());
  expect(await ui.dialogGroups.find()).toBeInTheDocument();
  await expect(await ui.dialogGroups.find()).toHaveNoA11yViolations();
  await act(async () => {
    await user.click(ui.doneButton.get());
  });

  // user update dialog should be accessible
  await user.click(await ui.aliceUpdateButton.find());
  await user.click(await ui.aliceRow.byRole('button', { name: 'update_details' }).find());
  expect(await ui.dialogUpdateUser.find()).toBeInTheDocument();
  await expect(await ui.dialogUpdateUser.find()).toHaveNoA11yViolations();
  await user.click(ui.cancelButton.get());

  // user tokens dialog should be accessible
  user.click(
    await ui.aliceRow
      .byRole('button', {
        name: 'users.update_tokens_for_x.Alice Merveille',
      })
      .find()
  );
  expect(await ui.dialogTokens.find()).toBeInTheDocument();
  await expect(await ui.dialogTokens.find()).toHaveNoA11yViolations();
  await user.click(ui.doneButton.get());

  // user password dialog should be accessible
  await user.click(await ui.aliceUpdateButton.find());
  await user.click(
    await ui.aliceRow.byRole('button', { name: 'my_profile.password.title' }).find()
  );
  expect(await ui.dialogPasswords.find()).toBeInTheDocument();
  await expect(await ui.dialogPasswords.find()).toHaveNoA11yViolations();
});

function renderUsersApp(featureList: Feature[] = [], currentUser?: CurrentUser) {
  // eslint-disable-next-line testing-library/no-unnecessary-act
  renderApp('admin/users', <UsersApp />, {
    currentUser: mockCurrentUser(currentUser),
    featureList,
  });
}
