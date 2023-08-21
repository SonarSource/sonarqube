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
import { within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { first, last } from 'lodash';
import selectEvent from 'react-select-event';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { ProjectActivityServiceMock } from '../../../../api/mocks/ProjectActivityServiceMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockNewCodePeriodBranch } from '../../../../helpers/mocks/new-code-definition';
import { mockAppState } from '../../../../helpers/testMocks';
import {
  RenderContext,
  renderAppWithComponentContext,
} from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import { Feature } from '../../../../types/features';
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import routes from '../../routes';

jest.mock('../../../../api/newCodeDefinition');
jest.mock('../../../../api/projectActivity');
jest.mock('../../../../api/branches');

const codePeriodsMock = new NewCodeDefinitionServiceMock();
const projectActivityMock = new ProjectActivityServiceMock();
const branchHandler = new BranchesServiceMock();

afterEach(() => {
  branchHandler.reset();
  codePeriodsMock.reset();
  projectActivityMock.reset();
});

it('renders correctly without branch support feature', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(await ui.generalSettingRadio.find()).toBeChecked();
  expect(ui.specificAnalysisRadio.query()).not.toBeInTheDocument();

  // User is not admin
  expect(ui.generalSettingsLink.query()).not.toBeInTheDocument();

  // Specific branch setting is not rendered without feature branch
  expect(ui.branchListHeading.query()).not.toBeInTheDocument();
  expect(ui.referenceBranchRadio.query()).not.toBeInTheDocument();
});

it('prevents selection of global setting if it is not compliant and warns non-admin about it', async () => {
  codePeriodsMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '99',
    inherited: true,
  });

  const { ui } = getPageObjects();
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(await ui.generalSettingRadio.find()).toBeChecked();
  expect(ui.generalSettingRadio.get()).toBeDisabled();
  expect(ui.complianceWarning.get()).toBeVisible();
});

it('prevents selection of global setting if it is not compliant and warns admin about it', async () => {
  codePeriodsMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '99',
    inherited: true,
  });

  const { ui } = getPageObjects();
  renderProjectBaselineApp({ appState: mockAppState({ canAdmin: true }) });
  await ui.appIsLoaded();

  expect(await ui.generalSettingRadio.find()).toBeChecked();
  expect(ui.generalSettingRadio.get()).toBeDisabled();
  expect(ui.complianceWarningAdmin.get()).toBeVisible();
  expect(ui.complianceWarning.query()).not.toBeInTheDocument();
});

it('renders correctly with branch support feature', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
    appState: mockAppState({ canAdmin: true }),
  });
  await ui.appIsLoaded();

  expect(await ui.generalSettingRadio.find()).toBeChecked();
  expect(ui.specificAnalysisRadio.query()).not.toBeInTheDocument();

  // User is admin
  expect(ui.generalSettingsLink.get()).toBeInTheDocument();

  // Specific branch setting is rendered with feature support branch
  expect(ui.branchListHeading.get()).toBeInTheDocument();
  expect(ui.referenceBranchRadio.get()).toBeInTheDocument();
});

it('can set previous version specific setting', async () => {
  const { ui, user } = getPageObjects();
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(await ui.previousVersionRadio.find()).toHaveClass('disabled');
  await ui.setPreviousVersionSetting();
  expect(ui.previousVersionRadio.get()).toBeChecked();

  // Save changes
  await user.click(ui.saveButton.get());

  expect(ui.saved.get()).toBeInTheDocument();

  // Set general setting
  await user.click(ui.generalSettingRadio.get());
  expect(ui.previousVersionRadio.get()).toHaveClass('disabled');
  await user.click(ui.saveButton.get());
  expect(ui.saved.get()).toBeInTheDocument();
});

it('can set number of days specific setting', async () => {
  const { ui, user } = getPageObjects();
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(await ui.numberDaysRadio.find()).toHaveClass('disabled');
  await ui.setNumberDaysSetting('10');
  expect(ui.numberDaysRadio.get()).toBeChecked();

  // Reset to initial state
  await user.click(ui.cancelButton.get());
  expect(ui.generalSettingRadio.get()).toBeChecked();
  expect(ui.numberDaysRadio.get()).toHaveClass('disabled');

  // Save changes
  await ui.setNumberDaysSetting('10');
  await user.click(ui.saveButton.get());

  expect(ui.saved.get()).toBeInTheDocument();
});

