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
import selectEvent from 'react-select-event';
import PermissionsServiceMock from '../../../api/mocks/PermissionsServiceMock';
import ProjectManagementServiceMock from '../../../api/mocks/ProjectsManagementServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockProject } from '../../../helpers/mocks/projects';
import { mockAppState, mockCurrentUser } from '../../../helpers/testMocks';
import { renderAppWithAdminContext } from '../../../helpers/testReactTestingUtils';
import { byPlaceholderText, byRole, byText } from '../../../helpers/testSelector';
import { AppState } from '../../../types/appstate';
import { ComponentQualifier } from '../../../types/component';
import { Permissions } from '../../../types/permissions';
import { GlobalSettingKeys } from '../../../types/settings';
import { LoggedInUser } from '../../../types/users';
import routes from '../routes';

let login: string;

const permissionsHandler = new PermissionsServiceMock();
const settingsHandler = new SettingsServiceMock();
const handler = new ProjectManagementServiceMock(settingsHandler);

jest.mock('../../../api/navigation', () => ({
  getComponentNavigation: jest.fn().mockImplementation(async ({ component }) => {
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
      })
    );
  }),
}));

const ui = {
  row: byRole('row'),
  firstProjectActions: byRole('button', {
    name: 'projects_management.show_actions_for_x.Project 1',
  }),
  editPermissions: byRole('link', { name: 'edit_permissions' }),
  applyPermissionTemplate: byRole('button', { name: 'projects_role.apply_template' }),
  restoreAccess: byRole('button', { name: 'global_permissions.restore_access' }),
  editPermissionsPage: byText('/project_roles?id=project1'),

  apply: byRole('button', { name: 'apply' }),
  cancel: byRole('button', { name: 'cancel' }),
  delete: byRole('button', { name: 'delete' }),
  create: byRole('button', { name: 'create' }),
  close: byRole('button', { name: 'close' }),
  restore: byRole('button', { name: 'restore' }),
  checkbox: byRole('checkbox'),
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

  visibilityFilter: byRole('combobox', { name: 'projects_management.filter_by_visibility' }),
  qualifierFilter: byRole('combobox', { name: 'projects_management.filter_by_component' }),
  analysisDateFilter: byPlaceholderText('last_analysis_before'),
  provisionedFilter: byRole('checkbox', {
    name: 'provisioning.only_provisioned provisioning.only_provisioned.tooltip',
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
  selectTemplate: byRole('combobox', { name: 'template field_required' }),

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
  submitDefaultVisibilityChange: byRole('button', {
    name: 'settings.projects.change_visibility_form.submit',
  }),

  restoreAccessDialog: byRole('dialog', {
    name: 'global_permissions.restore_access',
  }),
};

beforeAll(() => {
  jest.useFakeTimers({
    advanceTimers: true,
    now: new Date('2019-01-05T07:08:59Z'),
  });
});

afterEach(() => {
  permissionsHandler.reset();
  settingsHandler.reset();
  handler.reset();
});

it('should filter projects', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  await selectEvent.select(ui.visibilityFilter.get(), 'visibility.public');
  expect(ui.row.getAll()).toHaveLength(4);
  await user.click(ui.analysisDateFilter.get());
  await user.click(await screen.findByRole('gridcell', { name: '5' }));
  expect(ui.row.getAll()).toHaveLength(3);
  await user.click(ui.provisionedFilter.get());
  expect(ui.row.getAll()).toHaveLength(2);
  expect(ui.row.getAll()[1]).toHaveTextContent('Project 4');
  await selectEvent.select(ui.qualifierFilter.get(), 'qualifiers.VW');
  expect(ui.provisionedFilter.query()).not.toBeInTheDocument();
  expect(ui.row.getAll()).toHaveLength(2);
  expect(ui.row.getAll()[1]).toHaveTextContent('Portfolio 1');
  await selectEvent.select(ui.qualifierFilter.get(), 'qualifiers.APP');
  expect(ui.provisionedFilter.query()).not.toBeInTheDocument();
  expect(ui.row.getAll()).toHaveLength(2);
  expect(ui.row.getAll()[1]).toHaveTextContent('Application 1');
});

it('should search by text', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  await user.type(ui.searchFilter.get(), 'provision');
  expect(ui.row.getAll()).toHaveLength(2);
  await selectEvent.select(ui.qualifierFilter.get(), 'qualifiers.VW');
  expect(ui.row.getAll()).toHaveLength(4);
  expect(ui.searchFilter.get()).toHaveValue('');
  await user.type(ui.searchFilter.get(), 'Portfolio 2');
  expect(ui.row.getAll()).toHaveLength(2);
  await selectEvent.select(ui.qualifierFilter.get(), 'qualifiers.APP');
  expect(ui.row.getAll()).toHaveLength(4);
  expect(ui.searchFilter.get()).toHaveValue('');
  await user.type(ui.searchFilter.get(), 'Application 3');
  expect(ui.row.getAll()).toHaveLength(2);
});

