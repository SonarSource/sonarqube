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
import { byPlaceholderText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import DopTranslationServiceMock from '../../../api/mocks/DopTranslationServiceMock';
import GithubProvisioningServiceMock from '../../../api/mocks/GithubProvisioningServiceMock';
import GitlabProvisioningServiceMock from '../../../api/mocks/GitlabProvisioningServiceMock';
import PermissionsServiceMock from '../../../api/mocks/PermissionsServiceMock';
import ProjectManagementServiceMock from '../../../api/mocks/ProjectsManagementServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { getComponentNavigation } from '../../../api/navigation';
import { mockGitlabConfiguration } from '../../../helpers/mocks/alm-integrations';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockGitHubConfiguration } from '../../../helpers/mocks/dop-translation';
import { mockProject } from '../../../helpers/mocks/projects';
import { mockAppState, mockCurrentUser } from '../../../helpers/testMocks';
import { RenderContext, renderAppWithAdminContext } from '../../../helpers/testReactTestingUtils';
import { AppState } from '../../../types/appstate';
import { Feature } from '../../../types/features';
import { Permissions } from '../../../types/permissions';
import { ProvisioningType } from '../../../types/provisioning';
import { GlobalSettingKeys } from '../../../types/settings';
import { LoggedInUser } from '../../../types/users';
import routes from '../routes';

let login: string;

const permissionsHandler = new PermissionsServiceMock();
const settingsHandler = new SettingsServiceMock();
const dopTranslationHandler = new DopTranslationServiceMock();
const githubHandler = new GithubProvisioningServiceMock(dopTranslationHandler);
const gitlabHandler = new GitlabProvisioningServiceMock();
const handler = new ProjectManagementServiceMock(settingsHandler);

jest.mock('../../../api/navigation', () => ({ getComponentNavigation: jest.fn() }));

beforeAll(() => {
  jest.mocked(getComponentNavigation).mockImplementation(async ({ component }) => {
    const canBrowseProjectResponse = await permissionsHandler.handleGetPermissionUsersForComponent({
      projectKey: component,
      q: login,
      permission: Permissions.Browse,
    });
    const showPermissionsResponse = await permissionsHandler.handleGetPermissionUsersForComponent({
      projectKey: component,
      q: login,
      permission: Permissions.Admin,
    });

    return Promise.resolve(
      mockComponent({
        configuration: {
          canBrowseProject: canBrowseProjectResponse.users.length > 0,
          showPermissions: showPermissionsResponse.users.length > 0,
        },
      }),
    );
  });
});

