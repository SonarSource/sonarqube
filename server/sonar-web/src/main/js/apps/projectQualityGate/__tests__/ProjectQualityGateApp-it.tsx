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
import { addGlobalErrorMessage, addGlobalSuccessMessage } from 'design-system';
import selectEvent from 'react-select-event';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { QualityGatesServiceMock } from '../../../api/mocks/QualityGatesServiceMock';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/mocks/component';
import {
  RenderContext,
  renderAppWithComponentContext,
} from '../../../helpers/testReactTestingUtils';
import { Component } from '../../../types/types';
import routes from '../routes';

jest.mock('../../../api/quality-gates');

jest.mock('../../../app/utils/handleRequiredAuthorization');

jest.mock('design-system', () => ({
  ...jest.requireActual('design-system'),
  addGlobalErrorMessage: jest.fn(),
  addGlobalSuccessMessage: jest.fn(),
}));

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

  saveButton: byRole('button', { name: 'save' }),
  noConditionsNewCodeWarning: byText('project_quality_gate.no_condition_on_new_code'),
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
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('project_quality_gate.successfully_updated');

  // Set back default QG
  await userEvent.click(ui.defaultRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeDisabled();
  expect(ui.defaultRadioQualityGate.get()).toBeChecked();

  await userEvent.click(ui.saveButton.get());
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('project_quality_gate.successfully_updated');
});

it('shows warning for quality gate that doesnt have conditions on new code', async () => {
  handler.setGetGateForProjectName('Sonar way');
  renderProjectQualityGateApp();

  await userEvent.click(await ui.specificRadioQualityGate.find());
  await selectEvent.select(ui.qualityGatesSelect.get(), 'QG without conditions');
  expect(ui.QGWithoutConditionsOptionLabel.query()).not.toBeInTheDocument();

  await selectEvent.select(ui.qualityGatesSelect.get(), 'QG without new code conditions');
  expect(ui.noConditionsNewCodeWarning.get()).toBeInTheDocument();
});

it('renders nothing and shows alert when any API fails', async () => {
  handler.setThrowOnGetGateForProject(true);
  renderProjectQualityGateApp();

  await waitFor(() => {
    expect(addGlobalErrorMessage).toHaveBeenCalledWith('unknown');
  });

  expect(ui.qualityGateHeading.query()).not.toBeInTheDocument();
});

function renderProjectQualityGateApp(
  context?: RenderContext,
  componentOverrides: Partial<Component> = { configuration: { showQualityGates: true } },
) {
  renderAppWithComponentContext('project/quality_gate', routes, context, {
    component: mockComponent(componentOverrides),
  });
}
