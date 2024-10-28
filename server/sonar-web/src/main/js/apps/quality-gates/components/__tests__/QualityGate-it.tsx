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

import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { byLabelText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import { QualityGatesServiceMock } from '../../../../api/mocks/QualityGatesServiceMock';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import UsersServiceMock from '../../../../api/mocks/UsersServiceMock';
import { searchProjects, searchUsers } from '../../../../api/quality-gates';
import { dismissNotice } from '../../../../api/users';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderAppRoutes, RenderContext } from '../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../types/features';
import { SettingsKey } from '../../../../types/settings';
import { CaycStatus } from '../../../../types/types';
import { NoticeType } from '../../../../types/users';
import routes from '../../routes';

const ui = {
  batchUpdate: byRole('button', { name: 'quality_gates.mode_banner.button' }),
  singleUpdate: byRole('button', {
    name: /quality_gates.mqr_mode_update.single_metric.tooltip.message/,
  }),
  removeCondition: byRole('button', { name: /quality_gates.condition.delete/ }),
  listItem: byTestId('js-subnavigation-item'),
  requiresUpdateIndicator: byTestId('quality-gates-mqr-standard-mode-update-indicator'),
  qualityGateListItem: (qualityGateName: string) => byRole('link', { name: qualityGateName }),
  newConditionRow: byTestId('quality-gates__conditions-new').byRole('row'),
  overallConditionRow: byTestId('quality-gates__conditions-overall').byRole('row'),
  batchDialog: byRole('dialog', { name: /quality_gates.update_conditions.header/ }),
  singleDialog: byRole('dialog', { name: /quality_gates.update_conditions.header.single_metric/ }),
  updateMetricsBtn: byRole('button', { name: 'quality_gates.update_conditions.update_metrics' }),
  updateSingleBtn: byRole('button', { name: 'update_verb' }),
  cancelBtn: byRole('button', { name: 'cancel' }),
  standardBadge: byText('quality_gates.metric.standard_mode_short'),
  mqrBadge: byText('quality_gates.metric.mqr_mode_short'),
};

let qualityGateHandler: QualityGatesServiceMock;
let usersHandler: UsersServiceMock;
let settingsHandler: SettingsServiceMock;

beforeAll(() => {
  qualityGateHandler = new QualityGatesServiceMock();
  usersHandler = new UsersServiceMock();
  settingsHandler = new SettingsServiceMock();
});

afterEach(() => {
  qualityGateHandler.reset();
  usersHandler.reset();
  settingsHandler.reset();
});

it('should open the default quality gates', async () => {
  renderQualityGateApp();

  const defaultQualityGate = qualityGateHandler.getDefaultQualityGate();
  expect(
    await screen.findByRole('button', {
      current: 'page',
      name: `${defaultQualityGate.name} default`,
    }),
  ).toBeInTheDocument();
});

it('should list all quality gates', async () => {
  renderQualityGateApp();

  expect(
    await screen.findByRole('button', {
      name: `${qualityGateHandler.getDefaultQualityGate().name} default`,
    }),
  ).toBeInTheDocument();

  expect(
    screen.getByRole('button', {
      name: `${qualityGateHandler.getBuiltInQualityGate().name} quality_gates.built_in`,
    }),
  ).toBeInTheDocument();
});

it('should show MQR mode update icon if standard mode conditions are present', async () => {
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  expect(await ui.requiresUpdateIndicator.findAll()).toHaveLength(3);
});

it('should show Standard mode update icon if MQR mode conditions are present', async () => {
  settingsHandler.set(SettingsKey.MQRMode, 'false');
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  expect(await ui.requiresUpdateIndicator.findAll()).toHaveLength(4);
});

it('should render the built-in quality gate properly', async () => {
  const user = userEvent.setup();
  renderQualityGateApp();

  const builtInQualityGate = await screen.findByText('Sonar way');

  await user.click(builtInQualityGate);

  expect(await screen.findByText(/quality_gates.is_built_in.cayc.description/)).toBeInTheDocument();
});

it('should be able to create a quality gate then delete it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();
  let createButton = await screen.findByRole('button', { name: 'create' });

  // Using keyboard
  await user.click(createButton);
  await user.click(screen.getByRole('textbox', { name: /name.*/ }));
  await user.keyboard('testone');
  await user.click(screen.getByRole('button', { name: 'quality_gate.create' }));
  expect(await screen.findByRole('button', { name: 'testone' })).toBeInTheDocument();

  // Using modal button
  createButton = await screen.findByRole('button', { name: 'create' });
  await user.click(createButton);
  const saveButton = screen.getByRole('button', { name: 'quality_gate.create' });

  expect(saveButton).toBeDisabled();
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  await user.click(nameInput);
  await user.keyboard('testtwo');
  await user.click(saveButton);

  const newQG = await screen.findByRole('button', { name: 'testtwo' });

  expect(newQG).toBeInTheDocument();

  // Delete the quality gate
  await user.click(newQG);

  await user.click(screen.getByLabelText('actions'));
  const deleteButton = screen.getByRole('menuitem', { name: 'delete' });
  await user.click(deleteButton);
  const popup = screen.getByRole('dialog');
  const dialogDeleteButton = within(popup).getByRole('button', { name: 'delete' });
  await user.click(dialogDeleteButton);

  await waitFor(() => {
    expect(screen.queryByRole('button', { name: 'testtwo' })).not.toBeInTheDocument();
  });
});

