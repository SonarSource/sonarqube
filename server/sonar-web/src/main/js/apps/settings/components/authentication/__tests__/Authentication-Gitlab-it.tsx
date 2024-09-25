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
import { within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import ComputeEngineServiceMock from '../../../../../api/mocks/ComputeEngineServiceMock';
import GitlabProvisioningServiceMock from '../../../../../api/mocks/GitlabProvisioningServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { mockGitlabConfiguration } from '../../../../../helpers/mocks/alm-integrations';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import { Feature } from '../../../../../types/features';
import { ProvisioningType } from '../../../../../types/provisioning';
import { TaskStatuses, TaskTypes } from '../../../../../types/tasks';
import Authentication from '../Authentication';

let handler: GitlabProvisioningServiceMock;
let system: SystemServiceMock;
let settingsHandler: SettingsServiceMock;
let computeEngineHandler: ComputeEngineServiceMock;

beforeEach(() => {
  handler = new GitlabProvisioningServiceMock();
  system = new SystemServiceMock();
  settingsHandler = new SettingsServiceMock();
  computeEngineHandler = new ComputeEngineServiceMock();
});

afterEach(() => {
  handler.reset();
  settingsHandler.reset();
  system.reset();
  computeEngineHandler.reset();
});

const glContainer = byRole('tabpanel', { name: 'gitlab GitLab' });

const ui = {
  noGitlabConfiguration: glContainer.byText('settings.authentication.gitlab.form.not_configured'),
  createConfigButton: glContainer.byRole('button', {
    name: 'settings.authentication.form.create',
  }),
  editConfigButton: glContainer.byRole('button', {
    name: 'settings.authentication.form.edit',
  }),
  editMappingButton: glContainer.byRole('button', {
    name: 'settings.authentication.configuration.roles_mapping.button_label',
  }),
  deleteConfigButton: glContainer.byRole('button', {
    name: 'settings.authentication.form.delete',
  }),
  enableConfigButton: glContainer.byRole('button', {
    name: 'settings.authentication.form.enable',
  }),
  disableConfigButton: glContainer.byRole('button', {
    name: 'settings.authentication.form.disable',
  }),
  createDialog: byRole('dialog', {
    name: 'settings.authentication.gitlab.form.create',
  }),
  editDialog: byRole('dialog', {
    name: 'settings.authentication.gitlab.form.edit',
  }),
  applicationId: byRole('textbox', {
    name: 'property.applicationId.name',
  }),
  url: byRole('textbox', { name: 'property.url.name' }),
  secret: byRole('textbox', {
    name: 'property.secret.name',
  }),
  synchronizeGroups: byRole('switch', {
    description: 'property.synchronizeGroups.description',
  }),
  saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
  jitProvisioningRadioButton: glContainer.byRole('radio', {
    name: /settings.authentication.gitlab.provisioning_at_login/,
  }),
  autoProvisioningRadioButton: glContainer.byRole('radio', {
    name: /settings.authentication.gitlab.form.provisioning_with_gitlab/,
  }),
  jitAllowUsersToSignUpToggle: byRole('switch', {
    description: 'property.allowUsersToSignUp.description',
  }),
  autoProvisioningToken: byRole('textbox', {
    name: 'property.provisioningToken.name',
  }),
  autoProvisioningUpdateTokenButton: byRole('button', {
    name: 'settings.almintegration.form.secret.update_field',
  }),
  groups: byRole('textbox', {
    name: 'property.allowedGroups.name',
  }),
  deleteGroupButton: byRole('button', { name: /delete_value/ }),
  removeProvisioniongGroup: byRole('button', {
    name: /settings.definition.delete_value.property.allowedGroups.name./,
  }),
  saveProvisioning: glContainer.byRole('button', { name: 'save' }),
  cancelProvisioningChanges: glContainer.byRole('button', { name: 'cancel' }),
  confirmAutoProvisioningDialog: byRole('dialog', {
    name: 'settings.authentication.gitlab.confirm.AUTO_PROVISIONING',
  }),
  confirmJitProvisioningDialog: byRole('alertdialog', {
    name: 'settings.authentication.gitlab.confirm.JIT',
  }),
  confirmInsecureProvisioningDialog: byRole('alertdialog', {
    name: 'settings.authentication.gitlab.confirm.insecure',
  }),
  confirmProvisioningChange: byRole('button', {
    name: 'settings.authentication.gitlab.provisioning_change.confirm_changes',
  }),
  syncSummary: glContainer.byText(/Test summary/),
  syncWarning: glContainer.byText(/Warning/),
  gitlabProvisioningPending: glContainer
    .byRole('list')
    .byRole('status')
    .byText(/synchronization_pending/),
  gitlabProvisioningInProgress: glContainer
    .byRole('list')
    .byRole('status')
    .byText(/synchronization_in_progress/),
  gitlabProvisioningSuccess: glContainer.byText(/synchronization_successful/),
  gitlabProvisioningAlert: glContainer.byText(/synchronization_failed/),
  gitlabConfigurationStatus: glContainer.byRole('status', {
    name: /settings.authentication.gitlab.configuration/,
  }),
  gitlabMissingSecretErrorMessage: byText(
    'settings.authentication.gitlab.form.secret.required_for_url_change',
  ),
  testConfiguration: glContainer.byRole('button', {
    name: 'settings.authentication.configuration.test',
  }),
  autoRadioButton: byRole('radio', {
    name: 'settings.authentication.confirm_auto_provisioning.auto.label',
  }),
  jitRadioButton: byRole('radio', {
    name: 'settings.authentication.confirm_auto_provisioning.jit.label',
  }),
  confirmChoiceButton: byRole('button', {
    name: 'settings.authentication.confirm_auto_provisioning.confirm_choice',
  }),
  consentDialog: byRole('dialog', {
    name: 'settings.authentication.confirm_auto_provisioning.header',
  }),
  mappingRow: byRole('dialog', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.title.alm.gitlab',
  }).byRole('row'),
  mappingCheckbox: byRole('checkbox'),
  mappingDialogClose: byRole('dialog', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.title.alm.gitlab',
  }).byRole('button', {
    name: 'close',
  }),
  customRoleInput: byRole('textbox', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.add_custom_role',
  }),
  customRoleAddBtn: byRole('dialog', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.title.alm.gitlab',
  }).byRole('button', { name: 'add_verb' }),
  roleExistsError: byRole('dialog', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.title.alm.gitlab',
  }).byText('settings.authentication.configuration.roles_mapping.role_exists'),
  emptyRoleError: byRole('dialog', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.title.alm.gitlab',
  }).byText('settings.authentication.configuration.roles_mapping.empty_custom_role'),
  deleteCustomRoleCustom2: byRole('button', {
    name: 'settings.authentication.configuration.roles_mapping.dialog.delete_custom_role.custom2',
  }),
  getMappingRowByRole: (text: string) =>
    ui.mappingRow.getAll().find((row) => within(row).queryByText(text) !== null),
};

