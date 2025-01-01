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

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import AlmSettingsServiceMock from '../../../../../api/mocks/AlmSettingsServiceMock';
import DopTranslationServiceMock from '../../../../../api/mocks/DopTranslationServiceMock';
import GithubProvisioningServiceMock from '../../../../../api/mocks/GithubProvisioningServiceMock';
import GitlabProvisioningServiceMock from '../../../../../api/mocks/GitlabProvisioningServiceMock';
import PermissionsServiceMock from '../../../../../api/mocks/PermissionsServiceMock';
import ProjectManagementServiceMock from '../../../../../api/mocks/ProjectsManagementServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import SystemServiceMock from '../../../../../api/mocks/SystemServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockGitHubConfiguration } from '../../../../../helpers/mocks/dop-translation';
import { mockPermissionGroup, mockPermissionUser } from '../../../../../helpers/mocks/permissions';
import { mockProject } from '../../../../../helpers/mocks/projects';
import {
  PERMISSIONS_ORDER_FOR_PROJECT_TEMPLATE,
  PERMISSIONS_ORDER_FOR_VIEW,
} from '../../../../../helpers/permissions';
import {
  RenderContext,
  renderAppWithComponentContext,
} from '../../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../../types/alm-settings';
import { ComponentContextShape } from '../../../../../types/component';
import { Feature } from '../../../../../types/features';
import { Permissions } from '../../../../../types/permissions';
import { ProvisioningType } from '../../../../../types/provisioning';
import { Component, PermissionGroup, PermissionUser, Provider } from '../../../../../types/types';
import { projectPermissionsRoutes } from '../../../routes';
import { getPageObject } from '../../../test-utils';

let serviceMock: PermissionsServiceMock;
let dopTranslationHandler: DopTranslationServiceMock;
let githubHandler: GithubProvisioningServiceMock;
let gitlabHandler: GitlabProvisioningServiceMock;
let almHandler: AlmSettingsServiceMock;
let settingsHandler: SettingsServiceMock;
let projectHandler: ProjectManagementServiceMock;
let systemHandler: SystemServiceMock;

beforeAll(() => {
  serviceMock = new PermissionsServiceMock();
  dopTranslationHandler = new DopTranslationServiceMock();
  githubHandler = new GithubProvisioningServiceMock(dopTranslationHandler);
  gitlabHandler = new GitlabProvisioningServiceMock();
  almHandler = new AlmSettingsServiceMock();
  settingsHandler = new SettingsServiceMock();
  projectHandler = new ProjectManagementServiceMock(settingsHandler);
  systemHandler = new SystemServiceMock();
});

afterEach(() => {
  serviceMock.reset();
  dopTranslationHandler.reset();
  githubHandler.reset();
  gitlabHandler.reset();
  almHandler.reset();
  settingsHandler.reset();
  projectHandler.reset();
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

    expect(await screen.findByText(description)).toBeInTheDocument();
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

    expect(screen.getAllByRole('row').length).toBe(10);
    await ui.toggleFilterByPermission(Permissions.Admin);
    expect(screen.getAllByRole('row').length).toBe(3);
    await ui.toggleFilterByPermission(Permissions.Admin);
    expect(screen.getAllByRole('row').length).toBe(10);
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
      ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).query(),
    ).not.toBeInTheDocument();
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
    expect(
      ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get(),
    ).toBeInTheDocument();

    await ui.turnProjectPublic();
    expect(ui.makePublicDisclaimer.get()).toBeInTheDocument();
    await ui.confirmTurnProjectPublic();
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