it('can set reference branch specific setting', async () => {
  const { ui, user } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  expect(await ui.referenceBranchRadio.find()).toHaveClass('disabled');
  await ui.setReferenceBranchSetting('main');
  expect(ui.referenceBranchRadio.get()).toBeChecked();

  // Save changes
  await user.click(ui.saveButton.get());

  expect(ui.saved.get()).toBeInTheDocument();
});

it('cannot set specific analysis setting', async () => {
  const { ui } = getPageObjects();
  codePeriodsMock.setNewCodePeriod({
    type: NewCodeDefinitionType.SpecificAnalysis,
    value: 'analysis_id',
  });
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(await ui.specificAnalysisRadio.find()).toBeChecked();
  expect(ui.specificAnalysisRadio.get()).toHaveClass('disabled');
  expect(ui.specificAnalysisWarning.get()).toBeInTheDocument();

  await selectEvent.select(ui.analysisFromSelect.get(), 'baseline.branch_analyses.ranges.allTime');

  expect(first(ui.analysisListItem.getAll())).toHaveClass('disabled');
  expect(ui.saveButton.get()).toBeDisabled();
});

it('renders correctly branch modal', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.openBranchSettingModal('main');

  expect(ui.specificAnalysisRadio.query()).not.toBeInTheDocument();
});

it('can set a previous version setting for branch', async () => {
  const { ui, user } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();
  await ui.setBranchPreviousVersionSetting('main');

  expect(
    within(byRole('table').get()).getByText('new_code_definition.previous_version')
  ).toBeInTheDocument();

  await user.click(await ui.branchActionsButton('main').find());

  expect(ui.resetToDefaultButton.get()).toBeInTheDocument();
  await user.click(ui.resetToDefaultButton.get());

  expect(
    first(within(byRole('table').get()).getAllByText('branch_list.default_setting'))
  ).toBeInTheDocument();
});

it('can set a number of days setting for branch', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.setBranchNumberOfDaysSetting('main', '15');

  expect(
    within(byRole('table').get()).getByText('new_code_definition.number_days: 15')
  ).toBeInTheDocument();
});

it('cannot set a specific analysis setting for branch', async () => {
  const { ui } = getPageObjects();
  codePeriodsMock.setListBranchesNewCode([
    mockNewCodePeriodBranch({
      branchKey: 'main',
      type: NewCodeDefinitionType.SpecificAnalysis,
      value: 'analysis_id',
    }),
  ]);
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.openBranchSettingModal('main');

  expect(ui.specificAnalysisRadio.get()).toBeChecked();
  expect(ui.specificAnalysisRadio.get()).toHaveClass('disabled');
  expect(ui.specificAnalysisWarning.get()).toBeInTheDocument();

  await selectEvent.select(ui.analysisFromSelect.get(), 'baseline.branch_analyses.ranges.allTime');

  expect(first(ui.analysisListItem.getAll())).toHaveClass('disabled');
  expect(last(ui.saveButton.getAll())).toBeDisabled();
});

it('can set a reference branch setting for branch', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.setBranchReferenceToBranchSetting('main', 'normal-branch');

  expect(
    byRole('table').byText('baseline.reference_branch: normal-branch').get()
  ).toBeInTheDocument();
});

function renderProjectBaselineApp(context: RenderContext = {}, params?: string) {
  return renderAppWithComponentContext(
    'baseline',
    routes,
    {
      ...context,
      navigateTo: params ? `baseline?id=my-project&${params}` : 'baseline?id=my-project',
    },
    {
      component: mockComponent(),
    }
  );
}

