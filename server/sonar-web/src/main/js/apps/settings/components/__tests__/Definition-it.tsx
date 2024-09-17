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
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import SettingsServiceMock, {
  DEFAULT_DEFINITIONS_MOCK,
} from '../../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockDefinition } from '../../../../helpers/mocks/settings';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ExtendedSettingDefinition, SettingType, SettingValue } from '../../../../types/settings';
import { Component } from '../../../../types/types';
import Definition from '../Definition';

let settingsMock: SettingsServiceMock;

beforeAll(() => {
  settingsMock = new SettingsServiceMock();
});

afterEach(() => {
  settingsMock.reset();
});

beforeEach(jest.clearAllMocks);

const ui = {
  nameHeading: (name: string) => byRole('heading', { name }),
  announcementInput: byLabelText('property.sonar.announcement.message.name'),
  securedInput: byRole('textbox', { name: 'property.sonar.announcement.message.secured.name' }),
  multiValuesInput: byRole('textbox', { name: 'property.sonar.javascript.globals.name' }),
  urlKindInput: byRole('textbox', { name: /sonar.auth.gitlab.url/ }),
  nameInput: byRole('textbox', { name: /property.name.name/ }),
  valueInput: byRole('textbox', { name: /property.value.name/ }),
  savedMsg: byText('settings.state.saved'),
  validationMsg: byText(/settings.state.validation_failed/),
  jsonFormatStatus: byText('settings.json.format_error'),
  jsonFormatButton: byRole('button', { name: 'settings.json.format' }),
  toggleButton: byRole('switch'),
  selectOption: (name: string) => byRole('option', { name }),
  selectInput: byRole('combobox', { name: 'property.test.single.select.list.name' }),
  saveButton: byRole('button', { name: /save/ }),
  cancelButton: byRole('button', { name: /cancel/ }),
  changeButton: byRole('button', { name: 'change_verb' }),
  resetButton: (name: string | RegExp = 'reset_verb') => byRole('button', { name }),
  deleteValueButton: byRole('button', {
    name: /settings.definition.delete_value/,
  }),
  deleteFieldsButton: byRole('button', {
    name: /settings.definitions.delete_fields/,
  }),
};

it.each([
  SettingType.TEXT,
  SettingType.STRING,
  SettingType.PASSWORD,
  SettingType.INTEGER,
  SettingType.LONG,
  SettingType.FLOAT,
  'uknown type',
])(
  'renders definition for SettingType = %s and can do operations',
  async (settingType: SettingType) => {
    const user = userEvent.setup();
    renderDefinition({ type: settingType });

    expect(
      await ui.nameHeading('property.sonar.announcement.message.name').find(),
    ).toBeInTheDocument();

    // Should see no empty validation message
    await user.type(ui.announcementInput.get(), ' ');
    await user.click(ui.saveButton.get());
    expect(await ui.validationMsg.find()).toBeInTheDocument();

    // Should save variable
    await user.type(ui.announcementInput.get(), 'Testing');
    await user.click(await ui.saveButton.find());
    expect(ui.validationMsg.query()).not.toBeInTheDocument();
    expect(ui.announcementInput.get()).toHaveValue(' Testing');
    expect(ui.savedMsg.get()).toBeInTheDocument();

    // Validation message when clearing input to empty
    await user.clear(ui.announcementInput.get());
    expect(ui.validationMsg.get()).toBeInTheDocument();

    // Should reset to previous state on clicking cancel
    await user.type(ui.announcementInput.get(), 'Testing2');
    await user.click(ui.cancelButton.get());
    expect(ui.announcementInput.get()).toHaveValue(' Testing');

    // Clicking reset opens dialog and reset to default on confirm
    await user.click(
      ui.resetButton('settings.definition.reset.property.sonar.announcement.message.name').get(),
    );
    await user.click(ui.resetButton().get());
    expect(ui.announcementInput.get()).toHaveValue('');
  },
);

it('renders definition for SettingType = JSON and can do operations', async () => {
  const user = userEvent.setup();
  renderDefinition({ type: SettingType.JSON });

  expect(
    await ui.nameHeading('property.sonar.announcement.message.name').find(),
  ).toBeInTheDocument();

  // Should show error message if JSON format is not valid
  await user.type(ui.announcementInput.get(), 'invalid format');
  expect(ui.validationMsg.get()).toBeInTheDocument();
  await user.click(ui.jsonFormatButton.get());
  expect(ui.jsonFormatStatus.get()).toBeInTheDocument();

  // Can save valid json and format it
  await user.clear(ui.announcementInput.get());
  await user.type(ui.announcementInput.get(), '1');
  await user.click(ui.jsonFormatButton.get());
  expect(ui.jsonFormatStatus.query()).not.toBeInTheDocument();

  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();
});