it('should be able to copy a quality gate which is CaYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByText('Sonar way');
  await user.click(notDefaultQualityGate);
  await user.click(await screen.findByLabelText('actions'));
  const copyButton = screen.getByRole('menuitem', { name: 'copy' });

  await user.click(copyButton);
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await user.click(nameInput);
  await user.keyboard(' bis{Enter}');
  expect(await screen.findByRole('button', { name: /.* bis/ })).toBeInTheDocument();
});

it('should not be able to copy a quality gate which is not CaYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');
  await user.click(notDefaultQualityGate);
  await user.click(await screen.findByLabelText('actions'));
  const copyButton = screen.getByRole('menuitem', { name: 'copy' });

  expect(copyButton).toHaveAttribute('aria-disabled', 'true');
});

it('should be able to rename a quality gate', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();
  await user.click(await screen.findByLabelText('actions'));
  const renameButton = screen.getByRole('menuitem', { name: 'rename' });

  await user.click(renameButton);
  const nameInput = screen.getByRole('textbox', { name: /name.*/ });
  expect(nameInput).toBeInTheDocument();
  await user.click(nameInput);
  await user.keyboard('{Control>}a{/Control}New Name{Enter}');

  expect(await screen.findByRole('button', { name: /New Name.*/ })).toBeInTheDocument();
});

it('should not be able to set as default a quality gate which is not CaYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');
  await user.click(notDefaultQualityGate);
  await user.click(await screen.findByLabelText('actions'));
  const setAsDefaultButton = screen.getByRole('menuitem', { name: 'set_as_default' });
  expect(setAsDefaultButton).toHaveAttribute('aria-disabled', 'true');
});

it('should be able to set as default a quality gate which is CaYC compliant', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const notDefaultQualityGate = await screen.findByRole('button', { name: /Sonar way/ });
  await user.click(notDefaultQualityGate);
  await user.click(await screen.findByLabelText('actions'));
  const setAsDefaultButton = screen.getByRole('menuitem', { name: 'set_as_default' });
  await user.click(setAsDefaultButton);
  expect(await screen.findByRole('button', { name: /Sonar way default/ })).toBeInTheDocument();
});

it('should be able to add a condition on new code', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('SonarSource way - CFamily'));

  // On new code
  await user.click(await screen.findByText('quality_gates.add_condition'));

  const dialog = byRole('dialog');

  await user.click(dialog.byRole('radio', { name: 'quality_gates.conditions.new_code' }).get());

  await user.click(
    dialog.byRole('combobox', { name: 'quality_gates.conditions.fails_when' }).get(),
  );
  await user.click(dialog.byRole('option', { name: 'Issues' }).get());

  await user.click(
    await dialog.byRole('textbox', { name: 'quality_gates.conditions.value' }).find(),
  );
  await user.keyboard('12');
  await user.click(dialog.byRole('button', { name: 'quality_gates.add_condition' }).get());
  const newConditions = byTestId('quality-gates__conditions-new');
  expect(await newConditions.byRole('cell', { name: 'Issues' }).find()).toBeInTheDocument();
  expect(await newConditions.byRole('cell', { name: '12' }).find()).toBeInTheDocument();
});

it('should be able to add a condition on overall code', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('SonarSource way - CFamily'));

  // On overall code
  await user.click(await screen.findByText('quality_gates.add_condition'));

  const dialog = byRole('dialog');

  await user.click(dialog.byRole('radio', { name: 'quality_gates.conditions.overall_code' }).get());

  await user.click(
    dialog.byRole('combobox', { name: 'quality_gates.conditions.fails_when' }).get(),
  );

  // In real app there are no metrics with selectable condition operator
  // so we manually changed direction for Cognitive Complexity to 0 to test this behavior
  await user.click(await dialog.byRole('option', { name: 'Cognitive Complexity' }).find());

  await user.click(await dialog.byLabelText('quality_gates.conditions.operator').find());

  await user.click(dialog.byText('quality_gates.operator.LT').get());
  await user.click(dialog.byRole('textbox', { name: 'quality_gates.conditions.value' }).get());
  await user.keyboard('42');
  await user.click(dialog.byRole('button', { name: 'quality_gates.add_condition' }).get());

  const overallConditions = byTestId('quality-gates__conditions-overall');

  expect(
    await overallConditions.byRole('cell', { name: 'Cognitive Complexity' }).find(),
  ).toBeInTheDocument();
  expect(await overallConditions.byRole('cell', { name: '42' }).find()).toBeInTheDocument();
});