function getPageObjects() {
  const user = userEvent.setup();

  const ui = {
    pageHeading: byRole('heading', { name: 'project_baseline.page' }),
    branchTableHeading: byText('branch_list.branch'),
    branchListHeading: byRole('heading', { name: 'project_baseline.default_setting' }),
    generalSettingsLink: byRole('link', { name: 'project_baseline.page.description2.link' }),
    generalSettingRadio: byRole('radio', { name: 'project_baseline.global_setting' }),
    specificSettingRadio: byRole('radio', { name: 'project_baseline.specific_setting' }),
    previousVersionRadio: byRole('radio', {
      name: /new_code_definition.previous_version.description/,
    }),
    numberDaysRadio: byRole('radio', { name: /new_code_definition.number_days.description/ }),
    numberDaysInput: byRole('spinbutton'),
    referenceBranchRadio: byRole('radio', { name: /baseline.reference_branch.description/ }),
    chooseBranchSelect: byRole('combobox', { name: 'baseline.reference_branch.choose' }),
    specificAnalysisRadio: byRole('radio', { name: /baseline.specific_analysis.description/ }),
    specificAnalysisWarning: byText('baseline.specific_analysis.compliance_warning.title'),
    analysisFromSelect: byRole('combobox', { name: 'baseline.analysis_from' }),
    analysisListItem: byRole('radio', { name: /baseline.branch_analyses.analysis_for_x/ }),
    saveButton: byRole('button', { name: 'save' }),
    cancelButton: byRole('button', { name: 'cancel' }),
    branchActionsButton: (branch: string) =>
      byRole('button', { name: `branch_list.show_actions_for_x.${branch}` }),
    editButton: byRole('button', { name: 'edit' }),
    resetToDefaultButton: byRole('button', { name: 'reset_to_default' }),
    saved: byText('settings.state.saved'),
    complianceWarningAdmin: byText('new_code_definition.compliance.warning.explanation.admin'),
    complianceWarning: byText('new_code_definition.compliance.warning.explanation'),
  };

  async function appIsLoaded() {
    expect(await ui.pageHeading.find()).toBeInTheDocument();
  }

  async function setPreviousVersionSetting() {
    await user.click(ui.specificSettingRadio.get());
    await user.click(ui.previousVersionRadio.get());
  }

  async function setBranchPreviousVersionSetting(branch: string) {
    await openBranchSettingModal(branch);
    await user.click(last(ui.previousVersionRadio.getAll()) as HTMLElement);
    await user.click(last(ui.saveButton.getAll()) as HTMLElement);
  }

  async function setNumberDaysSetting(value: string) {
    await user.click(ui.specificSettingRadio.get());
    await user.click(ui.numberDaysRadio.get());
    await user.clear(ui.numberDaysInput.get());
    await user.type(ui.numberDaysInput.get(), value);
  }

  async function setBranchNumberOfDaysSetting(branch: string, value: string) {
    await openBranchSettingModal(branch);
    await user.click(last(ui.numberDaysRadio.getAll()) as HTMLElement);
    await user.clear(ui.numberDaysInput.get());
    await user.type(ui.numberDaysInput.get(), value);
    await user.click(last(ui.saveButton.getAll()) as HTMLElement);
  }

  async function setReferenceBranchSetting(branch: string) {
    await user.click(ui.specificSettingRadio.get());
    await user.click(ui.referenceBranchRadio.get());
    await selectEvent.select(ui.chooseBranchSelect.get(), branch);
  }

  async function setBranchReferenceToBranchSetting(branch: string, branchRef: string) {
    await openBranchSettingModal(branch);
    await user.click(last(ui.referenceBranchRadio.getAll()) as HTMLElement);
    await selectEvent.select(ui.chooseBranchSelect.get(), branchRef);
    await user.click(last(ui.saveButton.getAll()) as HTMLElement);
  }

  async function openBranchSettingModal(branch: string) {
    await user.click(await ui.branchActionsButton(branch).find());
    await user.click(ui.editButton.get());
  }

  return {
    ui: {
      ...ui,
      appIsLoaded,
      setNumberDaysSetting,
      setPreviousVersionSetting,
      setReferenceBranchSetting,
      setBranchPreviousVersionSetting,
      setBranchNumberOfDaysSetting,
      setBranchReferenceToBranchSetting,
      openBranchSettingModal,
    },
    user,
  };
}
