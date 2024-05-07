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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import React from 'react';
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import ComputeEngineServiceMock from '../../../../../api/mocks/ComputeEngineServiceMock';
import GithubProvisioningServiceMock from '../../../../../api/mocks/GithubProvisioningServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import { Feature } from '../../../../../types/features';
import { GitHubProvisioningStatus } from '../../../../../types/provisioning';
import { TaskStatuses } from '../../../../../types/tasks';
import Authentication from '../Authentication';

let handler: GithubProvisioningServiceMock;
let system: SystemServiceMock;
let settingsHandler: SettingsServiceMock;
let computeEngineHandler: ComputeEngineServiceMock;

beforeEach(() => {
  handler = new GithubProvisioningServiceMock();
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

const ghContainer = byRole('tabpanel', { name: 'github GitHub' });

const ui = {
  saveButton: byRole('button', { name: 'settings.authentication.saml.form.save' }),
  customMessageInformation: byText('settings.authentication.custom_message_information'),
  enabledToggle: byRole('switch'),
  testButton: byText('settings.authentication.saml.form.test'),
  textbox1: byRole('textbox', { name: 'test1' }),
  textbox2: byRole('textbox', { name: 'test2' }),
  tab: byRole('tab', { name: 'github GitHub' }),
  cancelDialogButton: byRole('dialog').byRole('button', { name: 'cancel' }),
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
  allowUsersToSignUp: byRole('switch', {
    name: 'property.sonar.auth.github.allowUsersToSignUp.name',
  }),
  projectVisibility: byRole('switch', {
    name: 'property.provisioning.github.project.visibility.enabled.name',
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
    ui.mappingRow.getAll().find((row) => within(row).queryByText(text) !== null),
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
  insecureConfigWarning: byRole('dialog').byText(
    'settings.authentication.github.provisioning_change.insecure_config',
  ),
  jitProvisioningButton: ghContainer.byRole('radio', {
    name: /settings.authentication.form.provisioning_at_login/,
  }),
  githubProvisioningButton: ghContainer.byRole('radio', {
    name: /settings.authentication.github.form.provisioning_with_github/,
  }),
  githubProvisioningPending: ghContainer
    .byRole('list')
    .byRole('status')
    .byText(/synchronization_pending/),
  githubProvisioningInProgress: ghContainer
    .byRole('list')
    .byRole('status')
    .byText(/synchronization_in_progress/),
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
    name: 'settings.authentication.configuration.test',
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
  getConfigDetailsTitle: () => ui.configDetailsDialog.byRole('heading').get(),
  getOrgs: () => ui.configDetailsDialog.byRole('listitem').getAll(),
  getIconForOrg: (text: string, org: HTMLElement) => byLabelText(text).get(org),
  fillForm: async (user: UserEvent) => {
    await user.type(await ui.clientId.find(), 'Awsome GITHUB config');
    await user.type(ui.clientSecret.get(), 'Client shut');
    await user.type(ui.appId.get(), 'App id');
    await user.type(ui.privateKey.get(), 'Private Key');
    await user.type(ui.githubApiUrl.get(), 'API Url');
    await user.type(ui.githubWebUrl.get(), 'WEb Url');
    await user.type(ui.organizations.get(), 'organization1');
  },
  createConfiguration: async (user: UserEvent) => {
    await user.click(await ui.createConfigButton.find());
    await ui.fillForm(user);

    await user.click(ui.saveConfigButton.get());
  },
  enableConfiguration: async (user: UserEvent) => {
    await user.click(await ui.tab.find());
    await ui.createConfiguration(user);
    await user.click(await ui.enableConfigButton.find());
  },
  enableProvisioning: async (user: UserEvent) => {
    await user.click(await ui.tab.find());

    await ui.createConfiguration(user);

    await user.click(await ui.enableConfigButton.find());
    await user.click(await ui.githubProvisioningButton.find());
    await user.click(ui.saveGithubProvisioning.get());
    await user.click(ui.confirmProvisioningButton.get());
  },
};

describe('Github tab', () => {
  it('should render an empty Github configuration', async () => {
    renderAuthentication();
    const user = userEvent.setup();
    await user.click(await ui.tab.find());
    expect(await ui.noGithubConfiguration.find()).toBeInTheDocument();
  });

  it('should be able to create a configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();

    await user.click(await ui.tab.find());
    await user.click(await ui.createConfigButton.find());

    expect(ui.saveConfigButton.get()).toBeDisabled();

    await ui.fillForm(user);
    expect(ui.saveConfigButton.get()).toBeEnabled();

    await user.click(ui.saveConfigButton.get());

    expect(await ui.editConfigButton.find()).toBeInTheDocument();
  });

  it('should be able to edit configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();
    await user.click(await ui.tab.find());

    await ui.createConfiguration(user);

    await user.click(ui.editConfigButton.get());
    await user.click(ui.deleteOrg('organization1').get());

    await user.click(ui.saveConfigButton.get());

    await user.click(await ui.editConfigButton.find());

    expect(ui.organizations.get()).toHaveValue('');
  });

  it('should be able to enable/disable configuration', async () => {
    const user = userEvent.setup();
    renderAuthentication();
    await user.click(await ui.tab.find());

    await ui.createConfiguration(user);

    await user.click(await ui.enableConfigButton.find());

    expect(await ui.disableConfigButton.find()).toBeInTheDocument();
    await user.click(ui.disableConfigButton.get());
    await waitFor(() => expect(ui.disableConfigButton.query()).not.toBeInTheDocument());

    expect(await ui.enableConfigButton.find()).toBeInTheDocument();
  });

  it('should not allow edtion below Enterprise to select Github provisioning', async () => {
    const user = userEvent.setup();

    renderAuthentication();
    await user.click(await ui.tab.find());

    await ui.createConfiguration(user);
    await user.click(await ui.enableConfigButton.find());

    expect(await ui.jitProvisioningButton.find()).toBeChecked();
    expect(ui.githubProvisioningButton.get()).toHaveAttribute('aria-disabled', 'true');
  });

  it('should be able to choose provisioning', async () => {
    const user = userEvent.setup();

    renderAuthentication([Feature.GithubProvisioning]);
    await user.click(await ui.tab.find());

    await ui.createConfiguration(user);

    expect(await ui.enableFirstMessage.find()).toBeInTheDocument();
    await user.click(await ui.enableConfigButton.find());

    expect(await ui.jitProvisioningButton.find()).toBeChecked();

    expect(ui.saveGithubProvisioning.get()).toBeDisabled();
    await user.click(ui.allowUsersToSignUp.get());

    expect(ui.saveGithubProvisioning.get()).toBeEnabled();
    await user.click(ui.saveGithubProvisioning.get());

    await waitFor(() => expect(ui.saveGithubProvisioning.query()).toBeDisabled());

    await user.click(ui.githubProvisioningButton.get());

    expect(ui.saveGithubProvisioning.get()).toBeEnabled();
    await user.click(ui.saveGithubProvisioning.get());
    await user.click(ui.confirmProvisioningButton.get());

    expect(await ui.githubProvisioningButton.find()).toBeChecked();
    expect(ui.disableConfigButton.get()).toBeDisabled();
    expect(ui.saveGithubProvisioning.get()).toBeDisabled();
  });

  describe('Github Provisioning', () => {
    let user: UserEvent;

    beforeEach(() => {
      user = userEvent.setup();
    });

    it('should display a success status when the synchronisation is a success', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Success,
        executedAt: '2022-02-03T11:45:35+0200',
      });

      renderAuthentication([Feature.GithubProvisioning]);
      await ui.enableProvisioning(user);
      expect(ui.githubProvisioningSuccess.get()).toBeInTheDocument();
      expect(ui.syncSummary.get()).toBeInTheDocument();
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
      await ui.enableProvisioning(user);
      expect(ui.githubProvisioningSuccess.get()).toBeInTheDocument();
      expect(ui.githubProvisioningPending.get()).toBeInTheDocument();
    });

    it('should display an error alert when the synchronisation failed', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Failed,
        executedAt: '2022-02-03T11:45:35+0200',
        errorMessage: "T'es mauvais Jacques",
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await ui.enableProvisioning(user);
      expect(ui.githubProvisioningAlert.get()).toBeInTheDocument();
      expect(ghContainer.get()).toHaveTextContent("T'es mauvais Jacques");
      expect(ui.githubProvisioningSuccess.query()).not.toBeInTheDocument();
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
      await ui.enableProvisioning(user);
      expect(ui.githubProvisioningAlert.get()).toBeInTheDocument();
      expect(ghContainer.get()).toHaveTextContent("T'es mauvais Jacques");
      expect(ui.githubProvisioningSuccess.query()).not.toBeInTheDocument();
      expect(ui.githubProvisioningInProgress.get()).toBeInTheDocument();
    });

    it('should display that config is valid for both provisioning with 1 org', async () => {
      renderAuthentication([Feature.GithubProvisioning]);
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValiditySuccess.query()).toBeInTheDocument());
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
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValiditySuccess.query()).toBeInTheDocument());
      expect(ui.configurationValiditySuccess.get()).toHaveTextContent('2');

      await user.click(ui.viewConfigValidityDetailsButton.get());
      expect(ui.getConfigDetailsTitle()).toBeInTheDocument();
      expect(ui.getOrgs()).toHaveLength(3);
      expect(
        ui.getIconForOrg(
          'settings.authentication.github.configuration.validation.details.valid_label',
          ui.getOrgs()[0],
        ),
      ).toBeInTheDocument();
      expect(
        ui.getIconForOrg(
          'settings.authentication.github.configuration.validation.details.valid_label',
          ui.getOrgs()[1],
        ),
      ).toBeInTheDocument();
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
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValidityWarning.get()).toBeInTheDocument());
      expect(ui.configurationValidityWarning.get()).toHaveTextContent(errorMessage);

      await user.click(ui.viewConfigValidityDetailsButton.get());
      expect(ui.getConfigDetailsTitle()).toBeInTheDocument();
      expect(
        ui.configDetailsDialog
          .byText('settings.authentication.github.configuration.validation.valid.short')
          .get(),
      ).toBeInTheDocument();
      expect(ui.getOrgs()[0]).toHaveTextContent('org1 - Installation suspended');
      expect(
        ui.getIconForOrg(
          'settings.authentication.github.configuration.validation.details.invalid_label',
          ui.getOrgs()[0],
        ),
      ).toBeInTheDocument();

      await user.click(ui.configDetailsDialog.byRole('button', { name: 'close' }).get());

      await user.click(ui.githubProvisioningButton.get());
      await waitFor(() => expect(ui.configurationValidityError.get()).toBeInTheDocument());
      expect(ui.configurationValidityError.get()).toHaveTextContent(errorMessage);
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
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValiditySuccess.get()).toBeInTheDocument());
      expect(ui.configurationValiditySuccess.get()).toHaveTextContent('1');

      await user.click(ui.viewConfigValidityDetailsButton.get());
      expect(ui.getConfigDetailsTitle()).toBeInTheDocument();
      expect(
        ui.configDetailsDialog
          .byText('settings.authentication.github.configuration.validation.valid.short')
          .get(),
      ).toBeInTheDocument();
      expect(ui.getOrgs()[0]).toHaveTextContent('org1');
      expect(
        ui.getIconForOrg(
          'settings.authentication.github.configuration.validation.details.valid_label',
          ui.getOrgs()[0],
        ),
      ).toBeInTheDocument();
      expect(ui.getOrgs()[1]).toHaveTextContent(
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
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValidityError.query()).toBeInTheDocument());
      expect(ui.configurationValidityError.get()).toHaveTextContent(errorMessage);

      await user.click(ui.viewConfigValidityDetailsButton.get());
      expect(ui.getConfigDetailsTitle()).toBeInTheDocument();
      expect(
        ui.configDetailsDialog
          .byText(/settings.authentication.github.configuration.validation.invalid/)
          .get(),
      ).toBeInTheDocument();
      expect(ui.configDetailsDialog.get()).toHaveTextContent(errorMessage);
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
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValiditySuccess.query()).toBeInTheDocument());
      expect(ui.configurationValiditySuccess.get()).not.toHaveTextContent(errorMessage);

      await user.click(ui.viewConfigValidityDetailsButton.get());
      expect(ui.getConfigDetailsTitle()).toBeInTheDocument();
      expect(
        ui.configDetailsDialog
          .byText('settings.authentication.github.configuration.validation.valid.short')
          .get(),
      ).toBeInTheDocument();
      await user.click(ui.configDetailsDialog.byRole('button', { name: 'close' }).get());

      await user.click(ui.githubProvisioningButton.get());

      expect(ui.configurationValidityError.get()).toBeInTheDocument();
      expect(ui.configurationValidityError.get()).toHaveTextContent(errorMessage);

      await user.click(ui.viewConfigValidityDetailsButton.get());
      expect(
        ui.configDetailsDialog
          .byText(/settings.authentication.github.configuration.validation.invalid/)
          .get(),
      ).toBeInTheDocument();
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
      await ui.enableConfiguration(user);

      assertAppIsLoaded();

      await waitFor(() => expect(ui.configurationValiditySuccess.query()).toBeInTheDocument());

      await user.click(ui.viewConfigValidityDetailsButton.get());

      expect(ui.getOrgs()[0]).toHaveTextContent('org1');
      expect(
        ui.getIconForOrg(
          'settings.authentication.github.configuration.validation.details.valid_label',
          ui.getOrgs()[0],
        ),
      ).toBeInTheDocument();
      expect(ui.getOrgs()[1]).toHaveTextContent('org2 - Test error');
      expect(
        ui.getIconForOrg(
          'settings.authentication.github.configuration.validation.details.invalid_label',
          ui.getOrgs()[1],
        ),
      ).toBeInTheDocument();

      await user.click(ui.configDetailsDialog.byRole('button', { name: 'close' }).get());

      await user.click(ui.githubProvisioningButton.get());

      expect(ui.configurationValidityError.get()).toBeInTheDocument();
      expect(ui.configurationValidityError.get()).toHaveTextContent(
        `settings.authentication.github.configuration.validation.invalid_org.org2.${errorMessage}`,
      );
      await user.click(ui.viewConfigValidityDetailsButton.get());

      expect(
        ui.configDetailsDialog
          .byLabelText(
            'settings.authentication.github.configuration.validation.details.invalid_label',
          )
          .getAll(),
      ).toHaveLength(1);
      expect(ui.getOrgs()[1]).toHaveTextContent(`org2 - ${errorMessage}`);
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
      await ui.enableConfiguration(user);
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

      assertAppIsLoaded();

      expect(await ui.configurationValidityError.find()).toBeInTheDocument();

      await user.click(ui.checkConfigButton.get());

      expect(ui.configurationValiditySuccess.get()).toBeInTheDocument();
      expect(ui.configurationValidityError.query()).not.toBeInTheDocument();
    });

    it('should show warning', async () => {
      handler.addProvisioningTask({
        status: TaskStatuses.Success,
        warnings: ['Warning'],
      });
      renderAuthentication([Feature.GithubProvisioning]);
      await ui.enableProvisioning(user);

      expect(await ui.syncWarning.find()).toBeInTheDocument();
      expect(ui.syncSummary.get()).toBeInTheDocument();
    });

    it('should display a modal if user was already using auto and continue using auto provisioning', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      settingsHandler.set('sonar.auth.github.userConsentForPermissionProvisioningRequired', '');
      renderAuthentication([Feature.GithubProvisioning]);

      await user.click(await ui.tab.find());

      expect(await ui.consentDialog.find()).toBeInTheDocument();
      await user.click(ui.continueAutoButton.get());

      expect(await ui.githubProvisioningButton.find()).toBeChecked();
      expect(ui.consentDialog.query()).not.toBeInTheDocument();
    });

    it('should display a modal if user was already using auto and switch to JIT', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      settingsHandler.set('sonar.auth.github.userConsentForPermissionProvisioningRequired', '');
      renderAuthentication([Feature.GithubProvisioning]);

      await user.click(await ui.tab.find());

      expect(await ui.consentDialog.find()).toBeInTheDocument();
      await user.click(ui.switchJitButton.get());

      expect(await ui.jitProvisioningButton.find()).toBeChecked();
      expect(ui.consentDialog.query()).not.toBeInTheDocument();
    });

    it('should sort mapping rows', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await ui.tab.find());

      expect(await ui.editMappingButton.find()).toBeInTheDocument();
      await user.click(ui.editMappingButton.get());

      const rows = (await ui.mappingRow.findAll()).filter(
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
      await user.click(await ui.tab.find());

      await ui.createConfiguration(user);
      await user.click(await ui.enableConfigButton.find());

      expect(await ui.jitProvisioningButton.find()).toBeChecked();
      expect(ui.editMappingButton.query()).not.toBeInTheDocument();
      await user.click(ui.githubProvisioningButton.get());
      expect(await ui.editMappingButton.find()).toBeInTheDocument();
      await user.click(ui.editMappingButton.get());

      expect(await ui.mappingRow.findAll()).toHaveLength(7);

      let readCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('read'));
      let adminCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('admin'));

      expect(readCheckboxes[0]).toBeChecked();
      expect(readCheckboxes[5]).not.toBeChecked();
      expect(adminCheckboxes[5]).toBeChecked();

      await user.click(readCheckboxes[0]);
      await user.click(readCheckboxes[5]);
      await user.click(adminCheckboxes[5]);
      await user.click(ui.mappingDialogClose.get());

      await user.click(ui.saveGithubProvisioning.get());
      await user.click(ui.confirmProvisioningButton.get());

      // Clean local mapping state
      await user.click(ui.jitProvisioningButton.get());
      await user.click(ui.githubProvisioningButton.get());

      await user.click(ui.editMappingButton.get());
      readCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('read'));
      adminCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('admin'));

      expect(readCheckboxes[0]).not.toBeChecked();
      expect(readCheckboxes[5]).toBeChecked();
      expect(adminCheckboxes[5]).not.toBeChecked();
      await user.click(ui.mappingDialogClose.get());
    });

    it('should apply new mapping on auto-provisioning', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await ui.tab.find());

      expect(await ui.saveGithubProvisioning.find()).toBeDisabled();
      await user.click(ui.editMappingButton.get());

      expect(await ui.mappingRow.findAll()).toHaveLength(7);

      let readCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('read'))[0];

      expect(readCheckboxes).toBeChecked();

      await user.click(readCheckboxes);
      await user.click(ui.mappingDialogClose.get());

      expect(await ui.saveGithubProvisioning.find()).toBeEnabled();

      await user.click(ui.saveGithubProvisioning.get());
      await user.click(ui.confirmProvisioningButton.get());

      // Clean local mapping state
      await user.click(ui.jitProvisioningButton.get());
      await user.click(ui.githubProvisioningButton.get());

      await user.click(ui.editMappingButton.get());
      readCheckboxes = ui.mappingCheckbox.getAll(ui.getMappingRowByRole('read'))[0];

      expect(readCheckboxes).not.toBeChecked();
      await user.click(ui.mappingDialogClose.get());
    });

    it('should add/remove/update custom roles', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      handler.enableGithubProvisioning();
      handler.addGitHubCustomRole('custom1', ['user', 'codeViewer', 'scan']);
      handler.addGitHubCustomRole('custom2', ['user', 'codeViewer', 'issueAdmin', 'scan']);
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await ui.tab.find());

      expect(await ui.saveGithubProvisioning.find()).toBeDisabled();
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
      await user.type(ui.customRoleInput.get(), 'read');
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

      expect(await ui.saveGithubProvisioning.find()).toBeEnabled();
      await user.click(ui.saveGithubProvisioning.get());

      await user.click(ui.confirmProvisioningButton.get());

      // Clean local mapping state
      await user.click(ui.jitProvisioningButton.get());
      await user.click(ui.githubProvisioningButton.get());

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

    it('should should show insecure config warning', async () => {
      const user = userEvent.setup();
      settingsHandler.presetGithubAutoProvisioning();
      renderAuthentication([Feature.GithubProvisioning]);
      await user.click(await ui.tab.find());

      expect(ui.allowUsersToSignUp.get()).toBeChecked();
      await user.click(ui.allowUsersToSignUp.get());
      await user.click(ui.saveGithubProvisioning.get());

      expect(ui.insecureConfigWarning.query()).not.toBeInTheDocument();

      await user.click(ui.allowUsersToSignUp.get());
      await user.click(ui.saveGithubProvisioning.get());

      expect(ui.insecureConfigWarning.get()).toBeInTheDocument();
      await user.click(ui.confirmProvisioningButton.get());

      await user.click(ui.githubProvisioningButton.get());
      await user.click(ui.saveGithubProvisioning.get());

      expect(ui.insecureConfigWarning.get()).toBeInTheDocument();
      await user.click(ui.confirmProvisioningButton.get());

      await user.click(ui.projectVisibility.get());
      await user.click(ui.saveGithubProvisioning.get());

      expect(ui.insecureConfigWarning.get()).toBeInTheDocument();
      await user.click(ui.confirmProvisioningButton.get());

      await user.click(ui.editConfigButton.get());
      await user.click(ui.saveConfigButton.get());

      expect(ui.insecureConfigWarning.get()).toBeInTheDocument();
      await user.click(ui.cancelDialogButton.get());
      await user.type(ui.organizations.get(), '123');
      await user.click(ui.saveConfigButton.get());
      expect(ui.insecureConfigWarning.query()).not.toBeInTheDocument();

      await user.click(ui.projectVisibility.get());
      await user.click(ui.saveGithubProvisioning.get());
      expect(ui.insecureConfigWarning.query()).not.toBeInTheDocument();

      await user.click(ui.jitProvisioningButton.get());
      await user.click(ui.saveGithubProvisioning.get());
      expect(ui.confirmProvisioningButton.get()).toBeInTheDocument();
      expect(ui.insecureConfigWarning.query()).not.toBeInTheDocument();
      await user.click(ui.confirmProvisioningButton.get());

      await user.click(ui.allowUsersToSignUp.get());
      await user.click(ui.saveGithubProvisioning.get());
      expect(ui.insecureConfigWarning.query()).not.toBeInTheDocument();
    });
  });
});

const assertAppIsLoaded = () => {
  expect(screen.queryByText('loading')).not.toBeInTheDocument();
};

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>,
    `?tab=${AlmKeys.GitHub}`,
  );
}