const ui = {
  pageDescription: byText('projects_management.page.description'),
  row: byRole('row'),
  firstProjectActions: byRole('button', {
    name: 'projects_management.show_actions_for_x.Project 1',
  }),
  projectActions: (projectName: string) =>
    byRole('button', { name: `projects_management.show_actions_for_x.${projectName}` }),
  editPermissions: byRole('menuitem', { name: 'edit_permissions' }),
  showPermissions: byRole('menuitem', { name: 'show_permissions' }),
  applyPermissionTemplate: byRole('menuitem', { name: 'projects_role.apply_template' }),
  restoreAccess: byRole('menuitem', { name: 'global_permissions.restore_access' }),
  noActionsAvailable: byRole('menuitem', { name: 'global_permissions.no_actions_available' }),
  editPermissionsPage: byText('/project_roles?id=project1'),

  apply: byRole('button', { name: 'apply' }),
  cancel: byRole('button', { name: 'cancel' }),
  delete: byRole('button', { name: 'delete' }),
  create: byRole('button', { name: 'create' }),
  close: byRole('button', { name: 'close' }),
  restore: byRole('button', { name: 'restore' }),
  checkbox: (projectName: string) =>
    byRole('checkbox', { name: `projects_management.select_project.${projectName}` }),
  deleteProjects: byRole('button', {
    name: /permission_templates.(select_to_delete|delete_selected)/,
  }),
  showMore: byRole('button', { name: 'show_more' }),
  checkAll: byRole('checkbox', { name: 'check_all' }),
  uncheckAll: byRole('checkbox', { name: 'uncheck_all' }),
  bulkApplyButton: byRole('button', {
    name: 'permission_templates.bulk_apply_permission_template',
  }),
  createProject: byRole('button', {
    name: 'qualifiers.create.TRK',
  }),
  manualProjectHeader: byText('onboarding.create_project.manual.title'),
  displayNameField: byRole('textbox', {
    name: /onboarding.create_project.display_name/,
  }),
  projectNextButton: byRole('button', { name: 'next' }),
  newCodeDefinitionHeader: byText('onboarding.create_x_project.new_code_definition.title1'),
  projectCreateButton: byRole('button', {
    name: 'onboarding.create_project.new_code_definition.create_x_projects1',
  }),
  visibilityFilter: byRole('combobox', { name: 'projects_management.filter_by_visibility' }),
  qualifierFilter: byRole('combobox', { name: 'projects_management.filter_by_component' }),
  analysisDateFilter: byPlaceholderText('last_analysis_before'),
  provisionedFilter: byRole('checkbox', {
    name: 'provisioning.only_provisioned',
  }),
  searchFilter: byRole('searchbox', { name: 'search.search_by_name_or_key' }),

  defaultVisibility: byText('settings.projects.default_visibility_of_new_projects'),

  createDialog: byRole('dialog', { name: 'qualifiers.create.TRK' }),
  displayNameInput: byRole('textbox', {
    name: 'onboarding.create_project.display_name field_required',
  }),
  projectKeyInput: byRole('textbox', {
    name: 'onboarding.create_project.project_key field_required',
  }),
  mainBranchNameInput: byRole('textbox', {
    name: 'onboarding.create_project.main_branch_name field_required',
  }),
  privateVisibility: byRole('radio', { name: 'visibility.private' }),
  successMsg: byText('projects_management.project_has_been_successfully_created'),

  bulkApplyDialog: byRole('dialog', {
    name: 'permission_templates.bulk_apply_permission_template',
  }),
  applyTemplateDialog: byRole('dialog', {
    name: 'projects_role.apply_template_to_x.Project 1',
  }),
  selectTemplate: (required: string) => byRole('combobox', { name: `template ${required}` }),

  deleteDialog: byRole('dialog', { name: 'qualifiers.delete.TRK' }),

  changeDefaultVisibilityDialog: byRole('dialog', {
    name: 'settings.projects.change_visibility_form.header',
  }),
  editDefaultVisibility: byRole('button', {
    name: 'settings.projects.change_visibility_form.label',
  }),
  visibilityPublicRadio: byRole('radio', {
    name: 'visibility.public visibility.public.description.short',
  }),
  defaultVisibilityWarning: byText(/settings.projects.change_visibility_form.warning/),
  submitDefaultVisibilityChange: byRole('button', {
    name: 'settings.projects.change_visibility_form.submit',
  }),

  restoreAccessDialog: byRole('dialog', {
    name: 'global_permissions.restore_access',
  }),
};

beforeEach(() => {
  jest.useFakeTimers({
    advanceTimers: true,
    now: new Date('2019-01-05T07:08:59Z'),
  });
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();

  permissionsHandler.reset();
  settingsHandler.reset();
  dopTranslationHandler.reset();
  githubHandler.reset();
  gitlabHandler.reset();
  handler.reset();
});

it('should filter projects', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));

  await user.click(ui.visibilityFilter.get());
  await user.click(byRole('option', { name: 'visibility.public' }).get());

  await waitFor(() => expect(ui.row.getAll()).toHaveLength(4));
  await user.click(ui.analysisDateFilter.get());
  await user.click(await screen.findByRole('gridcell', { name: '5' }));

  await waitFor(() => {
    expect(ui.row.getAll()).toHaveLength(3);
  });

  await user.click(ui.provisionedFilter.get());

  await waitFor(() => {
    expect(ui.row.getAll()).toHaveLength(2);
  });

  expect(ui.row.getAll()[1]).toHaveTextContent('Project 4');

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.VW' }).get());

  await waitFor(() => expect(ui.provisionedFilter.query()).not.toBeInTheDocument());
  expect(ui.row.getAll()).toHaveLength(2);
  await waitFor(() => expect(ui.row.getAll()[1]).toHaveTextContent('Portfolio 1'));

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.APP' }).get());

  expect(ui.provisionedFilter.query()).not.toBeInTheDocument();
  expect(ui.row.getAll()).toHaveLength(2);
  await waitFor(() => expect(ui.row.getAll()[1]).toHaveTextContent('Application 1'));
});