it('should be able to select a rating', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('SonarSource way - CFamily'));

  // Select a rating
  await user.click(await screen.findByText('quality_gates.add_condition'));

  const dialog = byRole('dialog');

  await user.click(dialog.byRole('radio', { name: 'quality_gates.conditions.overall_code' }).get());
  await user.click(
    dialog.byRole('combobox', { name: 'quality_gates.conditions.fails_when' }).get(),
  );
  await user.click(dialog.byRole('option', { name: 'Maintainability Rating' }).get());

  await user.click(dialog.byLabelText('quality_gates.conditions.value').get());
  await user.click(dialog.byText('B').get());
  await user.click(dialog.byRole('button', { name: 'quality_gates.add_condition' }).get());

  const overallConditions = byTestId('quality-gates__conditions-overall');

  expect(
    await overallConditions.byRole('cell', { name: /Maintainability Rating/ }).find(),
  ).toBeInTheDocument();
  expect(await overallConditions.byRole('cell', { name: 'B' }).find()).toBeInTheDocument();
});

it('should be able to edit a condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const newConditions = within(await screen.findByTestId('quality-gates__conditions-new'));

  await user.click(
    newConditions.getByLabelText('quality_gates.condition.edit.Line Coverage on New Code'),
  );
  const dialog = within(screen.getByRole('dialog'));
  const textBox = dialog.getByRole('textbox', { name: 'quality_gates.conditions.value' });
  await user.clear(textBox);
  await user.type(textBox, '23');
  await user.click(dialog.getByRole('button', { name: 'quality_gates.update_condition' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

  expect(await newConditions.findByText('Line Coverage')).toBeInTheDocument();
  expect(
    await within(await screen.findByTestId('quality-gates__conditions-new')).findByText('23.0%'),
  ).toBeInTheDocument();
});

it('should be able to handle duplicate or deprecated condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(
    // make it a regexp to ignore badges:
    await screen.findByRole('button', {
      name: new RegExp(qualityGateHandler.getCorruptedQualityGateName()),
    }),
  );

  expect(await screen.findByText('quality_gates.duplicated_conditions')).toBeInTheDocument();
  expect(
    await screen.findByRole('cell', { name: 'Complexity / Function deprecated' }),
  ).toBeInTheDocument();
});

it('should be able to handle delete condition', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('Non Cayc QG'));
  const newConditions = within(await screen.findByTestId('quality-gates__conditions-new'));

  await user.click(
    newConditions.getByLabelText('quality_gates.condition.delete.Coverage on New Code'),
  );

  const dialog = within(screen.getByRole('alertdialog'));
  await user.click(dialog.getByRole('button', { name: 'delete' }));

  await waitFor(() => {
    expect(newConditions.queryByRole('cell', { name: 'Coverage' })).not.toBeInTheDocument();
  });
});

it('should explain condition on branch', async () => {
  renderQualityGateApp({ featureList: [Feature.BranchSupport] });

  expect(
    await screen.findByText('quality_gates.conditions.new_code.description'),
  ).toBeInTheDocument();
  expect(
    await screen.findByText('quality_gates.conditions.overall_code.description'),
  ).toBeInTheDocument();
});

it('should show warning banner when CaYC condition is not properly set and should be able to update them', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const qualityGate = await screen.findByText('SonarSource way - CFamily');

  await user.click(qualityGate);

  expect(await screen.findByText('quality_gates.cayc_missing.banner.title')).toBeInTheDocument();
  expect(screen.getByText('quality_gates.cayc_missing.banner.description')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'quality_gates.cayc_condition.review_update' }),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('button', { name: 'quality_gates.cayc_condition.review_update' }),
  );
  expect(
    screen.getByRole('dialog', {
      name: 'quality_gates.cayc.review_update_modal.header.SonarSource way - CFamily',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByText('quality_gates.cayc.review_update_modal.description1'),
  ).toBeInTheDocument();
  expect(
    screen.getByText('quality_gates.cayc.review_update_modal.description2'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'quality_gates.cayc.review_update_modal.confirm_text' }),
  ).toBeInTheDocument();

  qualityGateHandler.setCaycStatusForQualityGate('SonarSource way - CFamily', CaycStatus.Compliant);

  await user.click(
    screen.getByRole('button', { name: 'quality_gates.cayc.review_update_modal.confirm_text' }),
  );

  expect(await screen.findByText('quality_gates.cayc.banner.title')).toBeInTheDocument();

  const overallConditionsWrapper = within(
    await screen.findByTestId('quality-gates__conditions-overall'),
  );
  expect(overallConditionsWrapper.getByText('Complexity / Function')).toBeInTheDocument();
});

