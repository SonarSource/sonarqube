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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { last } from 'lodash';
import React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import SettingsServiceMock, {
  DEFAULT_DEFINITIONS_MOCK,
} from '../../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { renderApp } from '../../../../helpers/testReactTestingUtils';
import { AdditionalCategoryComponentProps } from '../AdditionalCategories';
import Languages from '../Languages';

let settingsMock: SettingsServiceMock;

beforeAll(() => {
  settingsMock = new SettingsServiceMock();
});

afterEach(() => {
  settingsMock.reset();
});

beforeEach(jest.clearAllMocks);

const ui = {
  languagesHeading: byRole('heading', { name: 'property.category.languages' }),
  languagesSelect: byRole('combobox', { name: 'property.category.languages' }),
  jsGeneralSubCategoryHeading: byRole('heading', { name: 'property.category.javascript.General' }),
  jsGlobalVariablesHeading: byRole('heading', {
    name: 'property.sonar.javascript.globals.name',
  }),
  jsGlobalVariablesDescription: byText('List of Global variables'),
  jsFileSuffixesHeading: byRole('heading', {
    name: 'property.sonar.javascript.file.suffixes.name',
  }),
  jsGlobalVariablesInput: byRole('textbox', { name: 'property.sonar.javascript.globals.name' }),
  jsResetGlobalVariablesButton: byRole('button', {
    name: 'settings.definition.reset.property.sonar.javascript.globals.name',
  }),

  validationMsg: byText('settings.state.validation_failed.A non empty value must be provided'),
  saveButton: byRole('button', { name: 'save' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  resetButton: byRole('button', { name: 'reset_verb' }),
};

it('renders Language with selected Javascript category', async () => {
  renderLanguages({ selectedCategory: 'javascript' });

  expect(await ui.languagesHeading.find()).toBeInTheDocument();
  expect(await ui.jsGeneralSubCategoryHeading.find()).toBeInTheDocument();
  expect(ui.jsGlobalVariablesHeading.get()).toBeInTheDocument();
  expect(ui.jsFileSuffixesHeading.get()).toBeInTheDocument();
});

it('render Language without definitions', async () => {
  renderLanguages({ selectedCategory: 'niceLanguage' });

  expect(await ui.languagesHeading.find()).toBeInTheDocument();
  expect(screen.queryByText(/niceLanguage/)).not.toBeInTheDocument();
});

it('can save/reset/cancel or see error for custom mocked multi values definition Global Variables', async () => {
  const user = userEvent.setup();
  renderLanguages({ selectedCategory: 'javascript' });

  const jsVarsInputs = await ui.jsGlobalVariablesInput.findAll();
  const lastInput = last(jsVarsInputs);
  // Adding new js variable (multi-values input)
  expect(jsVarsInputs).toHaveLength(4);

  // Should see a validation message on typing empty string
  await user.type(lastInput as HTMLElement, ' ');
  await user.click(await ui.saveButton.find());
  expect(await ui.validationMsg.find()).toBeInTheDocument();

  // Should save variable
  await user.type(lastInput as HTMLElement, 'Testing');
  await user.click(await ui.saveButton.find());
  expect(ui.validationMsg.query()).not.toBeInTheDocument();
  expect(lastInput).toHaveValue(' Testing');

  // Should reset to previous state on clicking cancel
  await user.type(last(ui.jsGlobalVariablesInput.getAll()) as HTMLElement, 'Testing2');
  await user.click(ui.cancelButton.get());
  expect(last(ui.jsGlobalVariablesInput.getAll())).not.toHaveValue('Testing2');

  // Clicking reset opens dialog and reset to default on confirm
  const defaultValues = ['angular', 'google', 'd3', ''];
  await user.click(ui.jsResetGlobalVariablesButton.get());
  await user.click(ui.resetButton.get());
  const newInputs = ui.jsGlobalVariablesInput.getAll();
  defaultValues.forEach((value, index) => expect(newInputs[index]).toHaveValue(value));
});

function renderLanguages(
  overrides: Partial<AdditionalCategoryComponentProps> = {},
  component = mockComponent(),
) {
  return renderApp(
    '/',
    <Languages
      definitions={DEFAULT_DEFINITIONS_MOCK}
      component={component}
      categories={['javascript', 'sjava']}
      selectedCategory=""
      {...overrides}
    />,
  );
}