it('should search by text', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  await user.type(ui.searchFilter.get(), 'provision');
  expect(ui.row.getAll()).toHaveLength(2);

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.VW' }).get());

  await waitFor(() => expect(ui.row.getAll()).toHaveLength(4));
  expect(ui.searchFilter.get()).toHaveValue('');
  await user.type(ui.searchFilter.get(), 'Portfolio 2');
  expect(ui.row.getAll()).toHaveLength(2);

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.APP' }).get());

  await waitFor(() => expect(ui.row.getAll()).toHaveLength(4));
  expect(ui.searchFilter.get()).toHaveValue('');
  await user.type(ui.searchFilter.get(), 'Application 3');
  expect(ui.row.getAll()).toHaveLength(2);
});

it('should hide qualifier filter', async () => {
  renderProjectManagementApp({ qualifiers: [ComponentQualifier.Project] });
  // pretext to wait for loading
  expect(await ui.pageDescription.find()).toBeInTheDocument();
  expect(ui.qualifierFilter.query()).not.toBeInTheDocument();
});

it('should hide create Project button', async () => {
  renderProjectManagementApp();
  // pretext to wait for loading
  expect(await ui.pageDescription.find()).toBeInTheDocument();
  expect(ui.createProject.query()).not.toBeInTheDocument();
});

it('should delete projects, but not Portfolios or Applications', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  expect(await ui.deleteProjects.find()).toBeDisabled();

  await waitFor(() => {
    expect(ui.row.getAll()).toHaveLength(5);
  });

  await user.click(ui.checkbox('Project 1').get());
  await user.click(ui.checkbox('Project 2').get());

  expect(ui.deleteProjects.get()).toBeEnabled();
  await user.click(ui.deleteProjects.get());
  expect(ui.deleteDialog.get()).toBeInTheDocument();
  expect(
    ui.deleteDialog.byText('projects_management.delete_selected_warning.2').get(),
  ).toBeInTheDocument();
  await user.click(ui.deleteDialog.by(ui.delete).get());
  expect(ui.row.getAll()).toHaveLength(3);
});

