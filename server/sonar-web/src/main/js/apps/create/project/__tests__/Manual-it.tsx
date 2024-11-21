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

import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import DopTranslationServiceMock from '../../../../api/mocks/DopTranslationServiceMock';
import { ModeServiceMock } from '../../../../api/mocks/ModeServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { ProjectsServiceMock } from '../../../../api/mocks/ProjectsServiceMock';
import { getNewCodeDefinition } from '../../../../api/newCodeDefinition';
import { mockProject } from '../../../../helpers/mocks/projects';
import { mockAppState, mockCurrentUser } from '../../../../helpers/testMocks';
import { renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import { Permissions } from '../../../../types/permissions';
import routes from '../../../projects/routes';

jest.mock('../../../../api/measures');
jest.mock('../../../../api/favorites');
jest.mock('../../../../api/alm-settings');
jest.mock('../../../../api/dop-translation');
jest.mock('../../../../api/newCodeDefinition');
jest.mock('../../../../api/project-management', () => ({
  createProject: jest.fn().mockReturnValue(Promise.resolve({ project: mockProject() })),
}));
jest.mock('../../../../api/components', () => ({
  ...jest.requireActual('../../../../api/components'),
  searchProjects: jest.fn(),
  getScannableProjects: jest.fn(),
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists')),
}));
jest.mock('../../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({ value: 'main' }),
}));

const ui = {
  manualCreateProjectOption: byText('onboarding.create_project.select_method.manual'),
  manualProjectHeader: byText('onboarding.create_project.manual.title'),
  displayNameField: byRole('textbox', {
    name: /onboarding.create_project.display_name/,
  }),
  projectNextButton: byRole('button', { name: 'next' }),
  newCodeDefinitionSection: byRole('region', {
    name: 'onboarding.create_project.new_code_definition.title',
  }),
  newCodeDefinitionHeader: byText('onboarding.create_x_project.new_code_definition.title1'),
  inheritGlobalNcdRadio: byRole('radio', { name: 'new_code_definition.global_setting' }),
  projectCreateButton: byRole('button', {
    name: 'onboarding.create_project.new_code_definition.create_x_projects1',
  }),
  cancelButton: byRole('button', { name: 'cancel' }),
  closeButton: byRole('button', { name: 'clear' }),
  createProjectsButton: byRole('button', { name: 'projects.add' }),
  createLocalProject: byRole('menuitem', { name: 'my_account.add_project.manual' }),
  overrideNcdRadio: byRole('radio', { name: 'new_code_definition.specific_setting' }),
  ncdOptionPreviousVersionRadio: byRole('radio', {
    name: /new_code_definition.previous_version/,
  }),
  ncdOptionRefBranchRadio: byRole('radio', {
    name: /new_code_definition.reference_branch/,
  }),
  ncdOptionDaysRadio: byRole('radio', {
    name: /new_code_definition.number_days/,
  }),
  ncdOptionDaysInput: byRole('spinbutton', {
    name: /new_code_definition.number_days.specify_days/,
  }),
  ncdOptionDaysInputError: byText('new_code_definition.number_days.invalid.1.90'),
  projectDashboardText: byText('/dashboard?id=foo'),
  projectsPageTitle: byRole('heading', { name: 'projects.page' }),
};

async function fillFormAndNext(displayName: string, user: UserEvent) {
  expect(await ui.manualProjectHeader.find()).toBeInTheDocument();

  await user.click(ui.displayNameField.get());
  await user.keyboard(displayName);

  expect(ui.projectNextButton.get()).toBeEnabled();
  await user.click(ui.projectNextButton.get());
}

let almSettingsHandler: AlmSettingsServiceMock;
let dopTranslationHandler: DopTranslationServiceMock;
let newCodePeriodHandler: NewCodeDefinitionServiceMock;
let projectHandler: ProjectsServiceMock;
let modeHandler: ModeServiceMock;

const original = window.location;

beforeAll(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { replace: jest.fn() },
  });
  almSettingsHandler = new AlmSettingsServiceMock();
  dopTranslationHandler = new DopTranslationServiceMock();
  newCodePeriodHandler = new NewCodeDefinitionServiceMock();
  projectHandler = new ProjectsServiceMock();
  modeHandler = new ModeServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almSettingsHandler.reset();
  dopTranslationHandler.reset();
  newCodePeriodHandler.reset();
  projectHandler.reset();
  modeHandler.reset();
});