it('should show optimize banner when CaYC condition is not properly set and QG is compliant and should be able to update them', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const qualityGate = await screen.findByText('Non Cayc Compliant QG');

  await user.click(qualityGate);

  expect(await screen.findByText('quality_gates.cayc_optimize.banner.title')).toBeInTheDocument();
  expect(screen.getByText('quality_gates.cayc_optimize.banner.description')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'quality_gates.cayc_condition.review_optimize' }),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('button', { name: 'quality_gates.cayc_condition.review_optimize' }),
  );
  expect(
    screen.getByRole('dialog', {
      name: 'quality_gates.cayc.review_optimize_modal.header.Non Cayc Compliant QG',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByText('quality_gates.cayc.review_optimize_modal.description1'),
  ).toBeInTheDocument();
  expect(
    screen.getByText('quality_gates.cayc.review_update_modal.description2'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'quality_gates.cayc.review_optimize_modal.confirm_text' }),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('button', { name: 'quality_gates.cayc.review_optimize_modal.confirm_text' }),
  );
});

it('should not warn user when quality gate is not CaYC compliant and user has no permission to edit it', async () => {
  const user = userEvent.setup();
  renderQualityGateApp();

  const nonCompliantQualityGate = await screen.findByRole('button', { name: 'Non Cayc QG' });

  await user.click(nonCompliantQualityGate);

  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

it('should not show optimize banner when quality gate is compliant but non-CaYC and user has no permission to edit it', async () => {
  const user = userEvent.setup();
  renderQualityGateApp();

  const nonCompliantQualityGate = await screen.findByRole('button', {
    name: 'Non Cayc Compliant QG',
  });

  await user.click(nonCompliantQualityGate);

  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

it('should warn user when quality gate is not CaYC compliant and user has permission to edit it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const nonCompliantQualityGate = await screen.findByRole('button', { name: /Non Cayc QG/ });

  await user.click(nonCompliantQualityGate);

  expect(await screen.findByText(/quality_gates.cayc_missing.banner.title/)).toBeInTheDocument();
});

it('should show optimize banner when quality gate is compliant but non-CaYC and user has permission to edit it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  const nonCompliantQualityGate = await screen.findByRole('button', {
    name: /Non Cayc Compliant QG/,
  });

  await user.click(nonCompliantQualityGate);

  expect(await screen.findByText(/quality_gates.cayc_optimize.banner.title/)).toBeInTheDocument();
});

it('should render CaYC conditions on a separate table if Sonar way', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();
  await user.click(await screen.findByText('Sonar way'));

  expect(screen.queryByText('quality_gates.cayc.banner.title')).not.toBeInTheDocument();
  expect(
    await screen.findByRole('list', { name: 'quality_gates.cayc.condition_simplification_list' }),
  ).toBeInTheDocument();
});

it('should display CaYC condition simplification tour for users who didnt dismissed it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp({ currentUser: mockLoggedInUser() });

  const qualityGate = await screen.findByText('Sonar way');

  await user.click(qualityGate);

  expect(await byRole('alertdialog').find()).toBeInTheDocument();

  expect(
    byRole('alertdialog')
      .byText('quality_gates.cayc.condition_simplification_tour.page_1.title')
      .get(),
  ).toBeInTheDocument();

  await user.click(byRole('alertdialog').byRole('button', { name: 'next' }).get());

  expect(
    byRole('alertdialog')
      .byText('quality_gates.cayc.condition_simplification_tour.page_2.title')
      .get(),
  ).toBeInTheDocument();

  await user.click(byRole('alertdialog').byRole('button', { name: 'next' }).get());

  expect(
    byRole('alertdialog')
      .byText('quality_gates.cayc.condition_simplification_tour.page_3.title')
      .get(),
  ).toBeInTheDocument();

  await user.click(byRole('alertdialog').byRole('button', { name: 'dismiss' }).get());

  expect(byRole('alertdialog').query()).not.toBeInTheDocument();
  expect(dismissNotice).toHaveBeenLastCalledWith(NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION);
});

it('should not display CaYC condition simplification tour for users who dismissed it', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp({
    currentUser: mockLoggedInUser({
      dismissedNotices: { [NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION]: true },
    }),
  });

  const qualityGate = await screen.findByText('Sonar way');

  await user.click(qualityGate);

  expect(byRole('alertdialog').query()).not.toBeInTheDocument();
});