describe('Bulk permission templates', () => {
  it('should be applied to local projects', async () => {
    const user = userEvent.setup();
    handler.setProjects(
      Array.from({ length: 11 }, (_, i) => mockProject({ key: i.toString(), name: `Test ${i}` })),
    );
    renderProjectManagementApp();

    expect(await ui.bulkApplyButton.find()).toBeDisabled();

    await waitFor(() => {
      const projects = ui.row.getAll().slice(1);
      expect(projects).toHaveLength(11);
    });

    await user.click(ui.checkAll.get());
    expect(ui.bulkApplyButton.get()).toBeEnabled();

    await user.click(ui.bulkApplyButton.get());
    expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
    expect(
      await ui.bulkApplyDialog
        .byText('permission_templates.bulk_apply_permission_template.apply_to_selected.11')
        .find(),
    ).toBeInTheDocument();

    await user.click(ui.bulkApplyDialog.by(ui.apply).get());
    expect(
      await screen.findByText('bulk apply permission template error message'),
    ).toBeInTheDocument();
    expect(ui.bulkApplyDialog.get()).toBeInTheDocument();

    await user.click(ui.bulkApplyDialog.by(ui.cancel).get());

    await user.click(ui.uncheckAll.get());
    await user.click(ui.checkbox('Test 7').get());
    await user.click(ui.checkbox('Test 8').get());
    await user.click(ui.checkbox('Test 9').get());
    await user.click(ui.checkbox('Test 8').get()); // uncheck one
    await user.click(ui.bulkApplyButton.get());

    expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
    expect(
      await ui.bulkApplyDialog
        .byText('permission_templates.bulk_apply_permission_template.apply_to_selected.2')
        .find(),
    ).toBeInTheDocument();

    await user.click(ui.bulkApplyDialog.by(ui.selectTemplate('required')).get());
    await user.click(byRole('option', { name: 'Permission Template 2' }).get());

    await user.click(ui.bulkApplyDialog.by(ui.apply).get());

    expect(
      await within(ui.bulkApplyDialog.get()).findByText('projects_role.apply_template.success'),
    ).toBeInTheDocument();
  });

  it('should not be applied to managed GitHub projects', async () => {
    const user = userEvent.setup();
    dopTranslationHandler.gitHubConfigurations.push(
      mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
    );
    handler.setProjects(
      Array.from({ length: 11 }, (_, i) =>
        mockProject({ key: i.toString(), name: `Test ${i}`, managed: true }),
      ),
    );
    renderProjectManagementApp({}, {}, { featureList: [Feature.GithubProvisioning] });

    expect(await ui.bulkApplyButton.find()).toBeDisabled();

    await waitFor(() => {
      const projects = ui.row.getAll().slice(1);
      expect(projects).toHaveLength(11);
    });

    await user.click(ui.checkAll.get());
    expect(ui.bulkApplyButton.get()).toBeEnabled();

    await user.click(ui.bulkApplyButton.get());
    expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
    expect(
      await within(ui.bulkApplyDialog.get()).findByText(
        'permission_templates.bulk_apply_permission_template.apply_to_only_managed_projects.alm.github',
      ),
    ).toBeInTheDocument();
    expect(ui.bulkApplyDialog.by(ui.apply).get()).toBeDisabled();
  });

  it('should not be applied to managed GitLab projects', async () => {
    const user = userEvent.setup();
    handler.setProjects(
      Array.from({ length: 11 }, (_, i) =>
        mockProject({ key: i.toString(), name: `Test ${i}`, managed: true }),
      ),
    );

    gitlabHandler.setGitlabConfigurations([
      mockGitlabConfiguration({ id: '1', enabled: true, provisioningType: ProvisioningType.auto }),
    ]);
    renderProjectManagementApp({}, {}, { featureList: [Feature.GitlabProvisioning] });

    expect(await ui.bulkApplyButton.find()).toBeDisabled();

    await waitFor(() => {
      const projects = ui.row.getAll().slice(1);
      expect(projects).toHaveLength(11);
    });

    await user.click(ui.checkAll.get());
    expect(ui.bulkApplyButton.get()).toBeEnabled();

    await user.click(ui.bulkApplyButton.get());
    expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
    expect(
      await within(ui.bulkApplyDialog.get()).findByText(
        'permission_templates.bulk_apply_permission_template.apply_to_only_managed_projects.alm.gitlab',
      ),
    ).toBeInTheDocument();
    expect(ui.bulkApplyDialog.by(ui.apply).get()).toBeDisabled();
  });

  it('should not be applied to managed GitLab projects but to local project', async () => {
    const user = userEvent.setup();
    const allProjects = [
      ...Array.from({ length: 6 }, (_, i) =>
        mockProject({ key: `${i.toString()} managed`, name: `Test managed ${i}`, managed: true }),
      ),
      ...Array.from({ length: 5 }, (_, i) =>
        mockProject({ key: `${i.toString()} local`, name: `Test local ${i}`, managed: false }),
      ),
    ];

    handler.setProjects(allProjects);
    gitlabHandler.setGitlabConfigurations([
      mockGitlabConfiguration({ id: '1', enabled: true, provisioningType: ProvisioningType.auto }),
    ]);
    renderProjectManagementApp({}, {}, { featureList: [Feature.GitlabProvisioning] });

    expect(await ui.bulkApplyButton.find()).toBeDisabled();

    await waitFor(() => {
      const projects = ui.row.getAll().slice(1);
      expect(projects).toHaveLength(11);
    });

    await user.click(ui.checkAll.get());
    expect(ui.bulkApplyButton.get()).toBeEnabled();

    await user.click(ui.bulkApplyButton.get());
    expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
    expect(
      await within(ui.bulkApplyDialog.get()).findByText(
        /permission_templates.bulk_apply_permission_template.apply_to_selected.5/,
      ),
    ).toBeInTheDocument();
    expect(
      within(ui.bulkApplyDialog.get()).getByText(
        /permission_templates.bulk_apply_permission_template.apply_to_managed_projects.6.alm.gitlab/,
      ),
    ).toBeInTheDocument();
    expect(ui.bulkApplyDialog.by(ui.apply).get()).toBeEnabled();
  });
});

