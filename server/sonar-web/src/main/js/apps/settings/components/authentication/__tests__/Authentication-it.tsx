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
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import React from 'react';
import AuthenticationServiceMock from '../../../../../api/mocks/AuthenticationServiceMock';
import ComputeEngineServiceMock from '../../../../../api/mocks/ComputeEngineServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { mockGitlabConfiguration } from '../../../../../helpers/mocks/alm-integrations';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../../helpers/testSelector';
import { Feature } from '../../../../../types/features';
import { GitHubProvisioningStatus, ProvisioningType } from '../../../../../types/provisioning';
import { TaskStatuses, TaskTypes } from '../../../../../types/tasks';
import Authentication from '../Authentication';

let handler: AuthenticationServiceMock;
let system: SystemServiceMock;
let settingsHandler: SettingsServiceMock;
let computeEngineHandler: ComputeEngineServiceMock;

beforeEach(() => {
  handler = new AuthenticationServiceMock();
  system = new SystemServiceMock();
  settingsHandler = new SettingsServiceMock();
  computeEngineHandler = new ComputeEngineServiceMock();
  [
    {
      key: 'sonar.auth.saml.signature.enabled',
      value: 'false',
    },
    {
      key: 'sonar.auth.saml.enabled',
      value: 'false',
    },
    {
      key: 'sonar.auth.saml.applicationId',
      value: 'sonarqube',
    },
    {
      key: 'sonar.auth.saml.providerName',
      value: 'SAML',
    },
  ].forEach((setting: any) => settingsHandler.set(setting.key, setting.value));
});

afterEach(() => {
  handler.reset();
  settingsHandler.reset();
  system.reset();
  computeEngineHandler.reset();
});

const ghContainer = byRole('tabpanel', { name: 'github GitHub' });
const glContainer = byRole('tabpanel', { name: 'gitlab GitLab' });
const samlContainer = byRole('tabpanel', { name: 'SAML' });

