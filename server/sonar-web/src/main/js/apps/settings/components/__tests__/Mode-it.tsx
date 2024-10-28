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

import userEvent from '@testing-library/user-event';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { QualityGatesServiceMock } from '../../../../api/mocks/QualityGatesServiceMock';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import { definitions } from '../../../../helpers/mocks/definitions-list';
import { mockQualityGate } from '../../../../helpers/mocks/quality-gates';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { SettingsKey } from '../../../../types/settings';
import { Mode } from '../Mode';

let settingServiceMock: SettingsServiceMock;
let qualityGatesServiceMock: QualityGatesServiceMock;

beforeAll(() => {
  settingServiceMock = new SettingsServiceMock();
  qualityGatesServiceMock = new QualityGatesServiceMock();
  settingServiceMock.setDefinitions(definitions);
});

afterEach(() => {
  settingServiceMock.reset();
  qualityGatesServiceMock.reset();
});

const ui = {
  standard: byRole('radio', { name: /settings.mode.standard/ }),
  mqr: byRole('radio', { name: /settings.mode.mqr/ }),
  saveButton: byRole('button', { name: /settings.mode.save/ }),
  cancelButton: byRole('button', { name: 'cancel' }),
  qgHasOtherModeConditionMessage: (isStandardMode = false) =>
    byText(`settings.mode.instance_conditions_from_other_mode.${isStandardMode}`),
  saveWarning: byText('settings.mode.save.warning'),
};

it('should be able to select standard mode', async () => {
  const user = userEvent.setup();
  renderMode();

  expect(await ui.standard.find()).toBeInTheDocument();
  expect(ui.mqr.get()).toBeChecked();
  expect(ui.standard.get()).not.toBeChecked();
  expect(ui.saveButton.query()).not.toBeInTheDocument();
  expect(ui.cancelButton.query()).not.toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage().query()).not.toBeInTheDocument();

  await user.click(ui.standard.get());
  expect(ui.mqr.get()).not.toBeChecked();
  expect(ui.standard.get()).toBeChecked();
  expect(ui.saveButton.get()).toBeInTheDocument();
  expect(ui.cancelButton.get()).toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage().get()).toBeInTheDocument();

  await user.click(ui.cancelButton.get());
  expect(ui.mqr.get()).toBeChecked();
  expect(ui.standard.get()).not.toBeChecked();
  expect(ui.saveButton.query()).not.toBeInTheDocument();
  expect(ui.cancelButton.query()).not.toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage().query()).not.toBeInTheDocument();

  await user.click(ui.standard.get());
  await user.click(ui.saveButton.get());
  expect(ui.mqr.get()).not.toBeChecked();
  expect(ui.standard.get()).toBeChecked();
  expect(ui.saveButton.query()).not.toBeInTheDocument();
  expect(ui.cancelButton.query()).not.toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage().query()).not.toBeInTheDocument();
});

it('should be able to select mqr mode', async () => {
  const user = userEvent.setup();
  settingServiceMock.set(SettingsKey.MQRMode, 'false');
  renderMode();

  expect(await ui.standard.find()).toBeInTheDocument();
  expect(ui.mqr.get()).not.toBeChecked();
  expect(ui.standard.get()).toBeChecked();
  expect(ui.saveButton.query()).not.toBeInTheDocument();
  expect(ui.cancelButton.query()).not.toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage(true).query()).not.toBeInTheDocument();

  await user.click(ui.mqr.get());
  expect(ui.mqr.get()).toBeChecked();
  expect(ui.standard.get()).not.toBeChecked();
  expect(ui.saveButton.get()).toBeInTheDocument();
  expect(ui.cancelButton.get()).toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage(true).get()).toBeInTheDocument();

  await user.click(ui.cancelButton.get());
  expect(ui.mqr.get()).not.toBeChecked();
  expect(ui.standard.get()).toBeChecked();
  expect(ui.saveButton.query()).not.toBeInTheDocument();
  expect(ui.cancelButton.query()).not.toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage(true).query()).not.toBeInTheDocument();

  await user.click(ui.mqr.get());
  await user.click(ui.saveButton.get());
  expect(ui.mqr.get()).toBeChecked();
  expect(ui.standard.get()).not.toBeChecked();
  expect(ui.saveButton.query()).not.toBeInTheDocument();
  expect(ui.cancelButton.query()).not.toBeInTheDocument();
  expect(ui.saveWarning.query()).not.toBeInTheDocument();
  expect(ui.qgHasOtherModeConditionMessage(true).query()).not.toBeInTheDocument();
});

it('should not see quality gate info message when there are no Quality Gates that have conditions from other mode', async () => {
  const user = userEvent.setup();
  qualityGatesServiceMock.list = [
    mockQualityGate({ hasMQRConditions: false, hasStandardConditions: false }),
  ];
  renderMode();

  expect(await ui.standard.find()).toBeInTheDocument();
  expect(ui.mqr.get()).toBeChecked();

  await user.click(ui.standard.get());
  expect(ui.qgHasOtherModeConditionMessage().query()).not.toBeInTheDocument();
  expect(ui.saveWarning.get()).toBeInTheDocument();
});

function renderMode() {
  return renderComponent(<Mode />);
}