it('should not be applied to managed GitHub projects but to local project', async () => {
  const user = userEvent.setup();
  const allProjects = [
    ...Array.from({ length: 6 }, (_, i) =>
      mockProject({ key: `${i.toString()} managed`, name: `Test managed ${i}`, managed: true }),
    ),
    ...Array.from({ length: 5 }, (_, i) =>
      mockProject({ key: `${i.toString()} local`, name: `Test local ${i}`, managed: false }),
    ),
  ];

  handler.setProjects(allProjects);
  dopTranslationHandler.gitHubConfigurations.push(
    mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
  );
  renderProjectManagementApp({}, {}, { featureList: [Feature.GithubProvisioning] });

  expect(await ui.bulkApplyButton.find()).toBeDisabled();

  await waitFor(() => {
    const projects = ui.row.getAll().slice(1);
    expect(projects).toHaveLength(11);
  });
  await user.click(ui.checkAll.get());
  expect(ui.bulkApplyButton.get()).toBeEnabled();

  await user.click(ui.bulkApplyButton.get());
  expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
  expect(
    await within(ui.bulkApplyDialog.get()).findByText(
      /permission_templates.bulk_apply_permission_template.apply_to_selected.5/,
    ),
  ).toBeInTheDocument();
  expect(
    within(ui.bulkApplyDialog.get()).getByText(
      /permission_templates.bulk_apply_permission_template.apply_to_managed_projects.6.alm.github/,
    ),
  ).toBeInTheDocument();
  expect(ui.bulkApplyDialog.by(ui.apply).get()).toBeEnabled();
});

it('should load more and change the filter without caching old pages', async () => {
  const user = userEvent.setup();
  handler.setProjects([
    ...Array.from({ length: 60 }, (_, i) =>
      mockProject({
        key: `${ComponentQualifier.Project}${i.toString()}`,
        name: `Project ${i}`,
        qualifier: ComponentQualifier.Project,
      }),
    ),
    ...Array.from({ length: 60 }, (_, i) =>
      mockProject({
        key: `${ComponentQualifier.Portfolio}${i.toString()}`,
        name: `Portfolio ${i}`,
        qualifier: ComponentQualifier.Portfolio,
      }),
    ),
    ...Array.from({ length: 60 }, (_, i) =>
      mockProject({
        key: `${ComponentQualifier.Application}${i.toString()}`,
        name: `Application ${i}`,
        qualifier: ComponentQualifier.Application,
      }),
    ),
  ]);
  renderProjectManagementApp();

  await waitFor(() => expect(ui.row.getAll()).toHaveLength(51));
  await user.click(ui.showMore.get());

  // WaitFor to wait for the rerender
  await waitFor(() => {
    expect(ui.row.getAll()).toHaveLength(61);
  });
  const projectRows = ui.row.getAll();

  expect(projectRows[1]).toHaveTextContent('Project 0');
  expect(projectRows[60]).toHaveTextContent('Project 59');

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.VW' }).get());

  await waitFor(() => expect(projectRows[1]).not.toBeInTheDocument());
  const portfolioRows = ui.row.getAll();
  expect(portfolioRows).toHaveLength(51);
  expect(portfolioRows[1]).toHaveTextContent('Portfolio 0');

  await user.click(ui.showMore.get());

  // WaitFor to wait for the rerender
  await waitFor(() => {
    expect(ui.row.getAll()).toHaveLength(61);
  });
  const allPortfolioRows = ui.row.getAll();
  expect(allPortfolioRows[1]).toHaveTextContent('Portfolio 0');
  expect(allPortfolioRows[60]).toHaveTextContent('Portfolio 59');
});