const ui = {
  saveButton: byRole('button', { name: 'settings.authentication.saml.form.save' }),
  customMessageInformation: byText('settings.authentication.custom_message_information'),
  enabledToggle: byRole('switch'),
  testButton: byText('settings.authentication.saml.form.test'),
  textbox1: byRole('textbox', { name: 'test1' }),
  textbox2: byRole('textbox', { name: 'test2' }),
  saml: {
    noSamlConfiguration: byText('settings.authentication.saml.form.not_configured'),
    createConfigButton: samlContainer.byRole('button', {
      name: 'settings.authentication.form.create',
    }),
    providerName: byRole('textbox', { name: 'property.sonar.auth.saml.providerName.name' }),
    providerId: byRole('textbox', { name: 'property.sonar.auth.saml.providerId.name' }),
    providerCertificate: byRole('textbox', {
      name: 'property.sonar.auth.saml.certificate.secured.name',
    }),
    loginUrl: byRole('textbox', { name: 'property.sonar.auth.saml.loginUrl.name' }),
    userLoginAttribute: byRole('textbox', { name: 'property.sonar.auth.saml.user.login.name' }),
    userNameAttribute: byRole('textbox', { name: 'property.sonar.auth.saml.user.name.name' }),
    saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
    confirmProvisioningButton: byRole('button', {
      name: 'yes',
    }),
    saveScim: samlContainer.byRole('button', { name: 'save' }),
    enableConfigButton: samlContainer.byRole('button', {
      name: 'settings.authentication.form.enable',
    }),
    disableConfigButton: samlContainer.byRole('button', {
      name: 'settings.authentication.form.disable',
    }),
    editConfigButton: samlContainer.byRole('button', {
      name: 'settings.authentication.form.edit',
    }),
    enableFirstMessage: byText('settings.authentication.saml.enable_first'),
    jitProvisioningButton: byRole('radio', {
      name: 'settings.authentication.saml.form.provisioning_at_login',
    }),
    scimProvisioningButton: byRole('radio', {
      name: 'settings.authentication.saml.form.provisioning_with_scim',
    }),
    fillForm: async (user: UserEvent) => {
      const { saml } = ui;

      await user.clear(saml.providerName.get());
      await user.type(saml.providerName.get(), 'Awsome SAML config');
      await user.type(saml.providerId.get(), 'okta-1234');
      await user.type(saml.loginUrl.get(), 'http://test.org');
      await user.type(saml.providerCertificate.get(), '-secret-');
      await user.type(saml.userLoginAttribute.get(), 'login');
      await user.type(saml.userNameAttribute.get(), 'name');
    },
    createConfiguration: async (user: UserEvent) => {
      const { saml } = ui;

      await user.click(await saml.createConfigButton.find());
      await saml.fillForm(user);
      await user.click(saml.saveConfigButton.get());
    },
  },
  github: {
    tab: byRole('tab', { name: 'github GitHub' }),
    noGithubConfiguration: byText('settings.authentication.github.form.not_configured'),
    createConfigButton: ghContainer.byRole('button', {
      name: 'settings.authentication.form.create',
    }),
    clientId: byRole('textbox', {
      name: 'property.sonar.auth.github.clientId.secured.name',
    }),
    appId: byRole('textbox', { name: 'property.sonar.auth.github.appId.name' }),
    privateKey: byRole('textbox', {
      name: 'property.sonar.auth.github.privateKey.secured.name',
    }),
    clientSecret: byRole('textbox', {
      name: 'property.sonar.auth.github.clientSecret.secured.name',
    }),
    githubApiUrl: byRole('textbox', { name: 'property.sonar.auth.github.apiUrl.name' }),
    githubWebUrl: byRole('textbox', { name: 'property.sonar.auth.github.webUrl.name' }),
    allowUserToSignUp: byRole('switch', {
      name: 'sonar.auth.github.allowUsersToSignUp',
    }),
    organizations: byRole('textbox', {
      name: 'property.sonar.auth.github.organizations.name',
    }),
    saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
    confirmProvisioningButton: byRole('button', {
      name: 'settings.authentication.github.provisioning_change.confirm_changes',
    }),
    saveGithubProvisioning: ghContainer.byRole('button', { name: 'save' }),
    groupAttribute: byRole('textbox', {
      name: 'property.sonar.auth.github.group.name.name',
    }),
    enableConfigButton: ghContainer.byRole('button', {
      name: 'settings.authentication.form.enable',
    }),
    disableConfigButton: ghContainer.byRole('button', {
      name: 'settings.authentication.form.disable',
    }),
    editConfigButton: ghContainer.byRole('button', {
      name: 'settings.authentication.form.edit',
    }),
    editMappingButton: ghContainer.byRole('button', {
      name: 'settings.authentication.github.configuration.roles_mapping.button_label',
    }),
    mappingRow: byRole('dialog', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.title',
    }).byRole('row'),
    customRoleInput: byRole('textbox', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.add_custom_role',
    }),
    customRoleAddBtn: byRole('dialog', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.title',
    }).byRole('button', { name: 'add_verb' }),
    roleExistsError: byRole('dialog', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.title',
    }).byText('settings.authentication.github.configuration.roles_mapping.role_exists'),
    emptyRoleError: byRole('dialog', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.title',
    }).byText('settings.authentication.github.configuration.roles_mapping.empty_custom_role'),
    deleteCustomRoleCustom2: byRole('button', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.delete_custom_role.custom2',
    }),
    getMappingRowByRole: (text: string) =>
      ui.github.mappingRow.getAll().find((row) => within(row).queryByText(text) !== null),
    mappingCheckbox: byRole('checkbox'),
    mappingDialogClose: byRole('dialog', {
      name: 'settings.authentication.github.configuration.roles_mapping.dialog.title',
    }).byRole('button', {
      name: 'close',
    }),
    deleteOrg: (org: string) =>
      byRole('button', {
        name: `settings.definition.delete_value.property.sonar.auth.github.organizations.name.${org}`,
      }),
    enableFirstMessage: ghContainer.byText('settings.authentication.github.enable_first'),
    jitProvisioningButton: ghContainer.byRole('radio', {
      name: 'settings.authentication.form.provisioning_at_login',
    }),
    githubProvisioningButton: ghContainer.byRole('radio', {
      name: 'settings.authentication.github.form.provisioning_with_github',
    }),
    githubProvisioningPending: ghContainer.byText(/synchronization_pending/),
    githubProvisioningInProgress: ghContainer.byText(/synchronization_in_progress/),
    githubProvisioningSuccess: ghContainer.byText(/synchronization_successful/),
    githubProvisioningAlert: ghContainer.byText(/synchronization_failed/),
    configurationValidityLoading: ghContainer.byRole('status', {
      name: /github.configuration.validation.loading/,
    }),
    configurationValiditySuccess: ghContainer.byRole('status', {
      name: /github.configuration.validation.valid/,
    }),
    configurationValidityError: ghContainer.byRole('status', {
      name: /github.configuration.validation.invalid/,
    }),
    syncWarning: ghContainer.byText(/Warning/),
    syncSummary: ghContainer.byText(/Test summary/),
    configurationValidityWarning: ghContainer.byRole('status', {
      name: /github.configuration.validation.valid.short/,
    }),
    checkConfigButton: ghContainer.byRole('button', {
      name: 'settings.authentication.github.configuration.validation.test',
    }),
    viewConfigValidityDetailsButton: ghContainer.byRole('button', {
      name: 'settings.authentication.github.configuration.validation.details',
    }),
    configDetailsDialog: byRole('dialog', {
      name: 'settings.authentication.github.configuration.validation.details.title',
    }),
    continueAutoButton: byRole('button', {
      name: 'settings.authentication.github.confirm_auto_provisioning.continue',
    }),
    switchJitButton: byRole('button', {
      name: 'settings.authentication.github.confirm_auto_provisioning.switch_jit',
    }),
    consentDialog: byRole('dialog', {
      name: 'settings.authentication.github.confirm_auto_provisioning.header',
    }),
    getConfigDetailsTitle: () => within(ui.github.configDetailsDialog.get()).getByRole('heading'),
    getOrgs: () => within(ui.github.configDetailsDialog.get()).getAllByRole('listitem'),
    fillForm: async (user: UserEvent) => {
      const { github } = ui;

      await user.type(await github.clientId.find(), 'Awsome GITHUB config');
      await user.type(github.clientSecret.get(), 'Client shut');
      await user.type(github.appId.get(), 'App id');
      await user.type(github.privateKey.get(), 'Private Key');
      await user.type(github.githubApiUrl.get(), 'API Url');
      await user.type(github.githubWebUrl.get(), 'WEb Url');
      await user.type(github.organizations.get(), 'organization1');
    },
    createConfiguration: async (user: UserEvent) => {
      const { github } = ui;

      await user.click(await github.createConfigButton.find());
      await github.fillForm(user);

      await user.click(github.saveConfigButton.get());
    },
    enableConfiguration: async (user: UserEvent) => {
      const { github } = ui;
      await user.click(await github.tab.find());
      await github.createConfiguration(user);
      await user.click(await github.enableConfigButton.find());
    },
    enableProvisioning: async (user: UserEvent) => {
      const { github } = ui;
      await user.click(await github.tab.find());

      await github.createConfiguration(user);

      await user.click(await github.enableConfigButton.find());
      await user.click(await github.githubProvisioningButton.find());
      await user.click(github.saveGithubProvisioning.get());
      await user.click(github.confirmProvisioningButton.get());
    },
  },
  gitlab: {
    tab: byRole('tab', { name: 'gitlab GitLab' }),
    noGitlabConfiguration: glContainer.byText('settings.authentication.gitlab.form.not_configured'),
    createConfigButton: glContainer.byRole('button', {
      name: 'settings.authentication.form.create',
    }),
    editConfigButton: glContainer.byRole('button', {
      name: 'settings.authentication.form.edit',
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
    clientSecret: byRole('textbox', {
      name: 'property.clientSecret.name',
    }),
    synchronizeUserGroups: byRole('switch', {
      name: 'synchronizeUserGroups',
    }),
    saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
    jitProvisioningRadioButton: glContainer.byRole('radio', {
      name: 'settings.authentication.gitlab.provisioning_at_login',
    }),
    autoProvisioningRadioButton: glContainer.byRole('radio', {
      name: 'settings.authentication.gitlab.form.provisioning_with_gitlab',
    }),
    jitAllowUsersToSignUpToggle: byRole('switch', { name: 'sonar.auth.gitlab.allowUsersToSignUp' }),
    autoProvisioningToken: byRole('textbox', {
      name: 'property.provisioning.gitlab.token.secured.name',
    }),
    autoProvisioningUpdateTokenButton: byRole('button', {
      name: 'settings.almintegration.form.secret.update_field',
    }),
    autoProvisioningGroupsInput: byRole('textbox', {
      name: 'property.provisioning.gitlab.groups.name',
    }),
    removeProvisioniongGroup: byRole('button', {
      name: /settings.definition.delete_value.property.provisioning.gitlab.groups.name./,
    }),
    saveProvisioning: glContainer.byRole('button', { name: 'save' }),
    cancelProvisioningChanges: glContainer.byRole('button', { name: 'cancel' }),
    confirmAutoProvisioningDialog: byRole('dialog', {
      name: 'settings.authentication.gitlab.confirm.Auto',
    }),
    confirmJitProvisioningDialog: byRole('dialog', {
      name: 'settings.authentication.gitlab.confirm.JIT',
    }),
    confirmProvisioningChange: byRole('button', {
      name: 'settings.authentication.gitlab.provisioning_change.confirm_changes',
    }),
    syncSummary: glContainer.byText(/Test summary/),
    syncWarning: glContainer.byText(/Warning/),
    gitlabProvisioningPending: glContainer.byText(/synchronization_pending/),
    gitlabProvisioningInProgress: glContainer.byText(/synchronization_in_progress/),
    gitlabProvisioningSuccess: glContainer.byText(/synchronization_successful/),
    gitlabProvisioningAlert: glContainer.byText(/synchronization_failed/),
  },
};

it('should render tabs and allow navigation', async () => {
  const user = userEvent.setup();
  renderAuthentication();

  expect(screen.getAllByRole('tab')).toHaveLength(4);

  expect(screen.getByRole('tab', { name: 'SAML' })).toHaveAttribute('aria-selected', 'true');

  await user.click(screen.getByRole('tab', { name: 'github GitHub' }));

  expect(screen.getByRole('tab', { name: 'SAML' })).toHaveAttribute('aria-selected', 'false');
  expect(screen.getByRole('tab', { name: 'github GitHub' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
});

it('should not display the login message feature info box', () => {
  renderAuthentication();

  expect(ui.customMessageInformation.query()).not.toBeInTheDocument();
});

it('should display the login message feature info box', () => {
  renderAuthentication([Feature.LoginMessage]);

  expect(ui.customMessageInformation.get()).toBeInTheDocument();
});

describe('SAML tab', () => {
  const { saml } = ui;

  it('should render an empty SAML configuration', async () => {
    renderAuthentication();
    expect(await saml.noSamlConfiguration.find()).toBeInTheDocument();
  });

  it('should be able to create a configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();

    await user.click(await saml.createConfigButton.find());

    expect(saml.saveConfigButton.get()).toBeDisabled();
    await saml.fillForm(user);
    expect(saml.saveConfigButton.get()).toBeEnabled();

    await user.click(saml.saveConfigButton.get());

    expect(await saml.editConfigButton.find()).toBeInTheDocument();
  });

  it('should be able to enable/disable configuration', async () => {
    const { saml } = ui;
    const user = userEvent.setup();
    renderAuthentication();

    await saml.createConfiguration(user);
    await user.click(await saml.enableConfigButton.find());

    expect(await saml.disableConfigButton.find()).toBeInTheDocument();
    await user.click(saml.disableConfigButton.get());
    await waitFor(() => expect(saml.disableConfigButton.query()).not.toBeInTheDocument());

    expect(await saml.enableConfigButton.find()).toBeInTheDocument();
  });

  it('should be able to choose provisioning', async () => {
    const { saml } = ui;
    const user = userEvent.setup();

    renderAuthentication([Feature.Scim]);

    await saml.createConfiguration(user);

    expect(await saml.enableFirstMessage.find()).toBeInTheDocument();
    await user.click(await saml.enableConfigButton.find());

    expect(await saml.jitProvisioningButton.find()).toBeChecked();
    expect(saml.saveScim.get()).toBeDisabled();

    await user.click(saml.scimProvisioningButton.get());
    expect(saml.saveScim.get()).toBeEnabled();
    await user.click(saml.saveScim.get());
    await user.click(saml.confirmProvisioningButton.get());

    expect(await saml.scimProvisioningButton.find()).toBeChecked();
    expect(await saml.saveScim.find()).toBeDisabled();
  });

  it('should not allow editions below Enterprise to select SCIM provisioning', async () => {
    const { saml } = ui;
    const user = userEvent.setup();

    renderAuthentication();

    await saml.createConfiguration(user);
    await user.click(await saml.enableConfigButton.find());

    expect(await saml.jitProvisioningButton.find()).toBeChecked();
    expect(saml.scimProvisioningButton.get()).toHaveAttribute('aria-disabled', 'true');
  });
});

describe('Github tab', () => {
  const { github } = ui;

  it('should render an empty Github configuration', async () => {
    renderAuthentication();
    const user = userEvent.setup();
    await user.click(await github.tab.find());
    expect(await github.noGithubConfiguration.find()).toBeInTheDocument();
  });

  it('should be able to create a configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();

    await user.click(await github.tab.find());
    await user.click(await github.createConfigButton.find());

    expect(github.saveConfigButton.get()).toBeDisabled();

    await github.fillForm(user);
    expect(github.saveConfigButton.get()).toBeEnabled();

    await user.click(github.saveConfigButton.get());

    expect(await github.editConfigButton.find()).toBeInTheDocument();
  });

  it('should be able to edit configuration', async () => {
    const { github } = ui;
    const user = userEvent.setup();
    renderAuthentication();
    await user.click(await github.tab.find());

    await github.createConfiguration(user);

    await user.click(github.editConfigButton.get());
    await user.click(github.deleteOrg('organization1').get());

    await user.click(github.saveConfigButton.get());

    await user.click(await github.editConfigButton.find());

    expect(github.organizations.get()).toHaveValue('');
  });

  it('should be able to enable/disable configuration', async () => {
    const { github } = ui;
    const user = userEvent.setup();
    renderAuthentication();
    await user.click(await github.tab.find());

    await github.createConfiguration(user);

    await user.click(await github.enableConfigButton.find());

    expect(await github.disableConfigButton.find()).toBeInTheDocument();
    await user.click(github.disableConfigButton.get());
    await waitFor(() => expect(github.disableConfigButton.query()).not.toBeInTheDocument());

    expect(await github.enableConfigButton.find()).toBeInTheDocument();
  });

  it('should not allow edtion below Enterprise to select Github provisioning', async () => {
    const { github } = ui;
    const user = userEvent.setup();

    renderAuthentication();
    await user.click(await github.tab.find());

    await github.createConfiguration(user);
    await user.click(await github.enableConfigButton.find());

    expect(await github.jitProvisioningButton.find()).toBeChecked();
    expect(github.githubProvisioningButton.get()).toHaveAttribute('aria-disabled', 'true');
  });

  it('should be able to choose provisioning', async () => {
    const { github } = ui;
    const user = userEvent.setup();

    renderAuthentication([Feature.GithubProvisioning]);
    await user.click(await github.tab.find());

    await github.createConfiguration(user);

    expect(await github.enableFirstMessage.find()).toBeInTheDocument();
    await user.click(await github.enableConfigButton.find());

    expect(await github.jitProvisioningButton.find()).toBeChecked();

    expect(github.saveGithubProvisioning.get()).toBeDisabled();
    await user.click(github.allowUserToSignUp.get());

    expect(github.saveGithubProvisioning.get()).toBeEnabled();
    await user.click(github.saveGithubProvisioning.get());

    await waitFor(() => expect(github.saveGithubProvisioning.query()).toBeDisabled());

    await user.click(github.githubProvisioningButton.get());

    expect(github.saveGithubProvisioning.get()).toBeEnabled();
    await user.click(github.saveGithubProvisioning.get());
    await user.click(github.confirmProvisioningButton.get());

    expect(await github.githubProvisioningButton.find()).toBeChecked();
    expect(github.disableConfigButton.get()).toBeDisabled();
    expect(github.saveGithubProvisioning.get()).toBeDisabled();
  });

  describe('Github Provisioning', () => {
    let user: UserEvent;

    beforeEach(() => {
      jest.useFakeTimers({
        advanceTimers: true,
        now: new Date('2022-02-04T12:00:59Z'),
      });
      user = userEvent.setup();
    });

    afterEach(() => {
      jest.runOnlyPendingTimers();
      jest.useRealTimers();
    });

    it('should display a success status when the synchronisation is a success', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Success,
        executedAt: '2022-02-03T11:45:35+0200',
      });

      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableProvisioning(user);
      expect(github.githubProvisioningSuccess.get()).toBeInTheDocument();
      expect(github.syncSummary.get()).toBeInTheDocument();
    });

    it('should display a success status even when another task is pending', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Pending,
        executedAt: '2022-02-03T11:55:35+0200',
      });
      handler.addProvisioningTask({
        status: TaskStatuses.Success,
        executedAt: '2022-02-03T11:45:35+0200',
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableProvisioning(user);
      expect(github.githubProvisioningSuccess.get()).toBeInTheDocument();
      expect(github.githubProvisioningPending.get()).toBeInTheDocument();
    });

    it('should display an error alert when the synchronisation failed', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: "T'es mauvais Jacques",
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableProvisioning(user);
      expect(github.githubProvisioningAlert.get()).toBeInTheDocument();
      expect(github.githubProvisioningButton.get()).toHaveTextContent("T'es mauvais Jacques");
      expect(github.githubProvisioningSuccess.query()).not.toBeInTheDocument();
    });

    it('should display an error alert even when another task is in progress', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.InProgress,
        executedAt: '2022-02-03T11:55:35+0200',
      });
      handler.addProvisioningTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: "T'es mauvais Jacques",
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableProvisioning(user);
      expect(github.githubProvisioningAlert.get()).toBeInTheDocument();
      expect(github.githubProvisioningButton.get()).toHaveTextContent("T'es mauvais Jacques");
      expect(github.githubProvisioningSuccess.query()).not.toBeInTheDocument();
      expect(github.githubProvisioningInProgress.get()).toBeInTheDocument();
    });

    it('should display that config is valid for both provisioning with 1 org', async () => {
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());
    });

    it('should display that config is valid for both provisioning with multiple orgs', async () => {
      handler.setConfigurationValidity({
        installations: [
          {
            organization: 'org1',
            autoProvisioning: { status: GitHubProvisioningStatus.Success },
            jit: { status: GitHubProvisioningStatus.Success },
          },
          {
            organization: 'org2',
            autoProvisioning: { status: GitHubProvisioningStatus.Success },
            jit: { status: GitHubProvisioningStatus.Success },
          },
        ],
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());
      expect(github.configurationValiditySuccess.get()).toHaveTextContent('2');

      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label',
      );
      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg1',
      );
      expect(github.getOrgs()[1]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg2',
      );
    });

    it('should display that config is invalid for autoprovisioning if some apps are suspended but valid for jit', async () => {
      const errorMessage = 'Installation suspended';
      handler.setConfigurationValidity({
        installations: [
          {
            organization: 'org1',
            autoProvisioning: {
              status: GitHubProvisioningStatus.Failed,
              errorMessage,
            },
            jit: {
              status: GitHubProvisioningStatus.Failed,
              errorMessage,
            },
          },
        ],
      });

      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValidityWarning.get()).toBeInTheDocument());
      expect(github.configurationValidityWarning.get()).toHaveTextContent(errorMessage);

      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label',
      );
      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_labelorg1 - Installation suspended',
      );

      await user.click(github.configDetailsDialog.byRole('button', { name: 'close' }).get());

      await user.click(github.githubProvisioningButton.get());
      await waitFor(() => expect(github.configurationValidityError.get()).toBeInTheDocument());
      expect(github.configurationValidityError.get()).toHaveTextContent(errorMessage);
    });

    it('should display that config is valid but some organizations were not found', async () => {
      handler.setConfigurationValidity({
        installations: [
          {
            organization: 'org1',
            autoProvisioning: { status: GitHubProvisioningStatus.Success },
            jit: { status: GitHubProvisioningStatus.Success },
          },
        ],
      });

      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValiditySuccess.get()).toBeInTheDocument());
      expect(github.configurationValiditySuccess.get()).toHaveTextContent('1');

      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label',
      );
      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg1',
      );
      expect(github.getOrgs()[1]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.org_not_found.organization1',
      );
    });

    it('should display that config is invalid', async () => {
      const errorMessage = 'Test error';
      handler.setConfigurationValidity({
        application: {
          jit: {
            status: GitHubProvisioningStatus.Failed,
            errorMessage,
          },
          autoProvisioning: {
            status: GitHubProvisioningStatus.Failed,
            errorMessage,
          },
        },
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValidityError.query()).toBeInTheDocument());
      expect(github.configurationValidityError.get()).toHaveTextContent(errorMessage);

      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_label',
      );
      expect(github.configDetailsDialog.get()).toHaveTextContent(errorMessage);
    });

    it('should display that config is valid for jit, but not for auto', async () => {
      const errorMessage = 'Test error';
      handler.setConfigurationValidity({
        application: {
          jit: {
            status: GitHubProvisioningStatus.Success,
          },
          autoProvisioning: {
            status: GitHubProvisioningStatus.Failed,
            errorMessage,
          },
        },
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());
      expect(github.configurationValiditySuccess.get()).not.toHaveTextContent(errorMessage);

      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label',
      );
      await user.click(github.configDetailsDialog.byRole('button', { name: 'close' }).get());

      await user.click(github.githubProvisioningButton.get());

      expect(github.configurationValidityError.get()).toBeInTheDocument();
      expect(github.configurationValidityError.get()).toHaveTextContent(errorMessage);

      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_label',
      );
    });

    it('should display that config is invalid because of orgs', async () => {
      const errorMessage = 'Test error';
      handler.setConfigurationValidity({
        installations: [
          {
            organization: 'org1',
            autoProvisioning: { status: GitHubProvisioningStatus.Success },
            jit: { status: GitHubProvisioningStatus.Success },
          },
          {
            organization: 'org2',
            jit: { status: GitHubProvisioningStatus.Failed, errorMessage },
            autoProvisioning: { status: GitHubProvisioningStatus.Failed, errorMessage },
          },
        ],
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);

      await appLoaded();

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());

      await user.click(github.viewConfigValidityDetailsButton.get());

      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg1',
      );
      expect(github.getOrgs()[1]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_labelorg2 - Test error',
      );

      await user.click(github.configDetailsDialog.byRole('button', { name: 'close' }).get());

      await user.click(github.githubProvisioningButton.get());

      expect(github.configurationValidityError.get()).toBeInTheDocument();
      expect(github.configurationValidityError.get()).toHaveTextContent(
        `settings.authentication.github.configuration.validation.invalid_org.org2.${errorMessage}`,
      );
      await user.click(github.viewConfigValidityDetailsButton.get());
      expect(github.getOrgs()[1]).toHaveTextContent(
        `settings.authentication.github.configuration.validation.details.invalid_labelorg2 - ${errorMessage}`,
      );
    });

    it('should update provisioning validity after clicking Test Configuration', async () => {
      const errorMessage = 'Test error';
      handler.setConfigurationValidity({
        application: {
          jit: {
            status: GitHubProvisioningStatus.Failed,
            errorMessage,
          },
          autoProvisioning: {
            status: GitHubProvisioningStatus.Failed,
            errorMessage,
          },
        },
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableConfiguration(user);
      handler.setConfigurationValidity({
        application: {
          jit: {
            status: GitHubProvisioningStatus.Success,
          },
          autoProvisioning: {
            status: GitHubProvisioningStatus.Success,
          },
        },
      });

      await appLoaded();

      expect(await github.configurationValidityError.find()).toBeInTheDocument();

      await user.click(github.checkConfigButton.get());

      expect(github.configurationValiditySuccess.get()).toBeInTheDocument();
      expect(github.configurationValidityError.query()).not.toBeInTheDocument();
    });

    it('should show warning', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Success,
        warnings: ['Warning'],
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await github.enableProvisioning(user);

      expect(await github.syncWarning.find()).toBeInTheDocument();
      expect(github.syncSummary.get()).toBeInTheDocument();
    });

    it('should display a modal if user was already using auto and continue using auto provisioning', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      settingsHandler.set('sonar.auth.github.userConsentForPermissionProvisioningRequired', '');
      renderAuthentication([Feature.GithubProvisioning]);

      await user.click(await github.tab.find());

      expect(await github.consentDialog.find()).toBeInTheDocument();
      await user.click(github.continueAutoButton.get());

      expect(await github.githubProvisioningButton.find()).toBeChecked();
      expect(github.consentDialog.query()).not.toBeInTheDocument();
    });

    it('should display a modal if user was already using auto and switch to JIT', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      settingsHandler.set('sonar.auth.github.userConsentForPermissionProvisioningRequired', '');
      renderAuthentication([Feature.GithubProvisioning]);

      await user.click(await github.tab.find());

      expect(await github.consentDialog.find()).toBeInTheDocument();
      await user.click(github.switchJitButton.get());

      expect(await github.jitProvisioningButton.find()).toBeChecked();
      expect(github.consentDialog.query()).not.toBeInTheDocument();
    });

    it('should sort mapping rows', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await github.tab.find());

      expect(await github.editMappingButton.find()).toBeInTheDocument();
      await user.click(github.editMappingButton.get());

      const rows = (await github.mappingRow.findAll()).filter(
        (row) => within(row).queryAllByRole('checkbox').length > 0,
      );

      expect(rows).toHaveLength(5);

      expect(rows[0]).toHaveTextContent('read');
      expect(rows[1]).toHaveTextContent('triage');
      expect(rows[2]).toHaveTextContent('write');
      expect(rows[3]).toHaveTextContent('maintain');
      expect(rows[4]).toHaveTextContent('admin');
    });

    it('should apply new mapping and new provisioning type at the same time', async () => {
      const user = userEvent.setup();
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await github.tab.find());

      await github.createConfiguration(user);
      await user.click(await github.enableConfigButton.find());

      expect(await github.jitProvisioningButton.find()).toBeChecked();
      expect(github.editMappingButton.query()).not.toBeInTheDocument();
      await user.click(github.githubProvisioningButton.get());
      expect(await github.editMappingButton.find()).toBeInTheDocument();
      await user.click(github.editMappingButton.get());

      expect(await github.mappingRow.findAll()).toHaveLength(7);

      let readCheckboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('read'));
      let adminCheckboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('admin'));

      expect(readCheckboxes[0]).toBeChecked();
      expect(readCheckboxes[5]).not.toBeChecked();
      expect(adminCheckboxes[5]).toBeChecked();

      await user.click(readCheckboxes[0]);
      await user.click(readCheckboxes[5]);
      await user.click(adminCheckboxes[5]);
      await user.click(github.mappingDialogClose.get());

      await user.click(github.saveGithubProvisioning.get());
      await user.click(github.confirmProvisioningButton.get());

      // Clean local mapping state
      await user.click(github.jitProvisioningButton.get());
      await user.click(github.githubProvisioningButton.get());

      await user.click(github.editMappingButton.get());
      readCheckboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('read'));
      adminCheckboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('admin'));

      expect(readCheckboxes[0]).not.toBeChecked();
      expect(readCheckboxes[5]).toBeChecked();
      expect(adminCheckboxes[5]).not.toBeChecked();
      await user.click(github.mappingDialogClose.get());
    });

    it('should apply new mapping on auto-provisioning', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await github.tab.find());

      expect(await github.saveGithubProvisioning.find()).toBeDisabled();
      await user.click(github.editMappingButton.get());

      expect(await github.mappingRow.findAll()).toHaveLength(7);

      let readCheckboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('read'))[0];

      expect(readCheckboxes).toBeChecked();

      await user.click(readCheckboxes);
      await user.click(github.mappingDialogClose.get());

      expect(await github.saveGithubProvisioning.find()).toBeEnabled();

      await user.click(github.saveGithubProvisioning.get());

      // Clean local mapping state
      await user.click(github.jitProvisioningButton.get());
      await user.click(github.githubProvisioningButton.get());

      await user.click(github.editMappingButton.get());
      readCheckboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('read'))[0];

      expect(readCheckboxes).not.toBeChecked();
      await user.click(github.mappingDialogClose.get());
    });

    it('should add/remove/update custom roles', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      handler.addGitHubCustomRole('custom1', ['user', 'codeViewer', 'scan']);
      handler.addGitHubCustomRole('custom2', ['user', 'codeViewer', 'issueAdmin', 'scan']);
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await github.tab.find());

      expect(await github.saveGithubProvisioning.find()).toBeDisabled();
      await user.click(github.editMappingButton.get());

      const rows = (await github.mappingRow.findAll()).filter(
        (row) => within(row).queryAllByRole('checkbox').length > 0,
      );

      expect(rows).toHaveLength(7);

      let custom1Checkboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('custom1'));

      expect(custom1Checkboxes[0]).toBeChecked();
      expect(custom1Checkboxes[1]).toBeChecked();
      expect(custom1Checkboxes[2]).not.toBeChecked();
      expect(custom1Checkboxes[3]).not.toBeChecked();
      expect(custom1Checkboxes[4]).not.toBeChecked();
      expect(custom1Checkboxes[5]).toBeChecked();

      await user.click(custom1Checkboxes[1]);
      await user.click(custom1Checkboxes[2]);

      await user.click(github.deleteCustomRoleCustom2.get());

      expect(github.customRoleInput.get()).toHaveValue('');
      await user.type(github.customRoleInput.get(), 'read');
      await user.click(github.customRoleAddBtn.get());
      expect(await github.roleExistsError.find()).toBeInTheDocument();
      expect(github.customRoleAddBtn.get()).toBeDisabled();
      await user.clear(github.customRoleInput.get());
      expect(github.roleExistsError.query()).not.toBeInTheDocument();
      await user.type(github.customRoleInput.get(), 'custom1');
      await user.click(github.customRoleAddBtn.get());
      expect(await github.roleExistsError.find()).toBeInTheDocument();
      expect(github.customRoleAddBtn.get()).toBeDisabled();
      await user.clear(github.customRoleInput.get());
      await user.type(github.customRoleInput.get(), 'custom3');
      expect(github.roleExistsError.query()).not.toBeInTheDocument();
      expect(github.customRoleAddBtn.get()).toBeEnabled();
      await user.click(github.customRoleAddBtn.get());

      let custom3Checkboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('custom3'));
      expect(custom3Checkboxes[0]).toBeChecked();
      expect(custom3Checkboxes[1]).not.toBeChecked();
      expect(custom3Checkboxes[2]).not.toBeChecked();
      expect(custom3Checkboxes[3]).not.toBeChecked();
      expect(custom3Checkboxes[4]).not.toBeChecked();
      expect(custom3Checkboxes[5]).not.toBeChecked();
      await user.click(custom3Checkboxes[0]);
      expect(await github.emptyRoleError.find()).toBeInTheDocument();
      expect(github.mappingDialogClose.get()).toBeDisabled();
      await user.click(custom3Checkboxes[1]);
      expect(github.emptyRoleError.query()).not.toBeInTheDocument();
      expect(github.mappingDialogClose.get()).toBeEnabled();
      await user.click(github.mappingDialogClose.get());

      expect(await github.saveGithubProvisioning.find()).toBeEnabled();
      await user.click(github.saveGithubProvisioning.get());

      // Clean local mapping state
      await user.click(github.jitProvisioningButton.get());
      await user.click(github.githubProvisioningButton.get());

      await user.click(github.editMappingButton.get());

      expect(
        (await github.mappingRow.findAll()).filter(
          (row) => within(row).queryAllByRole('checkbox').length > 0,
        ),
      ).toHaveLength(7);
      custom1Checkboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('custom1'));
      custom3Checkboxes = github.mappingCheckbox.getAll(github.getMappingRowByRole('custom3'));
      expect(github.getMappingRowByRole('custom2')).toBeUndefined();
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
      await user.click(github.mappingDialogClose.get());
    });
  });
});