it('should advertise the Sonar way Quality Gate as AI-ready', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp({
    currentUser: mockLoggedInUser({
      dismissedNotices: { [NoticeType.QG_CAYC_CONDITIONS_SIMPLIFICATION]: true },
    }),
    featureList: [Feature.AiCodeAssurance],
  });

  await user.click(await screen.findByRole('link', { name: /Sonar way/ }));

  expect(
    await screen.findByRole('link', {
      name: 'quality_gates.ai_generated.description.clean_ai_generated_code',
    }),
  ).toBeInTheDocument();
});

it('should not allow to change value of prioritized_rule_issues', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp({ featureList: [Feature.PrioritizedRules] });

  await user.click(await screen.findByText('SonarSource way - CFamily'));

  await user.click(await screen.findByText('quality_gates.add_condition'));

  const dialog = byRole('dialog');

  await user.click(dialog.byRole('radio', { name: 'quality_gates.conditions.overall_code' }).get());
  await user.click(
    dialog.byRole('combobox', { name: 'quality_gates.conditions.fails_when' }).get(),
  );
  await user.click(dialog.byRole('option', { name: 'Issues from prioritized rules' }).get());

  expect(dialog.byRole('textbox', { name: 'quality_gates.conditions.value' }).get()).toBeDisabled();
  expect(dialog.byRole('textbox', { name: 'quality_gates.conditions.value' }).get()).toHaveValue(
    '0',
  );

  await user.click(dialog.byRole('button', { name: 'quality_gates.add_condition' }).get());

  const overallConditions = byTestId('quality-gates__conditions-overall');

  expect(
    await overallConditions.byRole('cell', { name: 'Issues from prioritized rules' }).find(),
  ).toBeInTheDocument();

  expect(
    byLabelText('quality_gates.condition.edit.Issues from prioritized rules').query(),
  ).not.toBeInTheDocument();
  expect(
    byLabelText('quality_gates.condition.delete.Issues from prioritized rules').get(),
  ).toBeInTheDocument();
});

it('should not allow to add prioritized_rule_issues condition if feature is not enabled', async () => {
  const user = userEvent.setup();
  qualityGateHandler.setIsAdmin(true);
  renderQualityGateApp();

  await user.click(await screen.findByText('SonarSource way - CFamily'));

  await user.click(await screen.findByText('quality_gates.add_condition'));

  const dialog = byRole('dialog');

  await user.click(dialog.byRole('radio', { name: 'quality_gates.conditions.overall_code' }).get());
  await user.click(dialog.byRole('combobox').get());
  expect(
    byRole('option', { name: 'Issues from prioritized rules' }).query(),
  ).not.toBeInTheDocument();
});

describe('The Project section', () => {
  it('should render list of projects correctly in different tabs', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');

    await user.click(notDefaultQualityGate);

    // by default it shows "selected" values
    expect(await screen.findAllByRole('checkbox')).toHaveLength(3);

    // change tabs to show deselected projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.without' }));
    expect(screen.getAllByRole('checkbox')).toHaveLength(3);

    // change tabs to show all projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.all' }));
    expect(screen.getAllByRole('checkbox')).toHaveLength(6);
  });

  it('should handle select and deselect correctly', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');

    await user.click(notDefaultQualityGate);

    expect(await screen.findAllByRole('checkbox')).toHaveLength(3);
    const checkedProjects = screen.getAllByRole('checkbox')[0];
    await user.click(checkedProjects);
    const reloadButton = screen.getByRole('button', { name: 'reload' });
    expect(reloadButton).toBeInTheDocument();
    await user.click(reloadButton);

    // FP
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(screen.getAllByRole('checkbox')).toHaveLength(2);

    // projects with disabled as true are not selectable
    // last checked project in mock service is disabled
    const disabledCheckedProjects = screen.getByRole('checkbox', {
      name: 'test5 test5 quality_gates.projects.ai_assured_message',
    });
    expect(disabledCheckedProjects).toBeDisabled();

    // change tabs to show deselected projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.without' }));

    const uncheckedProjects = screen.getAllByRole('checkbox')[0];
    expect(screen.getAllByRole('checkbox')).toHaveLength(4);
    await user.click(uncheckedProjects);
    expect(reloadButton).toBeInTheDocument();
    await user.click(reloadButton);
    expect(screen.getAllByRole('checkbox')).toHaveLength(3);

    // projects with disabled as true are not selectable
    // last unchecked project in mock service is disabled
    const disabledUncheckedProjects = screen.getByRole('checkbox', {
      name: 'test6 test6 quality_gates.projects.ai_assured_message',
    });
    expect(disabledUncheckedProjects).toBeDisabled();

    // change tabs to show all projects
    await user.click(screen.getByRole('radio', { name: 'quality_gates.projects.all' }));
    expect(screen.getAllByRole('checkbox')).toHaveLength(6);

    const disabledCheckedProjectsAll = screen.getByRole('checkbox', {
      name: 'test5 test5 quality_gates.projects.ai_assured_message',
    });
    expect(disabledCheckedProjectsAll).toBeDisabled();

    const disabledUncheckedProjectsAll = screen.getByRole('checkbox', {
      name: 'test6 test6 quality_gates.projects.ai_assured_message',
    });
    expect(disabledUncheckedProjectsAll).toBeDisabled();
  });

  it('should handle the search of projects', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');

    await user.click(notDefaultQualityGate);

    const searchInput = await screen.findByRole('searchbox', { name: 'search_verb' });
    expect(searchInput).toBeInTheDocument();
    await user.click(searchInput);
    await user.keyboard('test2{Enter}');

    // FP
    // eslint-disable-next-line jest-dom/prefer-in-document
    expect(screen.getAllByRole('checkbox')).toHaveLength(1);
  });

  it('should display show more button if there are multiple pages of data', async () => {
    jest.mocked(searchProjects).mockResolvedValueOnce({
      paging: { pageIndex: 2, pageSize: 3, total: 55 },
      results: [],
    });

    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const notDefaultQualityGate = await screen.findByText('SonarSource way - CFamily');
    await user.click(notDefaultQualityGate);

    expect(await screen.findByRole('button', { name: 'show_more' })).toBeInTheDocument();
  });
});

