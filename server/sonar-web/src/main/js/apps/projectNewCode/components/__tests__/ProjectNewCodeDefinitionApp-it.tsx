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
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { first, last } from 'lodash';
import selectEvent from 'react-select-event';
import { MessageTypes } from '../../../../api/messages';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import MessagesServiceMock from '../../../../api/mocks/MessagesServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { ProjectActivityServiceMock } from '../../../../api/mocks/ProjectActivityServiceMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockNewCodePeriodBranch } from '../../../../helpers/mocks/new-code-definition';
import { mockAnalysis } from '../../../../helpers/mocks/project-activity';
import { mockAppState } from '../../../../helpers/testMocks';
import {
  RenderContext,
  renderAppWithComponentContext,
} from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import { Feature } from '../../../../types/features';
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import routes from '../../routes';

jest.mock('../../../../api/newCodeDefinition');
jest.mock('../../../../api/projectActivity');
jest.mock('../../../../api/branches');

const newCodeDefinitionMock = new NewCodeDefinitionServiceMock();
const projectActivityMock = new ProjectActivityServiceMock();
const branchHandler = new BranchesServiceMock();
const messagesMock = new MessagesServiceMock();

afterEach(() => {
  branchHandler.reset();
  newCodeDefinitionMock.reset();
  projectActivityMock.reset();
  messagesMock.reset();
});

it('renders correctly without branch support feature', async () => {
  const { ui } = getPageObjects();
  renderProjectNewCodeDefinitionApp();
  await ui.appIsLoaded();

  expect(await ui.generalSettingRadio.find()).toBeChecked();
  expect(ui.specificAnalysisRadio.query()).not.toBeInTheDocument();

  // User is not admin
  expect(ui.generalSettingsLink.query()).not.toBeInTheDocument();

  // Specific branch setting is not rendered without feature branch
  expect(ui.branchListHeading.query()).not.toBeInTheDocument();
  expect(ui.referenceBranchRadio.query()).not.toBeInTheDocument();
});

it('renders correctly with branch support feature', async () => {
  const { ui } = getPageObjects();
  renderProjectNewCodeDefinitionApp({
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
  renderProjectNewCodeDefinitionApp();
  await ui.appIsLoaded();

  expect(await ui.previousVersionRadio.find()).toHaveClass('disabled');
  await ui.setPreviousVersionSetting();
  expect(ui.previousVersionRadio.get()).toBeChecked();

  // Save changes
  await user.click(ui.saveButton.get());

  expect(ui.saveButton.get()).toBeDisabled();

  // Set general setting
  await user.click(ui.generalSettingRadio.get());
  expect(ui.previousVersionRadio.get()).toHaveClass('disabled');
  await user.click(ui.saveButton.get());
  expect(ui.saveButton.get()).toBeDisabled();
});

it('can set number of days specific setting', async () => {
  const { ui, user } = getPageObjects();
  renderProjectNewCodeDefinitionApp();
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

  expect(ui.saveButton.get()).toBeDisabled();
});

it('can set reference branch specific setting', async () => {
  const { ui, user } = getPageObjects();
  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  expect(await ui.referenceBranchRadio.find()).toHaveClass('disabled');
  await ui.setReferenceBranchSetting('main');
  expect(ui.referenceBranchRadio.get()).toBeChecked();

  // Save changes
  await user.click(ui.saveButton.get());

  expect(ui.saveButton.get()).toBeDisabled();
});

it('cannot set specific analysis setting', async () => {
  const { ui } = getPageObjects();
  newCodeDefinitionMock.setListBranchesNewCode([
    mockNewCodePeriodBranch({
      branchKey: 'main',
      type: NewCodeDefinitionType.SpecificAnalysis,
      value: 'analysis_id',
    }),
  ]);
  projectActivityMock.setAnalysesList([
    mockAnalysis({
      key: `analysis_id`,
      date: '2018-01-11T00:00:00+0200',
    }),
  ]);
  renderProjectNewCodeDefinitionApp();
  await ui.appIsLoaded();

  expect(await ui.specificAnalysisRadio.find()).toBeChecked();
  expect(ui.baselineSpecificAnalysisDate.get()).toBeInTheDocument();

  expect(ui.specificAnalysisRadio.get()).toHaveClass('disabled');
  expect(ui.specificAnalysisWarning.get()).toBeInTheDocument();

  expect(ui.saveButton.get()).toBeDisabled();
});

it('renders correctly branch modal', async () => {
  const { ui } = getPageObjects();
  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.openBranchSettingModal('main');

  expect(ui.specificAnalysisRadio.query()).not.toBeInTheDocument();
});

it('can set a previous version setting for branch', async () => {
  const { ui, user } = getPageObjects();
  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();
  await ui.setBranchPreviousVersionSetting('main');

  expect(
    byRole('table').byRole('cell', { name: 'branch_list.default_setting' }).getAll(),
  ).toHaveLength(2);
  expect(byRole('table').byText('new_code_definition.previous_version').get()).toBeInTheDocument();

  await user.click(await ui.branchActionsButton('main').find());

  expect(ui.resetToDefaultButton.get()).toBeInTheDocument();
  await user.click(ui.resetToDefaultButton.get());

  expect(
    byRole('table').byRole('cell', { name: 'branch_list.default_setting' }).getAll(),
  ).toHaveLength(3);
});

it('can set a number of days setting for branch', async () => {
  const { ui } = getPageObjects();
  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.setBranchNumberOfDaysSetting('main', '15');

  expect(byRole('table').byText('new_code_definition.number_days: 15').get()).toBeInTheDocument();
});

it('cannot set a specific analysis setting for branch', async () => {
  const { ui, user } = getPageObjects();
  newCodeDefinitionMock.setListBranchesNewCode([
    mockNewCodePeriodBranch({
      branchKey: 'main',
      type: NewCodeDefinitionType.SpecificAnalysis,
      value: 'analysis_id',
    }),
  ]);
  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await user.click(await byLabelText('branch_list.show_actions_for_x.main').find());
  await user.click(await byRole('menuitem', { name: 'edit' }).find());
  expect(ui.specificAnalysisRadio.get()).toBeChecked();
  expect(ui.specificAnalysisRadio.get()).toHaveClass('disabled');
  expect(ui.specificAnalysisWarning.get()).toBeInTheDocument();

  expect(last(ui.saveButton.getAll())).toBeDisabled();
});

it('can set a reference branch setting for branch', async () => {
  const { ui } = getPageObjects();
  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });
  await ui.appIsLoaded();

  await ui.setBranchReferenceToBranchSetting('main', 'normal-branch');

  expect(
    byRole('table').byText('baseline.reference_branch: normal-branch').get(),
  ).toBeInTheDocument();
});