describe('GitHub provisioning', () => {
  beforeEach(() => {
    systemHandler.setProvider(Provider.Github);
  });

  it('should not allow to change visibility for GH Project with auto-provisioning', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(
      mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
    );
    almHandler.handleSetProjectBinding(AlmKeys.GitHub, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp({}, { featureList: [Feature.GithubProvisioning] });
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).toBeDisabled();
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeDisabled();
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).not.toBeChecked();
  });

  it('should allow to change visibility for non-GH Project', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(
      mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
    );
    almHandler.handleSetProjectBinding(AlmKeys.Azure, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp({}, { featureList: [Feature.GithubProvisioning] });
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).not.toHaveClass('disabled');
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(ui.visibilityRadio(Visibility.Private).get()).not.toHaveClass('disabled');
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
  });

  it('should allow to change visibility for GH Project with disabled auto-provisioning', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(mockGitHubConfiguration());
    almHandler.handleSetProjectBinding(AlmKeys.GitHub, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp({}, { featureList: [Feature.GithubProvisioning] });
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).not.toHaveClass('disabled');
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(ui.visibilityRadio(Visibility.Private).get()).not.toHaveClass('disabled');
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
  });

  it('should have disabled permissions for GH Project', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(
      mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
    );
    almHandler.handleSetProjectBinding(AlmKeys.GitHub, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp(
      {},
      { featureList: [Feature.GithubProvisioning] },
      {
        component: mockComponent({ visibility: Visibility.Private }),
      },
    );
    await ui.appLoaded();

    expect(ui.pageTitle.get()).toBeInTheDocument();
    await waitFor(() =>
      expect(ui.pageTitle.get()).toHaveAccessibleName(/project_permission.managed/),
    );
    expect(ui.pageTitle.byRole('img').get()).toBeInTheDocument();
    expect(ui.githubExplanations.get()).toBeInTheDocument();

    expect(ui.projectPermissionCheckbox('John', Permissions.Admin).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('John', Permissions.Admin).get()).toBeDisabled();
    expect(ui.projectPermissionCheckbox('Alexa', Permissions.IssueAdmin).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('Alexa', Permissions.IssueAdmin).get()).toBeEnabled();
    await ui.toggleProjectPermission('Alexa', Permissions.IssueAdmin);
    expect(ui.confirmRemovePermissionDialog.get()).toBeInTheDocument();
    expect(ui.confirmRemovePermissionDialog.get()).toHaveTextContent(
      `${Permissions.IssueAdmin}Alexa`,
    );
    await user.click(ui.confirmRemovePermissionDialog.byRole('button', { name: 'confirm' }).get());
    expect(ui.projectPermissionCheckbox('Alexa', Permissions.IssueAdmin).get()).not.toBeChecked();

    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()).toBeEnabled();
    await ui.toggleProjectPermission('sonar-users', Permissions.Browse);
    expect(ui.confirmRemovePermissionDialog.get()).toBeInTheDocument();
    expect(ui.confirmRemovePermissionDialog.get()).toHaveTextContent(
      `${Permissions.Browse}sonar-users`,
    );
    await user.click(ui.confirmRemovePermissionDialog.byRole('button', { name: 'confirm' }).get());
    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()).not.toBeChecked();
    expect(ui.projectPermissionCheckbox('sonar-admins', Permissions.Admin).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('sonar-admins', Permissions.Admin).get()).toHaveAttribute(
      'disabled',
    );

    const johnRow = screen.getAllByRole('row')[4];
    expect(johnRow).toHaveTextContent('John');
    expect(ui.githubLogo.get(johnRow)).toBeInTheDocument();
    const alexaRow = screen.getAllByRole('row')[5];
    expect(alexaRow).toHaveTextContent('Alexa');
    expect(ui.githubLogo.query(alexaRow)).not.toBeInTheDocument();
    const usersGroupRow = screen.getAllByRole('row')[1];
    expect(usersGroupRow).toHaveTextContent('sonar-users');
    expect(ui.githubLogo.query(usersGroupRow)).not.toBeInTheDocument();
    const adminsGroupRow = screen.getAllByRole('row')[2];
    expect(adminsGroupRow).toHaveTextContent('sonar-admins');
    expect(ui.githubLogo.query(adminsGroupRow)).toBeInTheDocument();

    expect(ui.applyTemplateBtn.query()).not.toBeInTheDocument();

    // not possible to grant permissions at all
    expect(
      screen
        .getAllByRole('checkbox', { checked: false })
        .every((item) => item.getAttributeNames().includes('disabled')),
    ).toBe(true);
  });

  it('should allow to change permissions for GH Project without auto-provisioning', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(mockGitHubConfiguration());
    almHandler.handleSetProjectBinding(AlmKeys.GitHub, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp(
      { visibility: Visibility.Private },
      { featureList: [Feature.GithubProvisioning] },
    );
    await ui.appLoaded();

    expect(ui.pageTitle.get()).toBeInTheDocument();
    expect(ui.pageTitle.byRole('img').query()).not.toBeInTheDocument();

    expect(ui.applyTemplateBtn.get()).toBeInTheDocument();

    // no restrictions
    expect(
      screen
        .getAllByRole('checkbox')
        .every((item) => item.getAttributeNames().includes('disabled')),
    ).toBe(false);
  });

  it('should allow to change permissions for non-GH Project', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(
      mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
    );
    renderPermissionsProjectApp({}, { featureList: [Feature.GithubProvisioning] });
    await ui.appLoaded();

    expect(ui.pageTitle.get()).toBeInTheDocument();
    expect(ui.nonGHProjectWarning.get()).toBeInTheDocument();
    expect(ui.pageTitle.byRole('img').query()).not.toBeInTheDocument();

    expect(ui.applyTemplateBtn.get()).toBeInTheDocument();

    // no restrictions
    expect(
      screen
        .getAllByRole('checkbox')
        .every((item) => item.getAttributeNames().includes('disabled')),
    ).toBe(false);
  });

  it.each([ComponentQualifier.Portfolio, ComponentQualifier.Application])(
    'shoudlnt show sync warning for portfolio and applications',
    async (qualifier) => {
      const user = userEvent.setup();
      const ui = getPageObject(user);
      dopTranslationHandler.gitHubConfigurations.push(
        mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
      );
      renderPermissionsProjectApp({ qualifier }, { featureList: [Feature.GithubProvisioning] });
      await ui.appLoaded();

      expect(ui.pageTitle.get()).toBeInTheDocument();
      expect(ui.nonGHProjectWarning.query()).not.toBeInTheDocument();
    },
  );
});