it('should create a Gitlab configuration and disable it with proper validation', async () => {
  handler.setGitlabConfigurations([]);
  renderAuthentication();
  const user = userEvent.setup();

  expect(await ui.noGitlabConfiguration.find()).toBeInTheDocument();
  expect(ui.createConfigButton.get()).toBeInTheDocument();

  await user.click(ui.createConfigButton.get());
  expect(await ui.createDialog.find()).toBeInTheDocument();
  await user.type(ui.applicationId.get(), '123');
  expect(ui.saveConfigButton.get()).toBeDisabled();
  await user.type(ui.url.get(), 'https://company.ui.com');
  await user.type(ui.secret.get(), '123');
  expect(ui.saveConfigButton.get()).toBeEnabled();
  await user.click(ui.synchronizeGroups.get());
  await user.click(ui.saveConfigButton.get());

  expect(await ui.editConfigButton.find()).toBeInTheDocument();
  expect(ui.noGitlabConfiguration.query()).not.toBeInTheDocument();
  expect(glContainer.get()).toHaveTextContent('https://company.ui.com');

  expect(ui.disableConfigButton.get()).toBeInTheDocument();
  await user.click(ui.disableConfigButton.get());
  expect(ui.enableConfigButton.get()).toBeInTheDocument();
  expect(ui.disableConfigButton.query()).not.toBeInTheDocument();
});