it('should display NCD banner if some branches had their NCD automatically changed', async () => {
  const { ui } = getPageObjects();

  newCodeDefinitionMock.setListBranchesNewCode([
    {
      projectKey: 'test-project:test',
      branchKey: 'test-branch',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '25',
      inherited: true,
      updatedAt: 1692720953662,
    },
    {
      projectKey: 'test-project:test',
      branchKey: 'master',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '32',
      previousNonCompliantValue: '150',
      updatedAt: 1692721852743,
    },
  ]);

  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });

  expect(await ui.branchNCDsBanner.find()).toBeInTheDocument();
  expect(
    ui.branchNCDsBanner.byText('new_code_definition.auto_update.branch.list_itemmaster32150').get(),
  ).toBeInTheDocument();
});

it('should not display NCD banner if some branches had their NCD automatically changed and banne has been dismissed', async () => {
  const { ui } = getPageObjects();

  newCodeDefinitionMock.setListBranchesNewCode([
    {
      projectKey: 'test-project:test',
      branchKey: 'test-branch',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '25',
      inherited: true,
      updatedAt: 1692720953662,
    },
    {
      projectKey: 'test-project:test',
      branchKey: 'master',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '32',
      previousNonCompliantValue: '150',
      updatedAt: 1692721852743,
    },
  ]);
  messagesMock.setMessageDismissed({
    projectKey: 'test-project:test',
    messageType: MessageTypes.BranchNcd90,
  });

  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });

  expect(await ui.branchNCDsBanner.query()).not.toBeInTheDocument();
});

it('should correctly dismiss branch banner', async () => {
  const { ui } = getPageObjects();

  newCodeDefinitionMock.setListBranchesNewCode([
    {
      projectKey: 'test-project:test',
      branchKey: 'test-branch',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '25',
      inherited: true,
      updatedAt: 1692720953662,
    },
    {
      projectKey: 'test-project:test',
      branchKey: 'master',
      type: NewCodeDefinitionType.NumberOfDays,
      value: '32',
      previousNonCompliantValue: '150',
      updatedAt: 1692721852743,
    },
  ]);

  renderProjectNewCodeDefinitionApp({
    featureList: [Feature.BranchSupport],
  });

  expect(await ui.branchNCDsBanner.find()).toBeInTheDocument();

  const user = userEvent.setup();
  await act(async () => {
    await user.click(ui.dismissButton.get());
  });

  expect(ui.branchNCDsBanner.query()).not.toBeInTheDocument();
});

function renderProjectNewCodeDefinitionApp(context: RenderContext = {}, params?: string) {
  return renderAppWithComponentContext(
    'baseline',
    routes,
    {
      ...context,
      navigateTo: params ? `baseline?id=my-project&${params}` : 'baseline?id=my-project',
    },
    {
      component: mockComponent(),
    },
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
    saveButton: byRole('button', { name: 'save' }),
    cancelButton: byRole('button', { name: 'cancel' }),
    branchActionsButton: (name: string) =>
      byRole('button', { name: `branch_list.show_actions_for_x.${name}` }),
    resetToDefaultButton: byRole('menuitem', { name: 'reset_to_default' }),
    branchNCDsBanner: byText(/new_code_definition.auto_update.branch.message/),
    dismissButton: byLabelText('dismiss'),
    baselineSpecificAnalysisDate: byText(/January 10, 2018/),
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
    await user.click(await byLabelText(`branch_list.edit_for_x.${branch}`).find());
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
