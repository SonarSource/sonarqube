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
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import * as React from 'react';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import NewCodePeriodsServiceMock from '../../../../api/mocks/NewCodePeriodsServiceMock';
import { getNewCodePeriod } from '../../../../api/newCodePeriod';
import { mockProject } from '../../../../helpers/mocks/projects';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import { NewCodePeriodSettingType } from '../../../../types/types';
import CreateProjectPage, { CreateProjectPageProps } from '../CreateProjectPage';

jest.mock('../../../../api/alm-settings');
jest.mock('../../../../api/newCodePeriod');
jest.mock('../../../../api/project-management', () => ({
  setupManualProjectCreation: jest
    .fn()
    .mockReturnValue(() => Promise.resolve({ project: mockProject() })),
}));
jest.mock('../../../../api/components', () => ({
  ...jest.requireActual('../../../../api/components'),
  doesComponentExists: jest
    .fn()
    .mockImplementation(({ component }) => Promise.resolve(component === 'exists')),
}));
jest.mock('../../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({ value: 'main' }),
}));

const ui = {
  manualCreateProjectOption: byText('onboarding.create_project.select_method.manual'),
  manualProjectHeader: byText('onboarding.create_project.setup_manually'),
  displayNameField: byRole('textbox', {
    name: /onboarding.create_project.display_name/,
  }),
  projectNextButton: byRole('button', { name: 'next' }),
  newCodeDefinitionHeader: byText('onboarding.create_project.new_code_definition.title'),
  newCodeDefinitionBackButton: byRole('button', { name: 'back' }),
  inheritGlobalNcdRadio: byRole('radio', { name: 'new_code_definition.global_setting' }),
  projectCreateButton: byRole('button', {
    name: 'onboarding.create_project.new_code_definition.create_project',
  }),
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
  ncdOptionDaysInput: byRole('textbox', {
    name: /new_code_definition.number_days.specify_days/,
  }),
  ncdOptionDaysInputError: byText('new_code_definition.number_days.invalid.1.90'),
  ncdWarningTextAdmin: byText('new_code_definition.compliance.warning.explanation.admin'),
  ncdWarningText: byText('new_code_definition.compliance.warning.explanation'),
  projectDashboardText: byText('/dashboard?id=foo'),
};

async function fillFormAndNext(displayName: string, user: UserEvent) {
  await user.click(ui.manualCreateProjectOption.get());

  expect(ui.manualProjectHeader.get()).toBeInTheDocument();

  await user.click(ui.displayNameField.get());
  await user.keyboard(displayName);

  expect(ui.projectNextButton.get()).toBeEnabled();
  await user.click(ui.projectNextButton.get());
}

let almSettingsHandler: AlmSettingsServiceMock;
let newCodePeriodHandler: NewCodePeriodsServiceMock;

beforeAll(() => {
  almSettingsHandler = new AlmSettingsServiceMock();
  newCodePeriodHandler = new NewCodePeriodsServiceMock();
});

beforeEach(() => {
  jest.clearAllMocks();
  almSettingsHandler.reset();
  newCodePeriodHandler.reset();
});

it('should fill form and move to NCD selection and back', async () => {
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('test', user);

  expect(ui.newCodeDefinitionHeader.get()).toBeInTheDocument();

  expect(ui.newCodeDefinitionBackButton.get()).toBeInTheDocument();
  await user.click(ui.newCodeDefinitionBackButton.get());

  expect(ui.manualProjectHeader.get()).toBeInTheDocument();

  // TODO this must work at some point
  // expect(ui.displayNameField.get()).toHaveValue('test');
});