it('should edit a configuration with proper validation and delete it', async () => {
  const user = userEvent.setup();
  renderAuthentication();

  expect(await ui.editConfigButton.find()).toBeInTheDocument();
  expect(glContainer.get()).toHaveTextContent('URL');
  expect(ui.disableConfigButton.get()).toBeInTheDocument();
  expect(ui.deleteConfigButton.get()).toBeInTheDocument();
  expect(ui.deleteConfigButton.get()).toBeDisabled();

  await user.click(ui.editConfigButton.get());
  expect(await ui.editDialog.find()).toBeInTheDocument();
  expect(ui.url.get()).toHaveValue('URL');
  expect(ui.applicationId.get()).toBeInTheDocument();
  expect(ui.secret.query()).not.toBeInTheDocument();
  expect(ui.synchronizeGroups.get()).toBeChecked();

  expect(ui.applicationId.get()).toBeInTheDocument();
  await user.clear(ui.applicationId.get());
  expect(ui.saveConfigButton.get()).toBeDisabled();
  await user.type(ui.applicationId.get(), '456');
  expect(ui.saveConfigButton.get()).toBeEnabled();

  expect(ui.url.get()).toBeInTheDocument();
  await user.clear(ui.url.get());
  expect(ui.saveConfigButton.get()).toBeDisabled();
  await user.type(ui.url.get(), 'www.internet.com');
  expect(ui.saveConfigButton.get()).toBeDisabled();
  expect(ui.gitlabMissingSecretErrorMessage.get()).toBeInTheDocument();
  await user.click(ui.autoProvisioningUpdateTokenButton.get());
  await user.type(ui.secret.get(), '123');
  expect(ui.gitlabMissingSecretErrorMessage.query()).not.toBeInTheDocument();
  expect(ui.saveConfigButton.get()).toBeEnabled();
  await user.click(ui.saveConfigButton.get());

  expect(glContainer.get()).not.toHaveTextContent('URL');
  expect(glContainer.get()).toHaveTextContent('www.internet.com');

  expect(ui.disableConfigButton.get()).toBeInTheDocument();
  await user.click(ui.disableConfigButton.get());
  expect(await ui.enableConfigButton.find()).toBeInTheDocument();
  expect(ui.deleteConfigButton.get()).toBeEnabled();
  await user.click(ui.deleteConfigButton.get());
  expect(await ui.noGitlabConfiguration.find()).toBeInTheDocument();
  expect(ui.editConfigButton.query()).not.toBeInTheDocument();
});

it('should be able to save just-in-time with no organizations', async () => {
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.jitProvisioningRadioButton.find()).toBeChecked();

  expect(ui.groups.get()).toHaveValue('Cypress Hill');
  expect(await ui.saveProvisioning.find()).toBeDisabled();
  await user.click(ui.deleteGroupButton.get());
  expect(await ui.saveProvisioning.find()).toBeEnabled();
});

it('should not be able to save Auto provisioning with no organizations', async () => {
  const user = userEvent.setup();
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.auto,
      allowedGroups: ['D12'],
      isProvisioningTokenSet: true,
    }),
  ]);
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.autoProvisioningRadioButton.find()).toBeChecked();

  expect(ui.groups.get()).toHaveValue('D12');
  expect(ui.saveProvisioning.get()).toBeDisabled();
  await user.click(ui.deleteGroupButton.get());
  expect(await ui.saveProvisioning.find()).toBeDisabled();
});

