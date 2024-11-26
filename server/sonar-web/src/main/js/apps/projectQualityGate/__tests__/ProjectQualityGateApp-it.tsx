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
import { addGlobalErrorMessage, addGlobalSuccessMessage } from '~design-system';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import {
  AiCodeAssuredServiceMock,
  PROJECT_WITH_AI_ASSURED_QG,
  PROJECT_WITHOUT_AI_ASSURED_QG,
} from '../../../api/mocks/AiCodeAssuredServiceMock';
import { QualityGatesServiceMock } from '../../../api/mocks/QualityGatesServiceMock';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent } from '../../../helpers/mocks/component';
import {
  renderAppWithComponentContext,
  RenderContext,
} from '../../../helpers/testReactTestingUtils';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import routes from '../routes';

jest.mock('../../../api/quality-gates');

jest.mock('../../../app/utils/handleRequiredAuthorization');

jest.mock('~design-system', () => ({
  ...jest.requireActual('~design-system'),
  addGlobalErrorMessage: jest.fn(),
  addGlobalSuccessMessage: jest.fn(),
}));

let handler: QualityGatesServiceMock;
const aiCodeAssurance = new AiCodeAssuredServiceMock();

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
  aiAssuredBanner: byText('project_quality_gate.ai_generated_code_protected.description'),
  containsAiCodeBanner: byText('project_quality_gate.ai_generated_code_not_protected.description'),
  qgAssuredSelctedSuccessMessage: byText('project_quality_gate.ai_assured_quality_gate'),
  qgAssuredNotSelectedWarningMessage: byText('project_quality_gate.not_ai_assured_quality_gate'),
};

beforeAll(() => {
  handler = new QualityGatesServiceMock();
});

afterEach(() => {
  handler.reset();
  aiCodeAssurance.reset();
});

it('should require authorization if no permissions set', async () => {
  renderProjectQualityGateApp({}, {});

  await waitFor(() => {
    expect(handleRequiredAuthorization).toHaveBeenCalled();
  });
  expect(ui.qualityGateHeading.query()).not.toBeInTheDocument();
});

it('should be able to select and save specific Quality Gate', async () => {
  const user = userEvent.setup();
  renderProjectQualityGateApp();

  expect(await ui.qualityGateHeading.find()).toBeInTheDocument();
  expect(ui.defaultRadioQualityGate.get()).toBeChecked();

  await user.click(ui.specificRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeEnabled();

  await user.click(ui.qualityGatesSelect.get());
  await user.click(byText('Sonar way').get());

  await user.click(ui.saveButton.get());
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('project_quality_gate.successfully_updated');

  // Set back default QG
  await user.click(ui.defaultRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeDisabled();
  expect(ui.defaultRadioQualityGate.get()).toBeChecked();

  await user.click(ui.saveButton.get());
  expect(addGlobalSuccessMessage).toHaveBeenCalledWith('project_quality_gate.successfully_updated');
});

it('shows warning for quality gate that doesnt have conditions on new code', async () => {
  const user = userEvent.setup();
  handler.setGetGateForProjectName('Sonar way');
  renderProjectQualityGateApp();

  await user.click(await ui.specificRadioQualityGate.find());

  await user.click(ui.qualityGatesSelect.get());
  await user.click(byText('QG without conditions').get());

  expect(ui.QGWithoutConditionsOptionLabel.query()).not.toBeInTheDocument();

  await user.click(ui.qualityGatesSelect.get());
  await user.click(byText('QG without new code conditions').get());

  expect(ui.noConditionsNewCodeWarning.get()).toBeInTheDocument();
});

it('should show AI assured banner if project is AI assured', async () => {
  renderProjectQualityGateApp(
    { featureList: [Feature.AiCodeAssurance] },
    {
      configuration: { showQualityGates: true },
      key: PROJECT_WITH_AI_ASSURED_QG,
      name: PROJECT_WITH_AI_ASSURED_QG,
    },
  );

  expect(await ui.aiAssuredBanner.find()).toBeInTheDocument();
});

it('should show contains AI code banner if project contains AI code but quality gate is not correct', async () => {
  renderProjectQualityGateApp(
    { featureList: [Feature.AiCodeAssurance] },
    {
      configuration: { showQualityGates: true },
      key: PROJECT_WITHOUT_AI_ASSURED_QG,
      name: PROJECT_WITHOUT_AI_ASSURED_QG,
    },
  );

  expect(await ui.containsAiCodeBanner.find()).toBeInTheDocument();
});

it('should not show any AI code banner if ai code feature is false', async () => {
  renderProjectQualityGateApp(
    { featureList: [Feature.AiCodeAssurance] },
    {
      configuration: { showQualityGates: true },
      key: 'no-ai-code',
      name: 'no-ai-code',
    },
  );

  expect(await ui.qualityGateHeading.find()).toBeInTheDocument();
  expect(ui.aiAssuredBanner.query()).not.toBeInTheDocument();
  expect(ui.containsAiCodeBanner.query()).not.toBeInTheDocument();
});

it('should show success/warning when selecting quality gate', async () => {
  const user = userEvent.setup();

  renderProjectQualityGateApp(
    { featureList: [Feature.AiCodeAssurance] },
    {
      configuration: { showQualityGates: true },
      key: PROJECT_WITH_AI_ASSURED_QG,
      name: PROJECT_WITH_AI_ASSURED_QG,
    },
  );

  expect(await ui.aiAssuredBanner.find()).toBeInTheDocument();

  await user.click(ui.specificRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeEnabled();

  await user.click(ui.qualityGatesSelect.get());
  await user.click(byText('Sonar way for AI code').get());
  expect(ui.qgAssuredSelctedSuccessMessage.get()).toBeInTheDocument();

  await user.click(ui.qualityGatesSelect.get());
  await user.click(byText('Sonar way').get());
  expect(ui.qgAssuredNotSelectedWarningMessage.get()).toBeInTheDocument();

  await user.click(ui.defaultRadioQualityGate.get());
  expect(ui.qualityGatesSelect.get()).toBeDisabled();
  expect(ui.defaultRadioQualityGate.get()).toBeChecked();
  expect(ui.qgAssuredNotSelectedWarningMessage.get()).toBeInTheDocument();
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