afterAll(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: original });
});

it('should fill form and move to NCD selection', async () => {
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('test', user);

  expect(ui.newCodeDefinitionHeader.get()).toBeInTheDocument();
});

it('should select the global NCD when it is compliant', async () => {
  jest
    .mocked(getNewCodeDefinition)
    .mockResolvedValue({ type: NewCodeDefinitionType.NumberOfDays, value: '30' });
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('test', user);

  expect(ui.newCodeDefinitionHeader.get()).toBeInTheDocument();
  expect(ui.inheritGlobalNcdRadio.get()).toBeInTheDocument();
  expect(ui.inheritGlobalNcdRadio.get()).toBeEnabled();
  expect(ui.projectCreateButton.get()).toBeDisabled();

  await user.click(ui.inheritGlobalNcdRadio.get());

  expect(ui.projectCreateButton.get()).toBeEnabled();
});

it('number of days ignores non-numeric inputs', async () => {
  jest
    .mocked(getNewCodeDefinition)
    .mockResolvedValue({ type: NewCodeDefinitionType.NumberOfDays, value: '60' });
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('test', user);

  expect(ui.projectCreateButton.get()).toBeDisabled();
  expect(ui.overrideNcdRadio.get()).not.toHaveClass('disabled');
  expect(ui.ncdOptionDaysRadio.get()).toHaveClass('disabled');

  await user.click(ui.overrideNcdRadio.get());
  expect(ui.ncdOptionDaysRadio.get()).not.toHaveClass('disabled');

  await user.click(ui.ncdOptionDaysRadio.get());

  expect(ui.ncdOptionDaysInput.get()).toBeInTheDocument();
  expect(ui.ncdOptionDaysInput.get()).toHaveValue(60);
  expect(ui.projectCreateButton.get()).toBeEnabled();

  await user.click(ui.ncdOptionDaysInput.get());
  await user.keyboard('abc');

  // it ignores the input and preserves its value
  expect(ui.ncdOptionDaysInput.get()).toHaveValue(60);
});

it('the project onboarding page should be displayed when the project is created', async () => {
  newCodePeriodHandler.setNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays });
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('testing', user);

  await user.click(ui.overrideNcdRadio.get());

  expect(ui.projectCreateButton.get()).toBeEnabled();
  await user.click(ui.projectCreateButton.get());

  expect(await ui.projectDashboardText.find()).toBeInTheDocument();
});

it('validate the private key field', async () => {
  const user = userEvent.setup();
  renderCreateProject();
  expect(ui.manualProjectHeader.get()).toBeInTheDocument();

  await user.click(ui.displayNameField.get());
  await user.keyboard('exists');

  await waitFor(() => {
    expect(ui.projectNextButton.get()).toBeDisabled();
  });
  await user.click(ui.projectNextButton.get());
});

it('should navigate back to the Projects page when clicking cancel or close', async () => {
  newCodePeriodHandler.setNewCodePeriod({ type: NewCodeDefinitionType.NumberOfDays });
  const user = userEvent.setup();
  renderCreateProject();

  await user.click(await ui.cancelButton.find());
  expect(await ui.projectsPageTitle.find()).toBeInTheDocument();

  await user.click(ui.createProjectsButton.get());
  await user.click(await ui.createLocalProject.find());

  await user.click(ui.closeButton.get());
  expect(await ui.projectsPageTitle.find()).toBeInTheDocument();

  await user.click(ui.createProjectsButton.get());
  await user.click(await ui.createLocalProject.find());

  expect(await ui.manualProjectHeader.find()).toBeInTheDocument();
  await fillFormAndNext('testing', user);
  expect(ui.newCodeDefinitionHeader.get()).toBeInTheDocument();

  await user.click(await ui.newCodeDefinitionSection.byRole('button', { name: 'clear' }).find());
  expect(await ui.projectsPageTitle.find()).toBeInTheDocument();
});

function renderCreateProject() {
  renderAppRoutes('projects/create?mode=manual', routes, {
    currentUser: mockCurrentUser({
      permissions: { global: [Permissions.ProjectCreation] },
    }),
    appState: mockAppState({ canAdmin: true }),
  });
}