it('should hide quilifier filter', () => {
  renderProjectManagementApp({ qualifiers: [ComponentQualifier.Project] });
  expect(ui.qualifierFilter.query()).not.toBeInTheDocument();
});

it('should hide create Project button', () => {
  renderProjectManagementApp();
  expect(ui.createProject.query()).not.toBeInTheDocument();
});

it('should delete projects, but not Portfolios or Applications', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  expect(await ui.deleteProjects.find()).toBeDisabled();
  expect(ui.row.getAll()).toHaveLength(5);
  await user.click(ui.checkbox.get(ui.row.getAll()[1]));
  await user.click(ui.checkbox.get(ui.row.getAll()[2]));
  expect(ui.deleteProjects.get()).toBeEnabled();
  await user.click(ui.deleteProjects.get());
  expect(ui.deleteDialog.get()).toBeInTheDocument();
  expect(
    within(ui.deleteDialog.get()).getByText('projects_management.delete_selected_warning.2')
  ).toBeInTheDocument();
  await user.click(ui.delete.get(ui.deleteDialog.get()));
  expect(ui.row.getAll()).toHaveLength(3);
});

it('should bulk apply permission templates to projects', async () => {
  const user = userEvent.setup();
  handler.setProjects(
    Array.from({ length: 11 }, (_, i) => mockProject({ key: i.toString(), name: `Test ${i}` }))
  );
  renderProjectManagementApp();

  expect(await ui.bulkApplyButton.find()).toBeDisabled();
  const projects = ui.row.getAll().slice(1);
  expect(projects).toHaveLength(11);
  await user.click(ui.checkAll.get());
  expect(ui.bulkApplyButton.get()).toBeEnabled();

  await user.click(ui.bulkApplyButton.get());
  expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
  expect(
    within(ui.bulkApplyDialog.get()).getByText(
      'permission_templates.bulk_apply_permission_template.apply_to_selected.11'
    )
  ).toBeInTheDocument();

  await user.click(ui.apply.get(ui.bulkApplyDialog.get()));
  expect(
    await screen.findByText('bulk apply permission template error message')
  ).toBeInTheDocument();
  expect(ui.bulkApplyDialog.get()).toBeInTheDocument();

  await user.click(ui.cancel.get(ui.bulkApplyDialog.get()));

  await user.click(ui.uncheckAll.get());
  await user.click(ui.checkbox.get(projects[8]));
  await user.click(ui.checkbox.get(projects[9]));
  await user.click(ui.checkbox.get(projects[10]));
  await user.click(ui.checkbox.get(projects[9])); // uncheck one
  await user.click(ui.bulkApplyButton.get());

  expect(await ui.bulkApplyDialog.find()).toBeInTheDocument();
  expect(
    within(ui.bulkApplyDialog.get()).getByText(
      'permission_templates.bulk_apply_permission_template.apply_to_selected.2'
    )
  ).toBeInTheDocument();
  await selectEvent.select(
    ui.selectTemplate.get(ui.bulkApplyDialog.get()),
    'Permission Template 2'
  );
  await user.click(ui.apply.get(ui.bulkApplyDialog.get()));

  expect(
    await within(ui.bulkApplyDialog.get()).findByText('projects_role.apply_template.success')
  ).toBeInTheDocument();
});

it('should load more and change the filter without caching old pages', async () => {
  const user = userEvent.setup();
  handler.setProjects([
    ...Array.from({ length: 60 }, (_, i) =>
      mockProject({
        key: ComponentQualifier.Project + i.toString(),
        name: `Project ${i}`,
        qualifier: ComponentQualifier.Project,
      })
    ),
    ...Array.from({ length: 60 }, (_, i) =>
      mockProject({
        key: ComponentQualifier.Portfolio + i.toString(),
        name: `Portfolio ${i}`,
        qualifier: ComponentQualifier.Portfolio,
      })
    ),
    ...Array.from({ length: 60 }, (_, i) =>
      mockProject({
        key: ComponentQualifier.Application + i.toString(),
        name: `Application ${i}`,
        qualifier: ComponentQualifier.Application,
      })
    ),
  ]);
  renderProjectManagementApp();
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(51));
  await user.click(ui.showMore.get());
  let rows = ui.row.getAll();
  expect(rows).toHaveLength(61);
  expect(rows[1]).toHaveTextContent('Project 0');
  expect(rows[60]).toHaveTextContent('Project 59');
  await selectEvent.select(ui.qualifierFilter.get(), 'qualifiers.VW');
  rows = ui.row.getAll();
  expect(rows).toHaveLength(51);
  expect(rows[1]).toHaveTextContent('Portfolio 0');
  await user.click(ui.showMore.get());
  rows = ui.row.getAll();
  expect(rows).toHaveLength(61);
  expect(rows[1]).toHaveTextContent('Portfolio 0');
  expect(rows[60]).toHaveTextContent('Portfolio 59');
});

