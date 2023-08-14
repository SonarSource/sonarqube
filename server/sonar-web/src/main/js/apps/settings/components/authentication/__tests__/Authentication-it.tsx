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
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import React from 'react';
import AuthenticationServiceMock from '../../../../../api/mocks/AuthenticationServiceMock';
import ComputeEngineServiceMock from '../../../../../api/mocks/ComputeEngineServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../../helpers/testSelector';
import { Feature } from '../../../../../types/features';
import { GitHubProvisioningStatus } from '../../../../../types/provisioning';
import { TaskStatuses } from '../../../../../types/tasks';
import Authentication from '../Authentication';

jest.mock('../../../../../api/system');

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

const ui = {
  saveButton: byRole('button', { name: 'settings.authentication.saml.form.save' }),
  customMessageInformation: byText('settings.authentication.custom_message_information'),
  enabledToggle: byRole('switch'),
  testButton: byText('settings.authentication.saml.form.test'),
  textbox1: byRole('textbox', { name: 'test1' }),
  textbox2: byRole('textbox', { name: 'test2' }),
  saml: {
    noSamlConfiguration: byText('settings.authentication.saml.form.not_configured'),
    createConfigButton: byRole('button', { name: 'settings.authentication.form.create' }),
    providerName: byRole('textbox', { name: 'Provider Name' }),
    providerId: byRole('textbox', { name: 'Provider ID' }),
    providerCertificate: byRole('textbox', { name: 'Identity provider certificate' }),
    loginUrl: byRole('textbox', { name: 'SAML login url' }),
    userLoginAttribute: byRole('textbox', { name: 'SAML user login attribute' }),
    userNameAttribute: byRole('textbox', { name: 'SAML user name attribute' }),
    saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
    confirmProvisioningButton: byRole('button', { name: 'yes' }),
    saveScim: byRole('button', { name: 'save' }),
    groupAttribute: byRole('textbox', { name: 'property.sonar.auth.saml.group.name.name' }),
    enableConfigButton: byRole('button', { name: 'settings.authentication.form.enable' }),
    disableConfigButton: byRole('button', { name: 'settings.authentication.form.disable' }),
    editConfigButton: byRole('button', { name: 'settings.authentication.form.edit' }),
    enableFirstMessage: byText('settings.authentication.saml.enable_first'),
    jitProvisioningButton: byRole('radio', {
      name: 'settings.authentication.saml.form.provisioning_at_login',
    }),
    scimProvisioningButton: byRole('radio', {
      name: 'settings.authentication.saml.form.provisioning_with_scim',
    }),
    fillForm: async (user: UserEvent) => {
      const { saml } = ui;
      await act(async () => {
        await user.clear(saml.providerName.get());
        await user.type(saml.providerName.get(), 'Awsome SAML config');
        await user.type(saml.providerId.get(), 'okta-1234');
        await user.type(saml.loginUrl.get(), 'http://test.org');
        await user.type(saml.providerCertificate.get(), '-secret-');
        await user.type(saml.userLoginAttribute.get(), 'login');
        await user.type(saml.userNameAttribute.get(), 'name');
      });
    },
    createConfiguration: async (user: UserEvent) => {
      const { saml } = ui;
      await act(async () => {
        await user.click((await saml.createConfigButton.findAll())[0]);
      });
      await saml.fillForm(user);
      await act(async () => {
        await user.click(saml.saveConfigButton.get());
      });
    },
  },
  github: {
    tab: byRole('tab', { name: 'github GitHub' }),
    noGithubConfiguration: byText('settings.authentication.github.form.not_configured'),
    createConfigButton: byRole('button', { name: 'settings.authentication.form.create' }),
    clientId: byRole('textbox', { name: 'Client ID' }),
    clientSecret: byRole('textbox', { name: 'Client Secret' }),
    githubAppId: byRole('textbox', { name: 'GitHub App ID' }), // not working
    privateKey: byRole('textarea', { name: 'Private Key' }), // not working
    githubApiUrl: byRole('textbox', { name: 'The API url for a GitHub instance.' }),
    githubWebUrl: byRole('textbox', { name: 'The WEB url for a GitHub instance.' }),
    allowUserToSignUp: byRole('switch', {
      name: 'sonar.auth.github.allowUsersToSignUp',
    }),
    syncGroupsAsTeams: byRole('switch', { name: 'sonar.auth.github.groupsSync' }),
    organizations: byRole('textbox', { name: 'Organizations' }),
    saveConfigButton: byRole('button', { name: 'settings.almintegration.form.save' }),
    confirmProvisioningButton: byRole('button', { name: 'yes' }),
    saveGithubProvisioning: byRole('button', { name: 'save' }),
    groupAttribute: byRole('textbox', { name: 'property.sonar.auth.github.group.name.name' }),
    enableConfigButton: byRole('button', { name: 'settings.authentication.form.enable' }),
    disableConfigButton: byRole('button', { name: 'settings.authentication.form.disable' }),
    editConfigButton: byRole('button', { name: 'settings.authentication.form.edit' }),
    deleteOrg: (org: string) =>
      byRole('button', {
        name: `settings.definition.delete_value.property.sonar.auth.github.organizations.name.${org}`,
      }),
    enableFirstMessage: byText('settings.authentication.github.enable_first'),
    jitProvisioningButton: byRole('radio', {
      name: 'settings.authentication.form.provisioning_at_login',
    }),
    githubProvisioningButton: byRole('radio', {
      name: 'settings.authentication.github.form.provisioning_with_github',
    }),
    githubProvisioningPending: byText(/synchronization_pending/),
    githubProvisioningInProgress: byText(/synchronization_in_progress/),
    githubProvisioningSuccess: byText(/synchronization_successful/),
    githubProvisioningAlert: byText(/synchronization_failed/),
    configurationValidityLoading: byRole('status', {
      name: /github.configuration.validation.loading/,
    }),
    configurationValiditySuccess: byRole('status', {
      name: /github.configuration.validation.valid/,
    }),
    configurationValidityError: byRole('status', {
      name: /github.configuration.validation.invalid/,
    }),
    syncWarning: byText(/Warning/),
    syncSummary: byText(/Test summary/),
    configurationValidityWarning: byRole('status', {
      name: /github.configuration.validation.valid.short/,
    }),
    checkConfigButton: byRole('button', {
      name: 'settings.authentication.github.configuration.validation.test',
    }),
    viewConfigValidityDetailsButton: byRole('button', {
      name: 'settings.authentication.github.configuration.validation.details',
    }),
    configDetailsDialog: byRole('dialog', {
      name: 'settings.authentication.github.configuration.validation.details.title',
    }),
    getConfigDetailsTitle: () => within(ui.github.configDetailsDialog.get()).getByRole('heading'),
    getOrgs: () => within(ui.github.configDetailsDialog.get()).getAllByRole('listitem'),
    fillForm: async (user: UserEvent) => {
      const { github } = ui;
      await act(async () => {
        await user.type(await github.clientId.find(), 'Awsome GITHUB config');
        await user.type(github.clientSecret.get(), 'Client shut');
        await user.type(github.githubApiUrl.get(), 'API Url');
        await user.type(github.githubWebUrl.get(), 'WEb Url');
        await user.type(github.organizations.get(), 'organization1');
      });
    },
    createConfiguration: async (user: UserEvent) => {
      const { github } = ui;
      await act(async () => {
        await user.click((await github.createConfigButton.findAll())[1]);
      });
      await github.fillForm(user);
      await act(async () => {
        await user.click(github.saveConfigButton.get());
      });
    },
    enableConfiguration: async (user: UserEvent) => {
      const { github } = ui;
      await act(async () => user.click(await github.tab.find()));
      await github.createConfiguration(user);
      await act(async () => user.click(await github.enableConfigButton.find()));
    },
    enableProvisioning: async (user: UserEvent) => {
      const { github } = ui;
      await act(async () => user.click(await github.tab.find()));

      await github.createConfiguration(user);

      await act(async () => user.click(await github.enableConfigButton.find()));
      await user.click(await github.githubProvisioningButton.find());
      await user.click(github.saveGithubProvisioning.get());
      await act(() => user.click(github.confirmProvisioningButton.get()));
    },
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
    'true'
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

    await user.click((await saml.createConfigButton.findAll())[0]);

    expect(saml.saveConfigButton.get()).toBeDisabled();
    await saml.fillForm(user);
    expect(saml.saveConfigButton.get()).toBeEnabled();

    await act(async () => {
      await user.click(saml.saveConfigButton.get());
    });

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

    await user.type(saml.groupAttribute.get(), 'group');
    expect(saml.saveScim.get()).toBeEnabled();
    await user.click(saml.saveScim.get());
    await waitFor(() => expect(saml.saveScim.query()).toBeDisabled());

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
    await user.click((await github.createConfigButton.findAll())[1]);

    expect(github.saveConfigButton.get()).toBeDisabled();

    await github.fillForm(user);
    expect(github.saveConfigButton.get()).toBeEnabled();

    await act(async () => {
      await user.click(github.saveConfigButton.get());
    });

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
    await user.click(github.syncGroupsAsTeams.get());

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

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());
      expect(github.configurationValiditySuccess.get()).toHaveTextContent('2');

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label'
      );
      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg1'
      );
      expect(github.getOrgs()[1]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg2'
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

      await waitFor(() => expect(github.configurationValidityWarning.get()).toBeInTheDocument());
      expect(github.configurationValidityWarning.get()).toHaveTextContent(errorMessage);

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label'
      );
      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_labelorg1 - Installation suspended'
      );

      await act(() =>
        user.click(within(github.configDetailsDialog.get()).getByRole('button', { name: 'close' }))
      );

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

      await waitFor(() => expect(github.configurationValiditySuccess.get()).toBeInTheDocument());
      expect(github.configurationValiditySuccess.get()).toHaveTextContent('1');

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label'
      );
      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg1'
      );
      expect(github.getOrgs()[1]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.org_not_found.organization1'
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

      await waitFor(() => expect(github.configurationValidityError.query()).toBeInTheDocument());
      expect(github.configurationValidityError.get()).toHaveTextContent(errorMessage);

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_label'
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

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());
      expect(github.configurationValiditySuccess.get()).not.toHaveTextContent(errorMessage);

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_label'
      );
      await act(() =>
        user.click(within(github.configDetailsDialog.get()).getByRole('button', { name: 'close' }))
      );

      await act(() => user.click(github.githubProvisioningButton.get()));

      expect(github.configurationValidityError.get()).toBeInTheDocument();
      expect(github.configurationValidityError.get()).toHaveTextContent(errorMessage);

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getConfigDetailsTitle()).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_label'
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

      await waitFor(() => expect(github.configurationValiditySuccess.query()).toBeInTheDocument());

      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));

      expect(github.getOrgs()[0]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.valid_labelorg1'
      );
      expect(github.getOrgs()[1]).toHaveTextContent(
        'settings.authentication.github.configuration.validation.details.invalid_labelorg2 - Test error'
      );

      await act(() =>
        user.click(within(github.configDetailsDialog.get()).getByRole('button', { name: 'close' }))
      );

      await act(() => user.click(github.githubProvisioningButton.get()));

      expect(github.configurationValidityError.get()).toBeInTheDocument();
      expect(github.configurationValidityError.get()).toHaveTextContent(
        `settings.authentication.github.configuration.validation.invalid_org.org2.${errorMessage}`
      );
      await act(() => user.click(github.viewConfigValidityDetailsButton.get()));
      expect(github.getOrgs()[1]).toHaveTextContent(
        `settings.authentication.github.configuration.validation.details.invalid_labelorg2 - ${errorMessage}`
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

      expect(await github.configurationValidityError.find()).toBeInTheDocument();

      await act(() => user.click(github.checkConfigButton.get()));

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
  });
});

function renderAuthentication(features: Feature[] = []) {
  renderComponent(
    <AvailableFeaturesContext.Provider value={features}>
      <Authentication definitions={definitions} />
    </AvailableFeaturesContext.Provider>
  );
}