it('should create project', async () => {
  const user = userEvent.setup();
  settingsHandler.set(GlobalSettingKeys.MainBranchName, 'main');
  renderProjectManagementApp({}, { permissions: { global: [Permissions.ProjectCreation] } });
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));

  await user.click(ui.createProject.get());

  expect(byText('/projects/create?mode=manual').get()).toBeInTheDocument();
});

it('should edit permissions of single project', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await user.click(await ui.firstProjectActions.find());
  expect(ui.editPermissions.get()).toBeInTheDocument();
  expect(ui.restoreAccess.query()).not.toBeInTheDocument();
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  await user.click(ui.editPermissions.get());

  expect(await ui.editPermissionsPage.find()).toBeInTheDocument();
});

it('should apply template for single object', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await user.click(await ui.firstProjectActions.find());
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  await user.click(ui.applyPermissionTemplate.get());

  expect(ui.applyTemplateDialog.get()).toBeInTheDocument();

  await user.click(await ui.applyTemplateDialog.by(ui.selectTemplate('required')).find());
  await user.click(byRole('option', { name: 'Permission Template 2' }).get());

  await user.click(ui.applyTemplateDialog.by(ui.apply).get());

  expect(
    await ui.applyTemplateDialog.byText('projects_role.apply_template.success').find(),
  ).toBeInTheDocument();
});

it('should restore access to admin', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp({}, { login: 'gooduser2', local: true });
  await user.click(await ui.firstProjectActions.find());
  expect(await ui.restoreAccess.find()).toBeInTheDocument();
  expect(ui.editPermissions.query()).not.toBeInTheDocument();
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  await user.click(ui.restoreAccess.get());
  expect(ui.restoreAccessDialog.get()).toBeInTheDocument();
  await user.click(ui.restoreAccessDialog.by(ui.restore).get());
  expect(ui.restoreAccessDialog.query()).not.toBeInTheDocument();
  await user.click(await ui.firstProjectActions.find());
  expect(ui.restoreAccess.query()).not.toBeInTheDocument();
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  expect(ui.editPermissions.get()).toBeInTheDocument();
});

it('should restore access for github project', async () => {
  const user = userEvent.setup();
  dopTranslationHandler.gitHubConfigurations.push(
    mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
  );
  renderProjectManagementApp(
    {},
    { login: 'gooduser2', local: true },
    { featureList: [Feature.GithubProvisioning] },
  );
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  await user.click(await ui.projectActions('Project 4').find());
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  expect(await ui.restoreAccess.find()).toBeInTheDocument();
  expect(ui.showPermissions.query()).not.toBeInTheDocument();
  await user.click(ui.restoreAccess.get());
  expect(ui.restoreAccessDialog.get()).toBeInTheDocument();
  await user.click(ui.restoreAccessDialog.by(ui.restore).get());
  expect(ui.restoreAccessDialog.query()).not.toBeInTheDocument();
  await user.click(await ui.projectActions('Project 4').find());
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  expect(ui.restoreAccess.query()).not.toBeInTheDocument();
  expect(ui.showPermissions.get()).toBeInTheDocument();
});

it('should not allow to restore access on github project for GH user', async () => {
  const user = userEvent.setup();
  dopTranslationHandler.gitHubConfigurations.push(
    mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
  );
  renderProjectManagementApp(
    {},
    { login: 'gooduser2', local: false },
    { featureList: [Feature.GithubProvisioning] },
  );
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  await user.click(await ui.projectActions('Project 4').find());
  expect(await ui.noActionsAvailable.find()).toBeInTheDocument();
  expect(ui.restoreAccess.query()).not.toBeInTheDocument();
  await user.click(await ui.projectActions('Project 1').find());
  expect(await ui.restoreAccess.find()).toBeInTheDocument();
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
});