describe('GitLab provisioning', () => {
  beforeEach(() => {
    systemHandler.setProvider(Provider.Gitlab);
    almHandler.handleSetProjectBinding(AlmKeys.GitLab, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    projectHandler.setProjects([mockProject({ key: 'my-project', managed: true })]);
  });

  it('should not allow to change visibility for GitLab Project with auto-provisioning', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    dopTranslationHandler.gitHubConfigurations.push(
      mockGitHubConfiguration({ provisioningType: ProvisioningType.jit }),
    );
    almHandler.handleSetProjectBinding(AlmKeys.GitLab, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });

    renderPermissionsProjectApp({}, { featureList: [Feature.GitlabProvisioning] });
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).toBeDisabled();
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeDisabled();
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).not.toBeChecked();
  });

  it('should allow to change visibility for non-GitLab Project', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    almHandler.handleSetProjectBinding(AlmKeys.GitHub, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp({}, { featureList: [Feature.GitlabProvisioning] });
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).not.toHaveClass('disabled');
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(ui.visibilityRadio(Visibility.Private).get()).not.toHaveClass('disabled');
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
  });

  it('should allow to change visibility for GitLab Project with disabled auto-provisioning', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    almHandler.handleSetProjectBinding(AlmKeys.GitLab, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    projectHandler.setProjects([mockProject({ key: 'my-project', managed: false })]);
    renderPermissionsProjectApp({}, { featureList: [Feature.GitlabProvisioning] });
    await ui.appLoaded();

    expect(ui.visibilityRadio(Visibility.Public).get()).not.toHaveClass('disabled');
    expect(ui.visibilityRadio(Visibility.Public).get()).toBeChecked();
    expect(ui.visibilityRadio(Visibility.Private).get()).not.toHaveClass('disabled');
    await ui.turnProjectPrivate();
    expect(ui.visibilityRadio(Visibility.Private).get()).toBeChecked();
  });

  it('should have disabled permissions for GitLab Project', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);
    almHandler.handleSetProjectBinding(AlmKeys.GitLab, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp(
      {},
      { featureList: [Feature.GitlabProvisioning] },
      {
        component: mockComponent({ visibility: Visibility.Private }),
      },
    );
    await ui.appLoaded();

    expect(ui.pageTitle.get()).toBeInTheDocument();
    await waitFor(() =>
      expect(ui.pageTitle.get()).toHaveAccessibleName(/project_permission.managed/),
    );
    expect(ui.pageTitle.byRole('img').get()).toBeInTheDocument();
    expect(ui.gitlabExplanations.get()).toBeInTheDocument();

    expect(ui.projectPermissionCheckbox('John', Permissions.Admin).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('John', Permissions.Admin).get()).toBeDisabled();
    expect(ui.projectPermissionCheckbox('Alexa', Permissions.IssueAdmin).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('Alexa', Permissions.IssueAdmin).get()).toBeEnabled();
    await ui.toggleProjectPermission('Alexa', Permissions.IssueAdmin);
    expect(ui.confirmRemovePermissionDialog.get()).toBeInTheDocument();
    expect(ui.confirmRemovePermissionDialog.get()).toHaveTextContent(
      `${Permissions.IssueAdmin}Alexa`,
    );
    await user.click(ui.confirmRemovePermissionDialog.byRole('button', { name: 'confirm' }).get());
    expect(ui.projectPermissionCheckbox('Alexa', Permissions.IssueAdmin).get()).not.toBeChecked();

    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()).toBeEnabled();
    await ui.toggleProjectPermission('sonar-users', Permissions.Browse);
    expect(ui.confirmRemovePermissionDialog.get()).toBeInTheDocument();
    expect(ui.confirmRemovePermissionDialog.get()).toHaveTextContent(
      `${Permissions.Browse}sonar-users`,
    );
    await user.click(ui.confirmRemovePermissionDialog.byRole('button', { name: 'confirm' }).get());
    expect(ui.projectPermissionCheckbox('sonar-users', Permissions.Browse).get()).not.toBeChecked();
    expect(ui.projectPermissionCheckbox('sonar-admins', Permissions.Admin).get()).toBeChecked();
    expect(ui.projectPermissionCheckbox('sonar-admins', Permissions.Admin).get()).toHaveAttribute(
      'disabled',
    );

    const johnRow = screen.getAllByRole('row')[4];
    expect(johnRow).toHaveTextContent('John');
    expect(ui.gitlabLogo.get(johnRow)).toBeInTheDocument();
    const alexaRow = screen.getAllByRole('row')[5];
    expect(alexaRow).toHaveTextContent('Alexa');
    expect(ui.gitlabLogo.query(alexaRow)).not.toBeInTheDocument();
    const usersGroupRow = screen.getAllByRole('row')[1];
    expect(usersGroupRow).toHaveTextContent('sonar-users');
    expect(ui.gitlabLogo.query(usersGroupRow)).not.toBeInTheDocument();
    const adminsGroupRow = screen.getAllByRole('row')[2];
    expect(adminsGroupRow).toHaveTextContent('sonar-admins');
    expect(ui.gitlabLogo.query(adminsGroupRow)).toBeInTheDocument();

    expect(ui.applyTemplateBtn.query()).not.toBeInTheDocument();

    // not possible to grant permissions at all
    expect(
      screen
        .getAllByRole('checkbox', { checked: false })
        .every((item) => item.getAttributeNames().includes('disabled')),
    ).toBe(true);
  });

  it('should allow to change permissions for GitLab Project without auto-provisioning', async () => {
    const user = userEvent.setup();
    const ui = getPageObject(user);

    projectHandler.setProjects([mockProject({ key: 'my-project', managed: false })]);
    almHandler.handleSetProjectBinding(AlmKeys.GitLab, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });
    renderPermissionsProjectApp(
      { visibility: Visibility.Private },
      { featureList: [Feature.GitlabProvisioning] },
    );
    await ui.appLoaded();

    expect(ui.pageTitle.get()).toBeInTheDocument();
    expect(ui.pageTitle.byRole('img').query()).not.toBeInTheDocument();

    expect(ui.applyTemplateBtn.get()).toBeInTheDocument();

    // no restrictions
    expect(
      screen
        .getAllByRole('checkbox')
        .every((item) => item.getAttributeNames().includes('disabled')),
    ).toBe(false);
  });

  it('should allow to change permissions for non-GitLab Project', async () => {
    const user = userEvent.setup();
    projectHandler.reset();
    projectHandler.setProjects([mockProject({ key: 'my-project', managed: false })]);
    almHandler.reset();
    almHandler.handleSetProjectBinding(AlmKeys.BitbucketServer, {
      almSetting: 'test',
      repository: 'test',
      monorepo: false,
      project: 'my-project',
    });

    const ui = getPageObject(user);

    renderPermissionsProjectApp({}, { featureList: [Feature.GitlabProvisioning] });
    await ui.appLoaded();

    expect(ui.pageTitle.get()).toBeInTheDocument();
    expect(ui.pageTitle.byRole('img').query()).not.toBeInTheDocument();

    expect(ui.applyTemplateBtn.get()).toBeInTheDocument();

    // no restrictions
    expect(
      screen
        .getAllByRole('checkbox')
        .every((item) => item.getAttributeNames().includes('disabled')),
    ).toBe(false);
  });
});

function renderPermissionsProjectApp(
  override: Partial<Component> = {},
  contextOverride: Partial<RenderContext> = {},
  componentContextOverride: Partial<ComponentContextShape> = {},
) {
  return renderAppWithComponentContext(
    'project_roles?id=my-project',
    projectPermissionsRoutes,
    contextOverride,
    {
      component: mockComponent({
        visibility: Visibility.Public,
        configuration: {
          canUpdateProjectVisibilityToPrivate: true,
          canApplyPermissionTemplate: true,
        },
        ...override,
      }),
      ...componentContextOverride,
    },
  );
}