it('renders definition for SettingType = BOOLEAN and can do operations', async () => {
  const user = userEvent.setup();
  renderDefinition({
    type: SettingType.BOOLEAN,
  });

  expect(
    await ui.nameHeading('property.sonar.announcement.message.name').find(),
  ).toBeInTheDocument();

  // Can toggle
  await user.click(ui.toggleButton.get());
  expect(ui.toggleButton.get()).toBeChecked();

  // Can cancel toggle
  await user.click(ui.cancelButton.get());
  expect(ui.toggleButton.get()).not.toBeChecked();

  // Can save toggle
  await user.click(ui.toggleButton.get());
  await user.click(ui.saveButton.get());
  expect(ui.toggleButton.get()).toBeChecked();
  expect(ui.savedMsg.get()).toBeInTheDocument();

  // Can reset toggle
  await user.click(
    ui.resetButton('settings.definition.reset.property.sonar.announcement.message.name').get(),
  );
  await user.click(ui.resetButton().get());
  expect(ui.toggleButton.get()).not.toBeChecked();
});

it('renders definition for SettingType = SINGLE_SELECT_LIST and can do operations', async () => {
  const user = userEvent.setup();
  const definition = {
    ...DEFAULT_DEFINITIONS_MOCK[0],
    key: 'test.single.select.list',
    type: SettingType.SINGLE_SELECT_LIST,
    defaultValue: 'default',
    options: ['first', 'second', 'default'],
  };
  settingsMock.setDefinition(definition);
  renderDefinition(definition, { key: definition.key, value: 'default' });

  expect(await ui.nameHeading('property.test.single.select.list.name').find()).toBeInTheDocument();

  // Can select option
  expect(ui.selectInput.get()).toHaveValue('default');
  await user.click(ui.selectInput.get());
  await user.click(ui.selectOption('first').get());
  expect(ui.selectInput.get()).toHaveValue('first');

  // Can cancel action
  await user.click(ui.cancelButton.get());
  expect(ui.selectInput.get()).toHaveValue('default');

  // Can save
  await user.click(ui.selectInput.get());
  await user.click(ui.selectOption('second').get());
  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();

  // Can reset
  await user.click(
    ui.resetButton('settings.definition.reset.property.test.single.select.list.name').get(),
  );
  await user.click(ui.resetButton().get());
  expect(ui.selectInput.get()).toHaveValue('default');
});

it('renders definition for SettingType = FORMATTED_TEXT and can do operations', async () => {
  const user = userEvent.setup();
  renderDefinition({
    type: SettingType.FORMATTED_TEXT,
  });

  expect(
    await ui.nameHeading('property.sonar.announcement.message.name').find(),
  ).toBeInTheDocument();

  // Should see no empty validation message
  await user.type(ui.announcementInput.get(), ' ');
  await user.click(ui.saveButton.get());
  expect(await ui.validationMsg.find()).toBeInTheDocument();

  // Can cancel message
  await user.clear(ui.announcementInput.get());
  await user.type(ui.announcementInput.get(), 'msg');
  await user.click(ui.cancelButton.get());
  expect(ui.announcementInput.get()).toHaveValue('');

  // Can save formatted message
  await user.type(ui.announcementInput.get(), 'https://ok.com');
  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();
  expect(ui.announcementInput.query()).not.toBeInTheDocument();
});

it('renders definition for multiValues type and can do operations', async () => {
  const user = userEvent.setup();
  renderDefinition(
    DEFAULT_DEFINITIONS_MOCK[2],
    {
      key: DEFAULT_DEFINITIONS_MOCK[2].key,
      values: DEFAULT_DEFINITIONS_MOCK[2].defaultValue?.split(','),
    },
    mockComponent(),
  );

  expect(await ui.nameHeading('property.sonar.javascript.globals.name').find()).toBeInTheDocument();
  expect(ui.multiValuesInput.getAll()).toHaveLength(4);

  // Should show validation message if no values
  await user.click(ui.deleteValueButton.getAll()[0]);
  await user.click(ui.deleteValueButton.getAll()[0]);
  await user.click(ui.deleteValueButton.getAll()[0]);

  expect(await ui.multiValuesInput.findAll()).toHaveLength(1);
  expect(ui.validationMsg.get()).toBeInTheDocument();

  // Can cancel and return to previous
  await user.click(ui.cancelButton.get());
  expect(ui.multiValuesInput.getAll()).toHaveLength(4);

  // Can update values and save
  await user.type(last(ui.multiValuesInput.getAll()) as HTMLElement, 'new value');
  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();
  expect(ui.multiValuesInput.getAll()).toHaveLength(5);

  // Can reset to default
  await user.click(
    ui.resetButton('settings.definition.reset.property.sonar.javascript.globals.name').get(),
  );
  await user.click(ui.resetButton().get());
  expect(ui.multiValuesInput.getAll()).toHaveLength(4);
});