it('should show github warning on changing default visibility to admin', async () => {
  const user = userEvent.setup();
  dopTranslationHandler.gitHubConfigurations.push(
    mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
  );
  renderProjectManagementApp({}, {}, { featureList: [Feature.GithubProvisioning] });
  await user.click(ui.editDefaultVisibility.get());
  expect(await ui.changeDefaultVisibilityDialog.find()).toBeInTheDocument();
  expect(ui.defaultVisibilityWarning.get()).toHaveTextContent('.github');
});

it('should not apply permissions for github projects', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  expect(ui.checkbox('Project 4').get()).not.toHaveAttribute('aria-disabled');
  expect(ui.checkbox('Project 1').get()).not.toHaveAttribute('aria-disabled');
  await user.click(ui.checkAll.get());
  expect(ui.checkbox('Project 4').get()).toBeChecked();
  expect(ui.checkbox('Project 1').get()).toBeChecked();
  await user.click(ui.projectActions('Project 4').get());
  expect(ui.applyPermissionTemplate.query()).not.toBeInTheDocument();
  expect(ui.editPermissions.query()).not.toBeInTheDocument();
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  expect(ui.showPermissions.get()).toBeInTheDocument();
  await user.click(ui.projectActions('Project 1').get());
  expect(await ui.editPermissions.find()).toBeInTheDocument();
  expect(ui.applyPermissionTemplate.get()).toBeInTheDocument();
  expect(ui.noActionsAvailable.query()).not.toBeInTheDocument();
  expect(ui.showPermissions.query()).not.toBeInTheDocument();
});

it('should not show local badge for applications and portfolios', async () => {
  const user = userEvent.setup();
  dopTranslationHandler.gitHubConfigurations.push(
    mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
  );
  renderProjectManagementApp({}, {}, { featureList: [Feature.GithubProvisioning] });
  await waitFor(() => expect(screen.getAllByText('local')).toHaveLength(3));

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.VW' }).get());

  await waitFor(() => expect(screen.queryByText('local')).not.toBeInTheDocument());

  await user.click(ui.qualifierFilter.get());
  await user.click(byRole('option', { name: 'qualifiers.APP' }).get());

  expect(screen.queryByText('local')).not.toBeInTheDocument();
});

it('should not show local badge if provisioning is not enabled', async () => {
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  expect(screen.queryByText('local')).not.toBeInTheDocument();
});

it('should display no action available for managed project if autoprovisioning is enabled for non admins', async () => {
  const user = userEvent.setup();
  dopTranslationHandler.gitHubConfigurations.push(
    mockGitHubConfiguration({ provisioningType: ProvisioningType.auto }),
  );
  handler.setProjects([
    mockProject({
      key: `${ComponentQualifier.Project}${1}`,
      name: 'Project 1',
      qualifier: ComponentQualifier.Project,
      managed: true,
    }),
  ]);
  renderProjectManagementApp(
    {},
    { login: 'gooduser2', local: false },
    { featureList: [Feature.GithubProvisioning] },
  );
  await user.click(await ui.firstProjectActions.find());
  expect(ui.noActionsAvailable.get()).toBeInTheDocument();
});

function renderProjectManagementApp(
  overrides: Partial<AppState> = {},
  user: Partial<LoggedInUser> = {},
  context: Partial<RenderContext> = {},
) {
  login = user?.login ?? 'gooduser1';
  renderAppWithAdminContext('admin/projects_management', routes, {
    appState: mockAppState({
      qualifiers: [
        ComponentQualifier.Project,
        ComponentQualifier.Application,
        ComponentQualifier.Portfolio,
      ],
      ...overrides,
    }),
    currentUser: mockCurrentUser(user),
    ...context,
  });
}