describe('GitLab', () => {
  const { gitlab } = ui;

  it('should create a Gitlab configuration and disable it', async () => {
    handler.setGitlabConfigurations([]);
    renderAuthentication();
    const user = userEvent.setup();
    await user.click(await gitlab.tab.find());

    expect(await gitlab.noGitlabConfiguration.find()).toBeInTheDocument();
    expect(gitlab.createConfigButton.get()).toBeInTheDocument();

    await user.click(gitlab.createConfigButton.get());
    expect(await gitlab.createDialog.find()).toBeInTheDocument();
    await user.type(gitlab.applicationId.get(), '123');
    await user.type(gitlab.url.get(), 'https://company.gitlab.com');
    await user.type(gitlab.clientSecret.get(), '123');
    await user.click(gitlab.synchronizeUserGroups.get());
    await user.click(gitlab.saveConfigButton.get());

    expect(await gitlab.editConfigButton.find()).toBeInTheDocument();
    expect(gitlab.noGitlabConfiguration.query()).not.toBeInTheDocument();
    expect(glContainer.get()).toHaveTextContent('https://company.gitlab.com');

    expect(gitlab.disableConfigButton.get()).toBeInTheDocument();
    await user.click(gitlab.disableConfigButton.get());
    expect(gitlab.enableConfigButton.get()).toBeInTheDocument();
    expect(gitlab.disableConfigButton.query()).not.toBeInTheDocument();
  });

  it('should edit/delete configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();
    await user.click(await gitlab.tab.find());

    expect(await gitlab.editConfigButton.find()).toBeInTheDocument();
    expect(glContainer.get()).toHaveTextContent('URL');
    expect(gitlab.disableConfigButton.get()).toBeInTheDocument();
    expect(gitlab.deleteConfigButton.get()).toBeInTheDocument();
    expect(gitlab.deleteConfigButton.get()).toBeDisabled();

    await user.click(gitlab.editConfigButton.get());
    expect(await gitlab.editDialog.find()).toBeInTheDocument();
    expect(gitlab.url.get()).toHaveValue('URL');
    expect(gitlab.applicationId.query()).not.toBeInTheDocument();
    expect(gitlab.clientSecret.query()).not.toBeInTheDocument();
    expect(gitlab.synchronizeUserGroups.get()).toBeChecked();
    await user.clear(gitlab.url.get());
    await user.type(gitlab.url.get(), 'https://company.gitlab.com');
    await user.click(gitlab.saveConfigButton.get());

    expect(glContainer.get()).not.toHaveTextContent('URL');
    expect(glContainer.get()).toHaveTextContent('https://company.gitlab.com');

    expect(gitlab.disableConfigButton.get()).toBeInTheDocument();
    await user.click(gitlab.disableConfigButton.get());
    expect(await gitlab.enableConfigButton.find()).toBeInTheDocument();
    expect(gitlab.deleteConfigButton.get()).toBeEnabled();
    await user.click(gitlab.deleteConfigButton.get());
    expect(await gitlab.noGitlabConfiguration.find()).toBeInTheDocument();
    expect(gitlab.editConfigButton.query()).not.toBeInTheDocument();
  });

  it('should change from just-in-time to Auto Provisioning with proper validation', async () => {
    const user = userEvent.setup();
    renderAuthentication([Feature.GitlabProvisioning]);
    await user.click(await gitlab.tab.find());

    expect(await gitlab.editConfigButton.find()).toBeInTheDocument();
    expect(gitlab.jitProvisioningRadioButton.get()).toBeChecked();

    user.click(gitlab.autoProvisioningRadioButton.get());
    expect(await gitlab.autoProvisioningRadioButton.find()).toBeEnabled();
    expect(gitlab.saveProvisioning.get()).toBeDisabled();

    await user.type(gitlab.autoProvisioningToken.get(), 'JRR Tolkien');
    expect(await gitlab.saveProvisioning.find()).toBeDisabled();

    await user.type(gitlab.autoProvisioningGroupsInput.get(), 'NWA');
    user.click(gitlab.autoProvisioningRadioButton.get());
    expect(await gitlab.saveProvisioning.find()).toBeEnabled();

    await user.click(gitlab.removeProvisioniongGroup.get());
    expect(await gitlab.saveProvisioning.find()).toBeDisabled();
    await user.type(gitlab.autoProvisioningGroupsInput.get(), 'Wu-Tang Clan');
    expect(await gitlab.saveProvisioning.find()).toBeEnabled();

    await user.clear(gitlab.autoProvisioningToken.get());
    expect(await gitlab.saveProvisioning.find()).toBeDisabled();
    await user.type(gitlab.autoProvisioningToken.get(), 'tiktoken');
    expect(await gitlab.saveProvisioning.find()).toBeEnabled();

    await user.click(gitlab.saveProvisioning.get());
    expect(gitlab.confirmAutoProvisioningDialog.get()).toBeInTheDocument();
    await user.click(gitlab.confirmProvisioningChange.get());
    expect(gitlab.confirmAutoProvisioningDialog.query()).not.toBeInTheDocument();

    expect(gitlab.autoProvisioningRadioButton.get()).toBeChecked();
    expect(await gitlab.saveProvisioning.find()).toBeDisabled();
  });

  it('should change from auto provisioning to JIT with proper validation', async () => {
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        type: ProvisioningType.auto,
        groups: ['D12'],
      }),
    ]);
    const user = userEvent.setup();
    renderAuthentication([Feature.GitlabProvisioning]);
    await user.click(await gitlab.tab.find());

    expect(await gitlab.editConfigButton.find()).toBeInTheDocument();

    expect(gitlab.jitProvisioningRadioButton.get()).not.toBeChecked();
    expect(gitlab.autoProvisioningRadioButton.get()).toBeChecked();
    expect(gitlab.autoProvisioningGroupsInput.get()).toHaveValue('D12');

    expect(gitlab.autoProvisioningToken.query()).not.toBeInTheDocument();
    expect(gitlab.autoProvisioningUpdateTokenButton.get()).toBeInTheDocument();

    await user.click(gitlab.jitProvisioningRadioButton.get());
    expect(await gitlab.jitProvisioningRadioButton.find()).toBeChecked();

    expect(await gitlab.saveProvisioning.find()).toBeEnabled();

    expect(gitlab.jitAllowUsersToSignUpToggle.get()).toBeInTheDocument();

    await user.click(gitlab.saveProvisioning.get());
    expect(gitlab.confirmJitProvisioningDialog.get()).toBeInTheDocument();
    await user.click(gitlab.confirmProvisioningChange.get());
    expect(gitlab.confirmJitProvisioningDialog.query()).not.toBeInTheDocument();

    expect(gitlab.jitProvisioningRadioButton.get()).toBeChecked();
    expect(await gitlab.saveProvisioning.find()).toBeDisabled();
  });

  it('should be able to allow user to sign up for JIT with proper validation', async () => {
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        type: ProvisioningType.jit,
      }),
    ]);
    const user = userEvent.setup();
    renderAuthentication([Feature.GitlabProvisioning]);
    await user.click(await gitlab.tab.find());

    expect(await gitlab.editConfigButton.find()).toBeInTheDocument();

    expect(gitlab.jitProvisioningRadioButton.get()).toBeChecked();
    expect(gitlab.autoProvisioningRadioButton.get()).not.toBeChecked();

    expect(gitlab.jitAllowUsersToSignUpToggle.get()).not.toBeChecked();

    expect(gitlab.saveProvisioning.get()).toBeDisabled();
    await user.click(gitlab.jitAllowUsersToSignUpToggle.get());
    expect(gitlab.saveProvisioning.get()).toBeEnabled();
    await user.click(gitlab.jitAllowUsersToSignUpToggle.get());
    expect(gitlab.saveProvisioning.get()).toBeDisabled();
    await user.click(gitlab.jitAllowUsersToSignUpToggle.get());

    await user.click(gitlab.saveProvisioning.get());

    expect(gitlab.jitProvisioningRadioButton.get()).toBeChecked();
    expect(gitlab.jitAllowUsersToSignUpToggle.get()).toBeChecked();
    expect(await gitlab.saveProvisioning.find()).toBeDisabled();
  });

  it('should be able to edit groups and token for Auto provisioning with proper validation', async () => {
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        type: ProvisioningType.auto,
        groups: ['Cypress Hill', 'Public Enemy'],
      }),
    ]);
    const user = userEvent.setup();
    renderAuthentication([Feature.GitlabProvisioning]);
    await user.click(await gitlab.tab.find());

    expect(gitlab.autoProvisioningRadioButton.get()).toBeChecked();
    expect(gitlab.autoProvisioningUpdateTokenButton.get()).toBeInTheDocument();
    expect(gitlab.autoProvisioningGroupsInput.get()).toHaveValue('Cypress Hill');

    expect(gitlab.saveProvisioning.get()).toBeDisabled();

    // Changing the Provisioning token should enable save
    await user.click(gitlab.autoProvisioningUpdateTokenButton.get());
    await user.type(gitlab.autoProvisioningGroupsInput.get(), 'Tok Token!');
    expect(gitlab.saveProvisioning.get()).toBeEnabled();
    await user.click(gitlab.cancelProvisioningChanges.get());
    expect(gitlab.saveProvisioning.get()).toBeDisabled();

    // Adding a group should enable save
    await user.click(gitlab.autoProvisioningGroupsInput.get());
    await user.tab();
    await user.tab();
    await user.tab();
    await user.tab();
    await user.keyboard('Run DMC');
    expect(gitlab.saveProvisioning.get()).toBeEnabled();
    await user.tab();
    await user.keyboard('{Enter}');
    expect(gitlab.saveProvisioning.get()).toBeDisabled();

    // Removing a group should enable save
    await user.click(gitlab.autoProvisioningGroupsInput.get());
    await user.tab();
    await user.keyboard('{Enter}');
    expect(gitlab.saveProvisioning.get()).toBeEnabled();

    // Removing all groups should disable save
    await user.click(gitlab.autoProvisioningGroupsInput.get());
    await user.tab();
    await user.keyboard('{Enter}');
    expect(gitlab.saveProvisioning.get()).toBeDisabled();
  });

  it('should be able to reset Auto Provisioning changes', async () => {
    handler.setGitlabConfigurations([
      mockGitlabConfiguration({
        allowUsersToSignUp: false,
        enabled: true,
        type: ProvisioningType.auto,
        groups: ['Cypress Hill', 'Public Enemy'],
      }),
    ]);
    const user = userEvent.setup();
    renderAuthentication([Feature.GitlabProvisioning]);
    await user.click(await gitlab.tab.find());

    expect(gitlab.autoProvisioningRadioButton.get()).toBeChecked();

    // Cancel doesn't fully work yet as the AuthenticationFormField needs to be worked on
    await user.click(gitlab.autoProvisioningGroupsInput.get());
    await user.tab();
    await user.tab();
    await user.tab();
    await user.tab();
    await user.keyboard('A Tribe Called Quest');
    await user.click(gitlab.autoProvisioningGroupsInput.get());
    await user.tab();
    await user.keyboard('{Enter}');
    await user.click(gitlab.autoProvisioningUpdateTokenButton.get());
    await user.type(gitlab.autoProvisioningGroupsInput.get(), 'ToToken!');
    expect(gitlab.saveProvisioning.get()).toBeEnabled();
    await user.click(gitlab.cancelProvisioningChanges.get());
    // expect(gitlab.autoProvisioningUpdateTokenButton.get()).toBeInTheDocument();
    expect(gitlab.autoProvisioningGroupsInput.get()).toHaveValue('Cypress Hill');
  });

  describe('Gitlab Provisioning', () => {
    beforeEach(() => {
      jest.useFakeTimers({
        advanceTimers: true,
        now: new Date('2022-02-04T12:00:59Z'),
      });
      handler.setGitlabConfigurations([
        mockGitlabConfiguration({
          id: '1',
          enabled: true,
          type: ProvisioningType.auto,
          groups: ['Test'],
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
      expect(await gitlab.gitlabProvisioningSuccess.find()).toBeInTheDocument();
      expect(gitlab.syncSummary.get()).toBeInTheDocument();
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
      expect(await gitlab.gitlabProvisioningSuccess.find()).toBeInTheDocument();
      expect(gitlab.gitlabProvisioningPending.get()).toBeInTheDocument();
    });

    it('should display an error alert when the synchronisation failed', async () => {
      computeEngineHandler.addTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: "T'es mauvais Jacques",
        type: TaskTypes.GitlabProvisioning,
      });
      renderAuthentication([Feature.GitlabProvisioning]);
      expect(await gitlab.gitlabProvisioningAlert.find()).toBeInTheDocument();
      expect(gitlab.autoProvisioningRadioButton.get()).toHaveTextContent("T'es mauvais Jacques");
      expect(gitlab.gitlabProvisioningSuccess.query()).not.toBeInTheDocument();
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
      expect(await gitlab.gitlabProvisioningAlert.find()).toBeInTheDocument();
      expect(gitlab.autoProvisioningRadioButton.get()).toHaveTextContent("T'es mauvais Jacques");
      expect(gitlab.gitlabProvisioningSuccess.query()).not.toBeInTheDocument();
      expect(gitlab.gitlabProvisioningInProgress.get()).toBeInTheDocument();
    });

    it('should show warning', async () => {
      computeEngineHandler.addTask({
        status: TaskStatuses.Success,
        warnings: ['Warning'],
        infoMessages: ['Test summary'],
        type: TaskTypes.GitlabProvisioning,
      });
      renderAuthentication([Feature.GitlabProvisioning]);

      expect(await gitlab.syncWarning.find()).toBeInTheDocument();
      expect(gitlab.syncSummary.get()).toBeInTheDocument();
    });
  });
});

const appLoaded = async () => {
  await waitFor(async () => {
    expect(await screen.findByText('loading')).not.toBeInTheDocument();
  });
};

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>,
  );
}