it('should create project', async () => {
  settingsHandler.set(GlobalSettingKeys.MainBranchName, 'main');
  const user = userEvent.setup();
  renderProjectManagementApp({}, { permissions: { global: [Permissions.ProjectCreation] } });
  await waitFor(() => expect(ui.row.getAll()).toHaveLength(5));
  await user.click(await ui.createProject.find());
  let dialog = ui.createDialog.get();
  expect(dialog).toBeInTheDocument();
  expect(ui.privateVisibility.get(dialog)).not.toBeChecked();
  await user.click(ui.privateVisibility.get(dialog));
  expect(ui.privateVisibility.get(dialog)).not.toBeChecked();
  await user.click(ui.cancel.get(dialog));

  expect(await ui.defaultVisibility.find()).toBeInTheDocument();
  expect(ui.defaultVisibility.get()).toHaveTextContent('—');
  await user.click(ui.editDefaultVisibility.get());
  expect(await ui.changeDefaultVisibilityDialog.find()).toBeInTheDocument();
  await user.click(ui.visibilityPublicRadio.get(ui.changeDefaultVisibilityDialog.get()));
  await user.click(ui.submitDefaultVisibilityChange.get(ui.changeDefaultVisibilityDialog.get()));
  expect(ui.changeDefaultVisibilityDialog.query()).not.toBeInTheDocument();
  expect(ui.defaultVisibility.get()).toHaveTextContent('visibility.public');

  await user.click(await ui.createProject.find());
  dialog = ui.createDialog.get();
  expect(dialog).toBeInTheDocument();
  await user.click(ui.privateVisibility.get(dialog));
  expect(ui.privateVisibility.get(dialog)).toBeChecked();
  await user.type(ui.displayNameInput.get(dialog), 'a Test');
  await user.type(ui.projectKeyInput.get(dialog), 'test');
  expect(ui.mainBranchNameInput.get(dialog)).toHaveValue('main');
  await user.click(ui.create.get(dialog));
  expect(ui.successMsg.get(dialog)).toBeInTheDocument();
  await user.click(ui.close.get(dialog));
  expect(ui.row.getAll()).toHaveLength(6);
  expect(ui.row.getAll()[1]).toHaveTextContent('qualifier.TRKa Testvisibility.privatetest—');
});

it('should edit permissions of single project', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await user.click(await ui.firstProjectActions.find());
  expect(ui.restoreAccess.query()).not.toBeInTheDocument();
  expect(ui.editPermissions.get()).toBeInTheDocument();
  await user.click(ui.editPermissions.get());

  expect(await ui.editPermissionsPage.find()).toBeInTheDocument();
});

it('should apply template for single object', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp();
  await user.click(await ui.firstProjectActions.find());
  await user.click(ui.applyPermissionTemplate.get());

  expect(ui.applyTemplateDialog.get()).toBeInTheDocument();
  await selectEvent.select(
    ui.selectTemplate.get(ui.applyTemplateDialog.get()),
    'Permission Template 2'
  );
  await user.click(ui.apply.get(ui.applyTemplateDialog.get()));

  expect(
    await within(ui.applyTemplateDialog.get()).findByText('projects_role.apply_template.success')
  ).toBeInTheDocument();
});

it('should restore access to admin', async () => {
  const user = userEvent.setup();
  renderProjectManagementApp({}, { login: 'gooduser2' });
  await user.click(await ui.firstProjectActions.find());
  expect(await ui.restoreAccess.find()).toBeInTheDocument();
  expect(ui.editPermissions.query()).not.toBeInTheDocument();
  await user.click(ui.restoreAccess.get());
  expect(ui.restoreAccessDialog.get()).toBeInTheDocument();
  await user.click(ui.restore.get(ui.restoreAccessDialog.get()));
  expect(ui.restoreAccessDialog.query()).not.toBeInTheDocument();
  await user.click(await ui.firstProjectActions.find());
  expect(ui.restoreAccess.query()).not.toBeInTheDocument();
  expect(ui.editPermissions.get()).toBeInTheDocument();
});

function renderProjectManagementApp(
  overrides: Partial<AppState> = {},
  user: Partial<LoggedInUser> = {}
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
  });
}
