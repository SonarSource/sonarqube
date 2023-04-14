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
import { byRole, byText } from 'testing-library-selector';
import NewCodePeriodsServiceMock from '../../../../api/mocks/NewCodePeriodsServiceMock';
import { ProjectActivityServiceMock } from '../../../../api/mocks/ProjectActivityServiceMock';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAppState } from '../../../../helpers/testMocks';
import {
  renderAppWithComponentContext,
  RenderContext,
} from '../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../types/features';
import routes from '../../routes';

jest.mock('../../../../api/newCodePeriod');
jest.mock('../../../../api/projectActivity');

const codePeriodsMock = new NewCodePeriodsServiceMock();
const projectActivityMock = new ProjectActivityServiceMock();

afterEach(() => {
  codePeriodsMock.reset();
  projectActivityMock.reset();
});

it('renders correctly without branch support feature', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(ui.generalSettingRadio.get()).toBeChecked();
  expect(ui.specificAnalysisRadio.get()).toBeInTheDocument();

  // User is not admin
  expect(ui.generalSettingsLink.query()).not.toBeInTheDocument();

  // Specific branch setting is not rendered without feature branch
  expect(ui.branchListHeading.query()).not.toBeInTheDocument();
  expect(ui.referenceBranchRadio.query()).not.toBeInTheDocument();
});

it('renders correctly with branch support feature', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
    appState: mockAppState({ canAdmin: true }),
  });
  await ui.appIsLoaded();

  expect(ui.generalSettingRadio.get()).toBeChecked();
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

  expect(ui.previousVersionRadio.get()).toHaveClass('disabled');
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

  expect(ui.numberDaysRadio.get()).toHaveClass('disabled');
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

  expect(ui.referenceBranchRadio.get()).toHaveClass('disabled');
  await ui.setReferenceBranchSetting('main');
  expect(ui.referenceBranchRadio.get()).toBeChecked();

  // Save changes
  await user.click(ui.saveButton.get());

  expect(ui.saved.get()).toBeInTheDocument();
});

it('can set specific analysis setting', async () => {
  const { ui, user } = getPageObjects();
  renderProjectBaselineApp();
  await ui.appIsLoaded();

  expect(ui.specificAnalysisRadio.get()).toHaveClass('disabled');
  await ui.setSpecificAnalysisSetting();
  expect(ui.specificAnalysisRadio.get()).toBeChecked();

  // Save changes
  await user.click(ui.saveButton.get());

  expect(ui.saved.get()).toBeInTheDocument();
});

it('can set a previous version setting for branch', async () => {
  const { ui, user } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();
  await ui.setBranchPreviousVersionSetting('main');

  expect(within(byRole('table').get()).getByText('baseline.previous_version')).toBeInTheDocument();

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

  expect(within(byRole('table').get()).getByText('baseline.number_days: 15')).toBeInTheDocument();
});

it('can set a specific analysis setting for branch', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.setBranchSpecificAnalysisSetting('main');

  expect(within(byRole('table').get()).getByText(/baseline.specific_analysis/)).toBeInTheDocument();
});

it('can set a reference branch setting for branch', async () => {
  const { ui } = getPageObjects();
  renderProjectBaselineApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.setBranchReferenceToBranchSetting('main', 'feature');

  expect(
    within(byRole('table').get()).getByText('baseline.reference_branch: feature')
  ).toBeInTheDocument();
});

function renderProjectBaselineApp(context: RenderContext = {}) {
  const branch = mockBranch({ name: 'main', isMain: true });
  return renderAppWithComponentContext('baseline', routes, context, {
    component: mockComponent(),
    branchLike: branch,
    branchLikes: [branch, mockBranch({ name: 'feature' })],
  });
}

function getPageObjects() {
  const user = userEvent.setup();

  const ui = {
    pageHeading: byRole('heading', { name: 'project_baseline.page' }),
    branchListHeading: byRole('heading', { name: 'project_baseline.default_setting' }),
    generalSettingsLink: byRole('link', { name: 'project_baseline.page.description2.link' }),
    generalSettingRadio: byRole('radio', { name: 'project_baseline.general_setting' }),
    specificSettingRadio: byRole('radio', { name: 'project_baseline.specific_setting' }),
    previousVersionRadio: byRole('radio', { name: /baseline.previous_version.description/ }),
    numberDaysRadio: byRole('radio', { name: /baseline.number_days.description/ }),
    numberDaysInput: byRole('textbox'),
    referenceBranchRadio: byRole('radio', { name: /baseline.reference_branch.description/ }),
    chooseBranchSelect: byRole('combobox', { name: 'baseline.reference_branch.choose' }),
    specificAnalysisRadio: byRole('radio', { name: /baseline.specific_analysis.description/ }),
    analysisFromSelect: byRole('combobox', { name: 'baseline.analysis_from' }),
    analysisListItem: byRole('radio', { name: /baseline.branch_analyses.analysis_for_x/ }),
    saveButton: byRole('button', { name: 'save' }),
    cancelButton: byRole('button', { name: 'cancel' }),
    branchActionsButton: (branch: string) =>
      byRole('button', { name: `branch_list.show_actions_for_x.${branch}` }),
    editButton: byRole('button', { name: 'edit' }),
    resetToDefaultButton: byRole('button', { name: 'reset_to_default' }),
    saved: byText('settings.state.saved'),
  };

  async function appIsLoaded() {
    expect(await ui.pageHeading.find()).toBeInTheDocument();
  }

  async function setPreviousVersionSetting() {
    await user.click(ui.specificSettingRadio.get());
    await user.click(ui.previousVersionRadio.get());
  }

  async function setBranchPreviousVersionSetting(branch: string) {
    await user.click(await ui.branchActionsButton(branch).find());
    await user.click(ui.editButton.get());
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
    await user.click(await ui.branchActionsButton(branch).find());
    await user.click(ui.editButton.get());
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
    await user.click(await ui.branchActionsButton(branch).find());
    await user.click(ui.editButton.get());
    await user.click(last(ui.referenceBranchRadio.getAll()) as HTMLElement);
    await selectEvent.select(ui.chooseBranchSelect.get(), branchRef);
    await user.click(last(ui.saveButton.getAll()) as HTMLElement);
  }

  async function setSpecificAnalysisSetting() {
    await user.click(ui.specificSettingRadio.get());
    await user.click(ui.specificAnalysisRadio.get());
    await selectEvent.select(
      ui.analysisFromSelect.get(),
      'baseline.branch_analyses.ranges.allTime'
    );
    await user.click(first(ui.analysisListItem.getAll()) as HTMLElement);
  }

  async function setBranchSpecificAnalysisSetting(branch: string) {
    await user.click(await ui.branchActionsButton(branch).find());
    await user.click(ui.editButton.get());
    await user.click(last(ui.specificAnalysisRadio.getAll()) as HTMLElement);
    await selectEvent.select(
      ui.analysisFromSelect.get(),
      'baseline.branch_analyses.ranges.allTime'
    );
    await user.click(first(ui.analysisListItem.getAll()) as HTMLElement);
    await user.click(last(ui.saveButton.getAll()) as HTMLElement);
  }

  return {
    ui: {
      ...ui,
      appIsLoaded,
      setNumberDaysSetting,
      setPreviousVersionSetting,
      setReferenceBranchSetting,
      setSpecificAnalysisSetting,
      setBranchPreviousVersionSetting,
      setBranchNumberOfDaysSetting,
      setBranchSpecificAnalysisSetting,
      setBranchReferenceToBranchSetting,
    },
    user,
  };
}