it('should change from just-in-time to Auto Provisioning if auto was never set before', async () => {
  const user = userEvent.setup();
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.jit,
      allowedGroups: [],
      isProvisioningTokenSet: false,
    }),
  ]);
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.editConfigButton.find()).toBeInTheDocument();
  expect(ui.jitProvisioningRadioButton.get()).toBeChecked();

  await user.click(ui.autoProvisioningRadioButton.get());
  expect(await ui.autoProvisioningRadioButton.find()).toBeEnabled();
  expect(ui.saveProvisioning.get()).toBeDisabled();

  await user.type(ui.autoProvisioningToken.get(), 'JRR Tolkien');
  expect(await ui.saveProvisioning.find()).toBeDisabled();

  await user.type(ui.groups.get(), 'Run DMC');
  expect(await ui.saveProvisioning.find()).toBeEnabled();
  await user.click(ui.deleteGroupButton.get());
  expect(await ui.saveProvisioning.find()).toBeDisabled();

  await user.type(ui.groups.get(), 'Public Enemy');
  expect(await ui.saveProvisioning.find()).toBeEnabled();
});

it('should change from just-in-time to Auto Provisioning if auto was set before', async () => {
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.jit,
      allowedGroups: ['D12'],
      isProvisioningTokenSet: true,
    }),
  ]);
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.editConfigButton.find()).toBeInTheDocument();
  expect(ui.jitProvisioningRadioButton.get()).toBeChecked();

  user.click(ui.autoProvisioningRadioButton.get());
  expect(await ui.autoProvisioningRadioButton.find()).toBeEnabled();
  expect(await ui.saveProvisioning.find()).toBeEnabled();

  expect(ui.groups.get()).toHaveValue('D12');
  await user.click(ui.deleteGroupButton.get());
  expect(await ui.saveProvisioning.find()).toBeDisabled();
  await user.type(ui.groups.get(), 'Wu Tang Clan');

  expect(ui.saveProvisioning.get()).toBeEnabled();
});

it('should change from auto provisioning to JIT with proper validation', async () => {
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.auto,
      allowedGroups: ['D12'],
      isProvisioningTokenSet: true,
    }),
  ]);
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.editConfigButton.find()).toBeInTheDocument();

  expect(ui.jitProvisioningRadioButton.get()).not.toBeChecked();
  expect(ui.autoProvisioningRadioButton.get()).toBeChecked();

  expect(ui.autoProvisioningToken.query()).not.toBeInTheDocument();
  expect(ui.autoProvisioningUpdateTokenButton.get()).toBeInTheDocument();

  await user.click(ui.jitProvisioningRadioButton.get());
  expect(await ui.jitProvisioningRadioButton.find()).toBeChecked();

  expect(await ui.saveProvisioning.find()).toBeEnabled();

  await user.click(ui.jitAllowUsersToSignUpToggle.get());
  await user.click(ui.deleteGroupButton.get());

  await user.click(ui.saveProvisioning.get());
  expect(
    ui.confirmJitProvisioningDialog
      .byText('settings.authentication.gitlab.provisioning_change.insecure_config')
      .get(),
  ).toBeInTheDocument();
  await user.click(ui.confirmProvisioningChange.get());
  expect(ui.confirmJitProvisioningDialog.query()).not.toBeInTheDocument();

  expect(ui.jitProvisioningRadioButton.get()).toBeChecked();
  expect(await ui.saveProvisioning.find()).toBeDisabled();
});

it('should show configuration warning with jit provisioning and no groups', async () => {
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.jit,
      allowedGroups: [],
      isProvisioningTokenSet: true,
    }),
  ]);
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.editConfigButton.find()).toBeInTheDocument();

  await user.click(ui.jitAllowUsersToSignUpToggle.get());
  await user.click(ui.saveProvisioning.get());

  expect(
    ui.confirmInsecureProvisioningDialog
      .byText('settings.authentication.gitlab.provisioning_change.insecure_config')
      .get(),
  ).toBeInTheDocument();

  await user.click(ui.confirmProvisioningChange.get());
  expect(ui.confirmJitProvisioningDialog.query()).not.toBeInTheDocument();

  expect(ui.jitProvisioningRadioButton.get()).toBeChecked();
  expect(await ui.saveProvisioning.find()).toBeDisabled();
});