it('renders definition for SettingType = PROPERTY_SET and can do operations', async () => {
  const user = userEvent.setup();
  renderDefinition(DEFAULT_DEFINITIONS_MOCK[5]);

  expect(
    await ui.nameHeading('property.sonar.cobol.compilationConstants.name').find(),
  ).toBeInTheDocument();
  expect(screen.getByRole('columnheader', { name: 'Name' })).toBeInTheDocument();
  expect(screen.getByRole('columnheader', { name: 'Value' })).toBeInTheDocument();

  // Should type new values
  await user.type(ui.nameInput.get(), 'any name');
  expect(ui.nameInput.getAll()).toHaveLength(2);

  // Can cancel changes
  await user.click(ui.cancelButton.get());
  expect(ui.nameInput.getAll()).toHaveLength(1);
  expect(ui.nameInput.get()).toHaveValue('');

  // Can save new values
  await user.type(ui.nameInput.get(), 'any name');
  await user.type(ui.valueInput.getAll()[0], 'any value');
  await user.click(ui.saveButton.get());

  expect(ui.savedMsg.get()).toBeInTheDocument();
  expect(ui.nameInput.getAll()[0]).toHaveValue('any name');
  expect(ui.valueInput.getAll()[0]).toHaveValue('any value');

  // Deleting previous value show validation message
  await user.click(ui.deleteFieldsButton.get());
  expect(ui.validationMsg.get()).toBeInTheDocument();

  // Can reset to default
  await user.click(ui.resetButton(/settings.definition.reset/).get());
  await user.click(ui.resetButton().get());

  expect(ui.savedMsg.get()).toBeInTheDocument();
  expect(ui.nameInput.get()).toHaveValue('');
  expect(ui.valueInput.get()).toHaveValue('');
});

it('renders secured definition and can do operations', async () => {
  const user = userEvent.setup();
  const key = `${DEFAULT_DEFINITIONS_MOCK[0].key}.secured`;
  settingsMock.setDefinition(
    mockDefinition({
      ...DEFAULT_DEFINITIONS_MOCK[0],
      key,
    }),
  );
  renderDefinition({
    key,
  });

  expect(
    await ui.nameHeading('property.sonar.announcement.message.secured.name').find(),
  ).toBeInTheDocument();

  // Can type new value and cancel change
  await user.type(ui.securedInput.get(), 'Anything');
  expect(ui.securedInput.get()).toHaveValue('Anything');

  // Can see validation message
  await user.clear(ui.securedInput.get());
  expect(ui.validationMsg.get()).toBeInTheDocument();

  // Can cancel change
  await user.click(ui.cancelButton.get());
  expect(ui.securedInput.get()).toHaveValue('');

  // Can save new value
  await user.type(ui.securedInput.get(), 'Anything');
  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();
  expect(ui.securedInput.query()).not.toBeInTheDocument();

  // Can change value by unlocking input
  await user.click(ui.changeButton.get());
  expect(ui.securedInput.get()).toBeInTheDocument();

  // Cam reset to default
  await user.click(ui.resetButton(/settings.definition.reset/).get());
  await user.click(ui.resetButton().get());

  expect(ui.savedMsg.get()).toBeInTheDocument();
});

it('renders correctly for URL kind definition', async () => {
  const user = userEvent.setup();
  renderDefinition({ key: 'sonar.auth.gitlab.url' });

  // Show validation message
  await user.type(ui.urlKindInput.get(), 'wrongurl');
  expect(ui.validationMsg.get()).toBeInTheDocument();
  expect(ui.saveButton.get()).toBeDisabled();

  // Hides validation msg with correct url
  await user.type(ui.urlKindInput.get(), 'http://hi.there');
  expect(ui.validationMsg.query()).not.toBeInTheDocument();
  expect(ui.saveButton.get()).toBeEnabled();
});

function renderDefinition(
  definition: Partial<ExtendedSettingDefinition> = {},
  initialSetting?: SettingValue,
  component?: Component,
) {
  return renderComponent(
    <Definition
      definition={{ ...DEFAULT_DEFINITIONS_MOCK[0], ...definition }}
      initialSettingValue={initialSetting}
      component={component}
    />,
  );
}
