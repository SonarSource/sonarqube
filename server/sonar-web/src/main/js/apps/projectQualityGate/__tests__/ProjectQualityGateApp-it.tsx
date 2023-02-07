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
import selectEvent from 'react-select-event';
import { byRole, byText } from 'testing-library-selector';
import { QualityGatesServiceMock } from '../../../api/mocks/QualityGatesServiceMock';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/mocks/component';
import {
  renderAppWithComponentContext,
  RenderContext,
} from '../../../helpers/testReactTestingUtils';
import { Component } from '../../../types/types';
import routes from '../routes';

jest.mock('../../../api/quality-gates');

jest.mock('../../../app/utils/handleRequiredAuthorization');

let handler: QualityGatesServiceMock;

const ui = {
  qualityGateHeading: byRole('heading', { name: 'project_quality_gate.page' }),
  defaultRadioQualityGate: byRole('radio', {
    name: /project_quality_gate.always_use_default/,
  }),
  specificRadioQualityGate: byRole('radio', { name: /project_quality_gate.always_use_specific/ }),
  qualityGatesSelect: byRole('combobox', { name: 'project_quality_gate.select_specific_qg' }),
  QGWithoutConditionsOptionLabel: byRole('radio', {
    name: /option QG without conditions selected/,
  }),
  QGWithoutNewCodeConditionOptionLabel: byRole('radio', {
    name: 'project_quality_gate.always_use_specific QG without new code conditions',
  }),
  saveButton: byRole('button', { name: 'save' }),
  statusMessage: byRole('status'),
  noConditionsNewCodeWarning: byText('project_quality_gate.no_condition_on_new_code'),
  alertMessage: byRole('alert'),
};

beforeAll(() => {
  handler = new QualityGatesServiceMock();
});

afterEach(() => handler.reset());

it('should require authorization if no permissions set', () => {
  renderProjectQualityGateApp({}, {});
  expect(handleRequiredAuthorization).toHaveBeenCalled();
  expect(ui.qualityGateHeading.query()).not.toBeInTheDocument();
});

it('should be able to select and save specific Quality Gate', async () => {
  renderProjectQualityGateApp();

  expect(await ui.qualityGateHeading.find()).toBeInTheDocument();
  expect(ui.defaultRadioQualityGate.get()).toBeChecked();

  await userEvent.click(ui.specificRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeEnabled();

  await selectEvent.select(ui.qualityGatesSelect.get(), 'Sonar way');
  await userEvent.click(ui.saveButton.get());
  expect(ui.statusMessage.get()).toHaveTextContent(/project_quality_gate.success/);

  // Set back default QG
  await userEvent.click(ui.defaultRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeDisabled();
  expect(ui.defaultRadioQualityGate.get()).toBeChecked();

  await userEvent.click(ui.saveButton.get());
  expect(ui.statusMessage.getAll()[1]).toHaveTextContent(/project_quality_gate.success/);
});

it('shows warning for quality gate that doesnt have conditions on new code', async () => {
  handler.setGetGateForProjectName('Sonar way');
  renderProjectQualityGateApp();

  await userEvent.click(await ui.specificRadioQualityGate.find());
  await selectEvent.select(ui.qualityGatesSelect.get(), 'QG without conditions');
  expect(ui.QGWithoutConditionsOptionLabel.query()).not.toBeInTheDocument();

  await selectEvent.select(ui.qualityGatesSelect.get(), 'QG without new code conditions');
  expect(ui.QGWithoutNewCodeConditionOptionLabel.get()).toBeInTheDocument();
  expect(ui.noConditionsNewCodeWarning.get()).toBeInTheDocument();
});

it('renders nothing and shows alert when any API fails', async () => {
  handler.setThrowOnGetGateForProject(true);
  renderProjectQualityGateApp();

  expect(await ui.alertMessage.find()).toHaveTextContent('unknown');
  expect(ui.qualityGateHeading.query()).not.toBeInTheDocument();
});

function renderProjectQualityGateApp(
  context?: RenderContext,
  componentOverrides: Partial<Component> = { configuration: { showQualityGates: true } }
) {
  renderAppWithComponentContext('project/quality_gate', routes, context, {
    component: mockComponent(componentOverrides),
  });
}