it('should be able to allow user to sign up for JIT with proper validation', async () => {
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.jit,
    }),
  ]);
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.editConfigButton.find()).toBeInTheDocument();

  expect(ui.jitProvisioningRadioButton.get()).toBeChecked();
  expect(ui.autoProvisioningRadioButton.get()).not.toBeChecked();

  expect(ui.jitAllowUsersToSignUpToggle.get()).not.toBeChecked();

  expect(ui.saveProvisioning.get()).toBeDisabled();
  await user.click(ui.jitAllowUsersToSignUpToggle.get());
  expect(ui.saveProvisioning.get()).toBeEnabled();
  await user.click(ui.jitAllowUsersToSignUpToggle.get());
  expect(ui.saveProvisioning.get()).toBeDisabled();
  await user.click(ui.jitAllowUsersToSignUpToggle.get());

  await user.click(ui.saveProvisioning.get());

  expect(ui.jitProvisioningRadioButton.get()).toBeChecked();
  expect(ui.jitAllowUsersToSignUpToggle.get()).toBeChecked();
  expect(await ui.saveProvisioning.find()).toBeDisabled();
});

it('should be able to edit token for Auto provisioning with proper validation', async () => {
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.auto,
      allowedGroups: ['Cypress Hill', 'Public Enemy'],
      isProvisioningTokenSet: true,
    }),
  ]);
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.autoProvisioningRadioButton.find()).toBeChecked();
  expect(ui.autoProvisioningUpdateTokenButton.get()).toBeInTheDocument();

  expect(ui.saveProvisioning.get()).toBeDisabled();

  // Changing the Provisioning token should enable save
  await user.click(ui.autoProvisioningUpdateTokenButton.get());
  expect(ui.saveProvisioning.get()).toBeDisabled();
  await user.click(ui.cancelProvisioningChanges.get());
  expect(ui.saveProvisioning.get()).toBeDisabled();
});

it('should be able to reset Auto Provisioning changes', async () => {
  handler.setGitlabConfigurations([
    mockGitlabConfiguration({
      allowUsersToSignUp: false,
      enabled: true,
      provisioningType: ProvisioningType.auto,
      allowedGroups: ['Cypress Hill', 'Public Enemy'],
      isProvisioningTokenSet: true,
    }),
  ]);
  const user = userEvent.setup();
  renderAuthentication([Feature.GitlabProvisioning]);

  expect(await ui.autoProvisioningRadioButton.find()).toBeChecked();

  await user.click(ui.autoProvisioningUpdateTokenButton.get());
  await user.type(ui.autoProvisioningToken.get(), 'ToToken!');
  expect(ui.saveProvisioning.get()).toBeEnabled();
  await user.click(ui.cancelProvisioningChanges.get());
  expect(ui.saveProvisioning.get()).toBeDisabled();
});