it('should select the global NCD when it is compliant', async () => {
  jest
    .mocked(getNewCodePeriod)
    .mockResolvedValue({ type: NewCodePeriodSettingType.NUMBER_OF_DAYS, value: '30' });
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

it('global NCD option should be disabled if not compliant', async () => {
  jest
    .mocked(getNewCodePeriod)
    .mockResolvedValue({ type: NewCodePeriodSettingType.NUMBER_OF_DAYS, value: '96' });
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('test', user);

  expect(ui.newCodeDefinitionHeader.get()).toBeInTheDocument();
  expect(ui.inheritGlobalNcdRadio.get()).toBeInTheDocument();
  expect(ui.inheritGlobalNcdRadio.get()).toBeDisabled();
  expect(ui.projectCreateButton.get()).toBeDisabled();
});

it.each([
  { canAdmin: true, message: ui.ncdWarningTextAdmin },
  { canAdmin: false, message: ui.ncdWarningText },
])(
  'should show warning message when global NCD is not compliant',
  async ({ canAdmin, message }) => {
    jest
      .mocked(getNewCodePeriod)
      .mockResolvedValue({ type: NewCodePeriodSettingType.NUMBER_OF_DAYS, value: '96' });
    const user = userEvent.setup();
    renderCreateProject({ appState: mockAppState({ canAdmin }) });
    await fillFormAndNext('test', user);

    expect(message.get()).toBeInTheDocument();
  }
);

it.each([ui.ncdOptionRefBranchRadio, ui.ncdOptionPreviousVersionRadio])(
  'should override the global NCD and pick a compliant NCD',
  async (option) => {
    jest
      .mocked(getNewCodePeriod)
      .mockResolvedValue({ type: NewCodePeriodSettingType.NUMBER_OF_DAYS, value: '96' });
    const user = userEvent.setup();
    renderCreateProject();
    await fillFormAndNext('test', user);

    expect(ui.newCodeDefinitionHeader.get()).toBeInTheDocument();
    expect(ui.inheritGlobalNcdRadio.get()).toBeInTheDocument();
    expect(ui.inheritGlobalNcdRadio.get()).toBeDisabled();
    expect(ui.projectCreateButton.get()).toBeDisabled();
    expect(ui.overrideNcdRadio.get()).toBeEnabled();
    expect(option.get()).toHaveClass('disabled');

    await user.click(ui.overrideNcdRadio.get());
    expect(option.get()).not.toHaveClass('disabled');

    await user.click(option.get());

    expect(ui.projectCreateButton.get()).toBeEnabled();
  }
);

it('number of days should show error message if value is not a number', async () => {
  jest
    .mocked(getNewCodePeriod)
    .mockResolvedValue({ type: NewCodePeriodSettingType.NUMBER_OF_DAYS, value: '60' });
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
  expect(ui.ncdOptionDaysInput.get()).toHaveValue('30');
  expect(ui.projectCreateButton.get()).toBeEnabled();

  await user.click(ui.ncdOptionDaysInput.get());
  await user.keyboard('abc');

  expect(ui.ncdOptionDaysInputError.get()).toBeInTheDocument();
  expect(ui.projectCreateButton.get()).toBeDisabled();

  await user.clear(ui.ncdOptionDaysInput.get());
  await user.click(ui.ncdOptionDaysInput.get());
  await user.keyboard('30');

  expect(ui.ncdOptionDaysInputError.query()).not.toBeInTheDocument();
  expect(ui.projectCreateButton.get()).toBeEnabled();
});

it('the project onboarding page should be displayed when the project is created', async () => {
  newCodePeriodHandler.setNewCodePeriod({ type: NewCodePeriodSettingType.NUMBER_OF_DAYS });
  const user = userEvent.setup();
  renderCreateProject();
  await fillFormAndNext('testing', user);

  await user.click(ui.overrideNcdRadio.get());

  expect(ui.projectCreateButton.get()).toBeEnabled();
  await user.click(ui.projectCreateButton.get());

  expect(await ui.projectDashboardText.find()).toBeInTheDocument();
});

function renderCreateProject(props: Partial<CreateProjectPageProps> = {}) {
  renderApp('project/create', <CreateProjectPage {...props} />);
}