describe('The Permissions section', () => {
  it('should not show button to grant permission when user is not admin', async () => {
    renderQualityGateApp();

    // await just to make sure we've loaded the page
    expect(
      await screen.findByRole('button', {
        name: `${qualityGateHandler.getDefaultQualityGate().name} default`,
      }),
    ).toBeInTheDocument();

    expect(screen.queryByText('quality_gates.permissions')).not.toBeInTheDocument();
  });
  it('should show button to grant permission when user is admin', async () => {
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    expect(screen.getByText('quality_gates.permissions')).toBeInTheDocument();
    expect(grantPermissionButton).toBeInTheDocument();
  });

  it('should assign permission to a user and delete it later', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    expect(screen.queryByText('userlogin')).not.toBeInTheDocument();

    // Granting permission to a user
    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    await user.click(grantPermissionButton);
    const popup = screen.getByRole('dialog');
    const searchUserInput = within(popup).getByRole('combobox', {
      name: 'quality_gates.permissions.search',
    });
    expect(searchUserInput).toBeInTheDocument();
    const addUserButton = screen.getByRole('button', {
      name: 'add_verb',
    });
    expect(addUserButton).toBeDisabled();
    await user.click(searchUserInput);
    await user.click(screen.getByRole('option', { name: 'userlogin' }));
    expect(addUserButton).toBeEnabled();
    await user.click(addUserButton);
    expect(screen.getByText('userlogin')).toBeInTheDocument();

    // Cancel granting permission
    await user.click(grantPermissionButton);
    await user.click(searchUserInput);
    await user.keyboard('test{Enter}');

    const cancelButton = screen.getByRole('button', {
      name: 'cancel',
    });
    await user.click(cancelButton);

    const permissionList = within(await screen.findByTestId('quality-gate-permissions'));
    expect(permissionList.getByRole('row')).toBeInTheDocument();

    // Delete the user permission
    const deleteButton = screen.getByTestId('permission-delete-button');
    await user.click(deleteButton);
    const deletePopup = screen.getByRole('dialog');
    const dialogDeleteButton = within(deletePopup).getByRole('button', { name: 'remove' });
    await user.click(dialogDeleteButton);
    expect(permissionList.queryByRole('row')).not.toBeInTheDocument();
  });

  it('should assign permission to a group and delete it later', async () => {
    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    expect(screen.queryByText('userlogin')).not.toBeInTheDocument();

    // Granting permission to a group
    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    await user.click(grantPermissionButton);
    const popup = screen.getByRole('dialog');
    const searchUserInput = within(popup).getByRole('combobox', {
      name: 'quality_gates.permissions.search',
    });
    const addUserButton = screen.getByRole('button', {
      name: 'add_verb',
    });
    await user.click(searchUserInput);
    await user.click(within(popup).getByRole('option', { name: 'Foo Foo' }));
    await user.click(addUserButton);
    expect(screen.getByText('Foo')).toBeInTheDocument();

    // Delete the group permission
    const deleteButton = screen.getByTestId('permission-delete-button');
    await user.click(deleteButton);
    const deletePopup = screen.getByRole('dialog');
    const dialogDeleteButton = within(deletePopup).getByRole('button', { name: 'remove' });
    await user.click(dialogDeleteButton);
    const permissionList = within(await screen.findByTestId('quality-gate-permissions'));
    expect(permissionList.queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('should handle searchUser service failure', async () => {
    jest.mocked(searchUsers).mockRejectedValue('error');

    const user = userEvent.setup();
    qualityGateHandler.setIsAdmin(true);
    renderQualityGateApp();

    const grantPermissionButton = await screen.findByRole('button', {
      name: 'quality_gates.permissions.grant',
    });
    await user.click(grantPermissionButton);
    const popup = screen.getByRole('dialog');
    const searchUserInput = within(popup).getByRole('combobox', {
      name: 'quality_gates.permissions.search',
    });
    await user.click(searchUserInput);

    expect(screen.getByText('select.search.noMatches')).toBeInTheDocument();
  });
});

describe('Mode transition', () => {
  describe('MQR mode', () => {
    it('should not see that quality gates require updates if not an admin', async () => {
      const user = userEvent.setup();
      renderQualityGateApp();

      expect(await ui.listItem.findAll()).toHaveLength(9);
      expect(ui.requiresUpdateIndicator.query()).not.toBeInTheDocument();
      await user.click(ui.qualityGateListItem('SonarSource way default').get());
      expect(byText('quality_gates.cayc.banner.title').get()).toBeInTheDocument();
      expect(ui.batchUpdate.query()).not.toBeInTheDocument();
      expect(ui.singleUpdate.query()).not.toBeInTheDocument();
      expect(ui.standardBadge.query()).not.toBeInTheDocument();
    });

    it('should see that quality gates require updates if an admin', async () => {
      const user = userEvent.setup();
      qualityGateHandler.setIsAdmin(true);
      renderQualityGateApp();

      expect(await ui.listItem.findAll()).toHaveLength(9);
      expect(
        ui.qualityGateListItem('SonarSource way default').by(ui.requiresUpdateIndicator).get(),
      ).toBeInTheDocument();
      await user.click(ui.qualityGateListItem('SonarSource way default').get());
      expect(byText('quality_gates.cayc.banner.title').query()).not.toBeInTheDocument();
      expect(ui.batchUpdate.get()).toBeInTheDocument();
      expect(ui.singleUpdate.getAll()).toHaveLength(5);
      expect(ui.standardBadge.getAll()).toHaveLength(5);
    });

    it('should update conditions to MQR mode', async () => {
      const user = userEvent.setup();
      qualityGateHandler.setIsAdmin(true);
      renderQualityGateApp();

      expect(await ui.listItem.findAll()).toHaveLength(9);
      await user.click(ui.qualityGateListItem('SonarSource way default').get());

      await user.click(ui.batchUpdate.get());
      expect(ui.batchDialog.get()).toBeInTheDocument();
      // + 1 for headers
      expect(ui.batchDialog.by(ui.newConditionRow).getAll()).toHaveLength(4);
      expect(ui.batchDialog.by(ui.overallConditionRow).getAll()).toHaveLength(3);
      await user.click(ui.batchDialog.by(ui.cancelBtn).get());

      expect(ui.newConditionRow.getAt(7)).toHaveTextContent(
        'Reliability Ratingquality_gates.metric.standard_mode_short',
      );
      expect(ui.singleUpdate.get(ui.newConditionRow.getAt(7))).toBeInTheDocument();
      await user.click(ui.singleUpdate.get(ui.newConditionRow.getAt(7)));
      expect(ui.singleDialog.get()).toBeInTheDocument();
      expect(ui.singleDialog.get()).toHaveTextContent(
        'quality_gates.metric.standard_mode_longReliability Ratingquality_gates.metric.mqr_mode_longReliability Rating',
      );
      await user.click(ui.updateSingleBtn.get());

      expect(ui.singleUpdate.getAll()).toHaveLength(4);
      expect(ui.standardBadge.getAll()).toHaveLength(4);

      await user.click(ui.batchUpdate.get());
      expect(ui.batchDialog.get()).toBeInTheDocument();
      // + 1 for headers
      expect(ui.batchDialog.by(ui.newConditionRow).getAll()).toHaveLength(3);
      expect(ui.batchDialog.by(ui.overallConditionRow).getAll()).toHaveLength(3);
      expect(ui.batchDialog.by(ui.newConditionRow).getAt(1)).toHaveTextContent(
        'Maintainability RatingMaintainability Rating',
      );
      expect(ui.batchDialog.by(ui.overallConditionRow).getAt(1)).toHaveTextContent(
        'Reliability RatingReliability Rating',
      );
      await user.click(ui.batchDialog.by(ui.updateMetricsBtn).get());

      expect(byText('quality_gates.cayc.banner.title').get()).toBeInTheDocument();
      expect(ui.batchUpdate.query()).not.toBeInTheDocument();
      expect(ui.singleUpdate.query()).not.toBeInTheDocument();
      expect(ui.standardBadge.query()).not.toBeInTheDocument();
    });
  });

  describe('Standard mode', () => {
    beforeEach(() => {
      settingsHandler.set(SettingsKey.MQRMode, 'false');
    });

    it('should not see that quality gates require updates if not an admin', async () => {
      const user = userEvent.setup();
      renderQualityGateApp();

      expect(await ui.listItem.findAll()).toHaveLength(9);
      expect(ui.requiresUpdateIndicator.query()).not.toBeInTheDocument();
      await user.click(ui.qualityGateListItem('QG with MQR conditions').get());
      expect(ui.batchUpdate.query()).not.toBeInTheDocument();
      expect(ui.singleUpdate.query()).not.toBeInTheDocument();
      expect(ui.mqrBadge.query()).not.toBeInTheDocument();
    });

    it('should see that quality gates require updates if an admin', async () => {
      const user = userEvent.setup();
      qualityGateHandler.setIsAdmin(true);
      renderQualityGateApp();

      expect(await ui.listItem.findAll()).toHaveLength(9);
      expect(
        ui.qualityGateListItem('QG with MQR conditions').by(ui.requiresUpdateIndicator).get(),
      ).toBeInTheDocument();
      await user.click(ui.qualityGateListItem('QG with MQR conditions').get());
      expect(ui.batchUpdate.get()).toBeInTheDocument();
      expect(ui.singleUpdate.getAll()).toHaveLength(4);
      expect(ui.mqrBadge.getAll()).toHaveLength(4);
    });

    it('should update conditions to Standard mode', async () => {
      const user = userEvent.setup();
      qualityGateHandler.setIsAdmin(true);
      renderQualityGateApp();

      expect(await ui.listItem.findAll()).toHaveLength(9);
      await user.click(ui.qualityGateListItem('QG with MQR conditions').get());

      await user.click(ui.batchUpdate.get());
      expect(ui.batchDialog.get()).toBeInTheDocument();
      // + 1 for headers
      expect(ui.batchDialog.by(ui.newConditionRow).getAll()).toHaveLength(3);
      expect(ui.batchDialog.by(ui.overallConditionRow).getAll()).toHaveLength(3);
      await user.click(ui.batchDialog.by(ui.cancelBtn).get());

      expect(ui.newConditionRow.getAt(1)).toHaveTextContent(
        'Blocker Severity Issuesquality_gates.metric.mqr_mode_short',
      );
      expect(ui.singleUpdate.get(ui.newConditionRow.getAt(1))).toBeInTheDocument();
      await user.click(ui.singleUpdate.get(ui.newConditionRow.getAt(1)));
      expect(ui.singleDialog.get()).toBeInTheDocument();
      expect(ui.singleDialog.get()).toHaveTextContent(
        'quality_gates.metric.mqr_mode_longBlocker Severity Issuesquality_gates.metric.standard_mode_longBlocker Issues',
      );
      await user.click(ui.updateSingleBtn.get());

      expect(ui.singleUpdate.getAll()).toHaveLength(3);
      expect(ui.mqrBadge.getAll()).toHaveLength(3);

      await user.click(ui.batchUpdate.get());
      expect(ui.batchDialog.get()).toBeInTheDocument();
      // + 1 for headers
      expect(ui.batchDialog.by(ui.newConditionRow).getAll()).toHaveLength(2);
      expect(ui.batchDialog.by(ui.overallConditionRow).getAll()).toHaveLength(3);
      expect(ui.batchDialog.by(ui.newConditionRow).getAt(1)).toHaveTextContent(
        'High Severity IssuesCritical Issues',
      );
      expect(ui.batchDialog.by(ui.overallConditionRow).getAt(1)).toHaveTextContent(
        'Blocker and High Severity Accepted Issuesquality_gates.update_conditions.removed',
      );
      expect(ui.batchDialog.by(ui.overallConditionRow).getAt(2)).toHaveTextContent(
        'Security RatingSecurity Rating',
      );
      await user.click(ui.batchDialog.by(ui.updateMetricsBtn).get());

      expect(byText('quality_gates.cayc_missing.banner.title').get()).toBeInTheDocument();
      expect(ui.batchUpdate.query()).not.toBeInTheDocument();
      expect(ui.singleUpdate.query()).not.toBeInTheDocument();
      expect(ui.mqrBadge.query()).not.toBeInTheDocument();
    });
  });
});

function renderQualityGateApp(context?: RenderContext) {
  return renderAppRoutes('quality_gates', routes, context);
}