describe('Gitlab Provisioning', () => {
  beforeEach(() => {
    jest.useFakeTimers({
      advanceTimers: true,
      now: new Date('2022-02-04T12:00:59Z'),
    });
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        enabled: true,
        provisioningType: ProvisioningType.auto,
        allowedGroups: ['Test'],
      }),
    ]);
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should display a success status when the synchronisation is a success', async () => {
    computeEngineHandler.addTask({
      status: TaskStatuses.Success,
      executedAt: '2022-02-03T11:45:35+0200',
      infoMessages: ['Test summary'],
      type: TaskTypes.GitlabProvisioning,
    });

    renderAuthentication([Feature.GitlabProvisioning]);
    expect(await ui.gitlabProvisioningSuccess.find()).toBeInTheDocument();
    expect(ui.syncSummary.get()).toBeInTheDocument();
  });

  it('should display a success status even when another task is pending', async () => {
    computeEngineHandler.addTask({
      status: TaskStatuses.Pending,
      executedAt: '2022-02-03T11:55:35+0200',
      type: TaskTypes.GitlabProvisioning,
    });
    computeEngineHandler.addTask({
      status: TaskStatuses.Success,
      executedAt: '2022-02-03T11:45:35+0200',
      type: TaskTypes.GitlabProvisioning,
    });
    renderAuthentication([Feature.GitlabProvisioning]);
    expect(await ui.gitlabProvisioningSuccess.find()).toBeInTheDocument();
    expect(ui.gitlabProvisioningPending.get()).toBeInTheDocument();
  });

  it('should display an error alert when the synchronisation failed', async () => {
    computeEngineHandler.addTask({
      status: TaskStatuses.Failed,
      executedAt: '2022-02-03T11:45:35+0200',
      errorMessage: "T'es mauvais Jacques",
      type: TaskTypes.GitlabProvisioning,
    });
    renderAuthentication([Feature.GitlabProvisioning]);
    expect(await ui.gitlabProvisioningAlert.find()).toBeInTheDocument();
    expect(glContainer.get()).toHaveTextContent("T'es mauvais Jacques");
    expect(ui.gitlabProvisioningSuccess.query()).not.toBeInTheDocument();
  });

  it('should display an error alert even when another task is in progress', async () => {
    computeEngineHandler.addTask({
      status: TaskStatuses.InProgress,
      executedAt: '2022-02-03T11:55:35+0200',
      type: TaskTypes.GitlabProvisioning,
    });
    computeEngineHandler.addTask({
      status: TaskStatuses.Failed,
      executedAt: '2022-02-03T11:45:35+0200',
      errorMessage: "T'es mauvais Jacques",
      type: TaskTypes.GitlabProvisioning,
    });
    renderAuthentication([Feature.GitlabProvisioning]);
    expect(await ui.gitlabProvisioningAlert.find()).toBeInTheDocument();
    expect(glContainer.get()).toHaveTextContent("T'es mauvais Jacques");
    expect(ui.gitlabProvisioningSuccess.query()).not.toBeInTheDocument();
    expect(ui.gitlabProvisioningInProgress.get()).toBeInTheDocument();
  });

  it('should show warning', async () => {
    computeEngineHandler.addTask({
      status: TaskStatuses.Success,
      warnings: ['Warning'],
      infoMessages: ['Test summary'],
      type: TaskTypes.GitlabProvisioning,
    });
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.syncWarning.find()).toBeInTheDocument();
    expect(ui.syncSummary.get()).toBeInTheDocument();
  });

  it('should show configuration validity', async () => {
    const user = userEvent.setup();
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.gitlabConfigurationStatus.find()).toHaveTextContent(
      'settings.authentication.gitlab.configuration.valid.AUTO_PROVISIONING',
    );
    await user.click(ui.jitProvisioningRadioButton.get());
    await user.click(ui.saveProvisioning.get());
    await user.click(ui.confirmProvisioningChange.get());
    expect(ui.gitlabConfigurationStatus.get()).toHaveTextContent(
      'settings.authentication.gitlab.configuration.valid.JIT',
    );
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({ ...handler.gitlabConfigurations[0], errorMessage: 'ERROR' }),
    ]);
    await user.click(ui.testConfiguration.get());
    expect(glContainer.get()).toHaveTextContent('ERROR');
    await user.click(ui.disableConfigButton.get());
    expect(ui.gitlabConfigurationStatus.query()).not.toBeInTheDocument();
  });

  it('should display a modal if user was already using auto and continue using auto provisioning', async () => {
    const user = userEvent.setup();

    settingsHandler.set('sonar.auth.gitlab.userConsentForPermissionProvisioningRequired', '');
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        provisioningType: ProvisioningType.auto,
        allowedGroups: ['D12'],
        isProvisioningTokenSet: true,
      }),
    ]);
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.consentDialog.find()).toBeInTheDocument();
    await user.click(ui.autoRadioButton.get());
    await user.click(ui.confirmChoiceButton.get());

    expect(await ui.autoProvisioningRadioButton.find()).toBeChecked();
    expect(ui.consentDialog.query()).not.toBeInTheDocument();
  });

  it('should display a modal if user was already using auto and switch to JIT', async () => {
    const user = userEvent.setup();

    settingsHandler.set('sonar.auth.gitlab.userConsentForPermissionProvisioningRequired', '');
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        provisioningType: ProvisioningType.auto,
        allowedGroups: ['D12'],
        isProvisioningTokenSet: true,
      }),
    ]);
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.consentDialog.find()).toBeInTheDocument();
    await user.click(ui.jitRadioButton.get());
    await user.click(ui.confirmChoiceButton.get());

    expect(await ui.jitProvisioningRadioButton.find()).toBeChecked();
    expect(ui.consentDialog.query()).not.toBeInTheDocument();
  });

  it('should sort mapping rows', async () => {
    const user = userEvent.setup();
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        provisioningType: ProvisioningType.auto,
        allowedGroups: ['D12'],
        isProvisioningTokenSet: true,
      }),
    ]);
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.editMappingButton.find()).toBeInTheDocument();
    await user.click(ui.editMappingButton.get());

    const rows = (await ui.mappingRow.findAll()).filter(
      (row) => within(row).queryAllByRole('checkbox').length > 0,
    );

    expect(rows).toHaveLength(5);

    expect(rows[0]).toHaveTextContent('guest');
    expect(rows[1]).toHaveTextContent('reporter');
    expect(rows[2]).toHaveTextContent('developer');
    expect(rows[3]).toHaveTextContent('maintainer');
    expect(rows[4]).toHaveTextContent('owner');
  });

  it('should apply new mapping and new provisioning type at the same time', async () => {
    const user = userEvent.setup();
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        provisioningType: ProvisioningType.jit,
        allowedGroups: ['D12'],
        isProvisioningTokenSet: true,
      }),
    ]);
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.jitProvisioningRadioButton.find()).toBeChecked();
    expect(ui.editMappingButton.query()).not.toBeInTheDocument();
    await user.click(ui.autoProvisioningRadioButton.get());
    expect(await ui.editMappingButton.find()).toBeInTheDocument();
    await user.click(ui.editMappingButton.get());
    expect(await ui.mappingRow.findAll()).toHaveLength(7);

    let guestCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('guest'));
    let ownerCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('owner'));

    expect(guestCheckboxes[0]).toBeChecked();
    expect(guestCheckboxes[1]).toBeChecked();
    expect(guestCheckboxes[2]).not.toBeChecked();
    expect(guestCheckboxes[3]).not.toBeChecked();
    expect(guestCheckboxes[4]).not.toBeChecked();
    expect(guestCheckboxes[5]).not.toBeChecked();
    expect(ownerCheckboxes[0]).toBeChecked();
    expect(ownerCheckboxes[1]).toBeChecked();
    expect(ownerCheckboxes[2]).toBeChecked();
    expect(ownerCheckboxes[3]).toBeChecked();
    expect(ownerCheckboxes[4]).toBeChecked();
    expect(ownerCheckboxes[5]).toBeChecked();

    await user.click(guestCheckboxes[0]);
    await user.click(guestCheckboxes[5]);
    await user.click(ownerCheckboxes[5]);
    await user.click(ui.mappingDialogClose.get());

    await user.click(ui.saveProvisioning.get());
    await user.click(ui.confirmProvisioningChange.get());

    // Clean local mapping state
    await user.click(ui.jitProvisioningRadioButton.get());
    await user.click(ui.autoProvisioningRadioButton.get());

    await user.click(ui.editMappingButton.get());
    guestCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('guest'));
    ownerCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('owner'));

    expect(guestCheckboxes[0]).not.toBeChecked();
    expect(guestCheckboxes[5]).toBeChecked();
    expect(ownerCheckboxes[5]).not.toBeChecked();
    await user.click(ui.mappingDialogClose.get());
  });

  it('should add/remove/update custom roles', async () => {
    const user = userEvent.setup();
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        provisioningType: ProvisioningType.auto,
        allowedGroups: ['D12'],
        isProvisioningTokenSet: true,
      }),
    ]);
    handler.addGitLabCustomRole('custom1', ['user', 'codeViewer', 'scan']);
    handler.addGitLabCustomRole('custom2', ['user', 'codeViewer', 'issueAdmin', 'scan']);
    renderAuthentication([Feature.GitlabProvisioning]);

    expect(await ui.saveProvisioning.find()).toBeDisabled();
    await user.click(ui.editMappingButton.get());

    const rows = (await ui.mappingRow.findAll()).filter(
      (row) => within(row).queryAllByRole('checkbox').length > 0,
    );

    expect(rows).toHaveLength(7);

    let custom1Checkboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('custom1'));

    expect(custom1Checkboxes[0]).toBeChecked();
    expect(custom1Checkboxes[1]).toBeChecked();
    expect(custom1Checkboxes[2]).not.toBeChecked();
    expect(custom1Checkboxes[3]).not.toBeChecked();
    expect(custom1Checkboxes[4]).not.toBeChecked();
    expect(custom1Checkboxes[5]).toBeChecked();

    await user.click(custom1Checkboxes[1]);
    await user.click(custom1Checkboxes[2]);

    await user.click(ui.deleteCustomRoleCustom2.get());

    expect(ui.customRoleInput.get()).toHaveValue('');
    await user.type(ui.customRoleInput.get(), 'guest');
    await user.click(ui.customRoleAddBtn.get());
    expect(await ui.roleExistsError.find()).toBeInTheDocument();
    expect(ui.customRoleAddBtn.get()).toBeDisabled();
    await user.clear(ui.customRoleInput.get());
    expect(ui.roleExistsError.query()).not.toBeInTheDocument();
    await user.type(ui.customRoleInput.get(), 'custom1');
    await user.click(ui.customRoleAddBtn.get());
    expect(await ui.roleExistsError.find()).toBeInTheDocument();
    expect(ui.customRoleAddBtn.get()).toBeDisabled();
    await user.clear(ui.customRoleInput.get());
    await user.type(ui.customRoleInput.get(), 'custom3');
    expect(ui.roleExistsError.query()).not.toBeInTheDocument();
    expect(ui.customRoleAddBtn.get()).toBeEnabled();
    await user.click(ui.customRoleAddBtn.get());

    let custom3Checkboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('custom3'));
    expect(custom3Checkboxes[0]).toBeChecked();
    expect(custom3Checkboxes[1]).not.toBeChecked();
    expect(custom3Checkboxes[2]).not.toBeChecked();
    expect(custom3Checkboxes[3]).not.toBeChecked();
    expect(custom3Checkboxes[4]).not.toBeChecked();
    expect(custom3Checkboxes[5]).not.toBeChecked();
    await user.click(custom3Checkboxes[0]);
    expect(await ui.emptyRoleError.find()).toBeInTheDocument();
    expect(ui.mappingDialogClose.get()).toBeDisabled();
    await user.click(custom3Checkboxes[1]);
    expect(ui.emptyRoleError.query()).not.toBeInTheDocument();
    expect(ui.mappingDialogClose.get()).toBeEnabled();
    await user.click(ui.mappingDialogClose.get());

    expect(await ui.saveProvisioning.find()).toBeEnabled();
    await user.click(ui.saveProvisioning.get());

    // Clean local mapping state
    await user.click(ui.jitProvisioningRadioButton.get());
    await user.click(ui.autoProvisioningRadioButton.get());

    await user.click(ui.editMappingButton.get());

    expect(
      (await ui.mappingRow.findAll()).filter(
        (row) => within(row).queryAllByRole('checkbox').length > 0,
      ),
    ).toHaveLength(7);
    custom1Checkboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('custom1'));
    custom3Checkboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('custom3'));
    expect(ui.getMappingRowByRole('custom2')).toBeUndefined();
    expect(custom1Checkboxes[0]).toBeChecked();
    expect(custom1Checkboxes[1]).not.toBeChecked();
    expect(custom1Checkboxes[2]).toBeChecked();
    expect(custom1Checkboxes[3]).not.toBeChecked();
    expect(custom1Checkboxes[4]).not.toBeChecked();
    expect(custom1Checkboxes[5]).toBeChecked();
    expect(custom3Checkboxes[0]).not.toBeChecked();
    expect(custom3Checkboxes[1]).toBeChecked();
    expect(custom3Checkboxes[2]).not.toBeChecked();
    expect(custom3Checkboxes[3]).not.toBeChecked();
    expect(custom3Checkboxes[4]).not.toBeChecked();
    expect(custom3Checkboxes[5]).not.toBeChecked();
    await user.click(ui.mappingDialogClose.get());
  });
});

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>,
    `?tab=${AlmKeys.GitLab}`,
  );
}
