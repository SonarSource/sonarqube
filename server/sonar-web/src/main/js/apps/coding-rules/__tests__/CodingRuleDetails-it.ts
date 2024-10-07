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
import { fireEvent, screen } from '@testing-library/react';
import CodingRulesServiceMock, { RULE_TAGS_MOCK } from '../../../api/mocks/CodingRulesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { RULE_1 } from '../../../api/mocks/data/ids';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { byRole } from '../../../sonar-aligned/helpers/testSelector';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';
import { Feature } from '../../../types/features';
import { SettingsKey } from '../../../types/settings';
import { getPageObjects, renderCodingRulesApp } from '../utils-tests';

const rulesHandler = new CodingRulesServiceMock();
const settingsHandler = new SettingsServiceMock();
afterEach(() => {
  rulesHandler.reset();
  settingsHandler.reset();
});

describe('rendering', () => {
  it('shows rule with default description section and params', async () => {
    const { ui } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?open=' + RULE_1);
    await ui.detailsloaded();
    expect(ui.ruleTitle('Awsome java rule').get()).toBeInTheDocument();
    expect(
      ui.ruleCleanCodeAttributeCategory(CleanCodeAttributeCategory.Intentional).get(),
    ).toBeInTheDocument();
    expect(ui.ruleCleanCodeAttribute(CleanCodeAttribute.Clear).get()).toBeInTheDocument();
    // 1 In Rule details + 1 in facet
    expect(ui.ruleSoftwareQuality(SoftwareQuality.Maintainability).getAll()).toHaveLength(2);
    expect(document.title).toEqual('page_title.template.with_category.coding_rules.page');
    expect(screen.getByText('Why')).toBeInTheDocument();
    expect(screen.getByText('Because')).toBeInTheDocument();

    // Check params data
    expect(screen.getByText('html description for key 1')).toBeInTheDocument();
    expect(screen.getByText('default value for key 2')).toBeInTheDocument();
    expect(ui.softwareQualitiesSection.get()).toBeInTheDocument();
    expect(ui.cleanCodeAttributeSection.get()).toBeInTheDocument();
  });

  it('shows external rule', async () => {
    const { ui } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?open=rule6');
    await ui.detailsloaded();
    expect(ui.ruleTitle('Bad Python rule').get()).toBeInTheDocument();
    expect(ui.externalDescription('Bad Python rule').get()).toBeInTheDocument();
  });

  it('shows hotspot rule', async () => {
    const { ui, user } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?open=rule2');
    await ui.detailsloaded();
    expect(ui.ruleTitle('Hot hotspot').get()).toBeInTheDocument();
    expect(ui.introTitle.get()).toBeInTheDocument();
    expect(ui.softwareQualitiesSection.query()).not.toBeInTheDocument();
    expect(ui.cleanCodeAttributeSection.query()).not.toBeInTheDocument();

    // Shows correct tabs
    [ui.whatRiskTab, ui.assessTab, ui.moreInfoTab].forEach((tab) => {
      expect(tab.get()).toBeInTheDocument();
    });

    await user.click(ui.moreInfoTab.get());
    expect(ui.resourceLink.get()).toBeInTheDocument();
  });

  it('shows rule advanced section', async () => {
    const { ui } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?open=rule5');
    await ui.detailsloaded();
    expect(ui.ruleTitle('Awsome Python rule').get()).toBeInTheDocument();
    expect(ui.introTitle.get()).toBeInTheDocument();
    // Shows correct tabs
    [ui.howToFixTab, ui.moreInfoTab].forEach((tab) => {
      expect(tab.get()).toBeInTheDocument();
    });
  });

  it('shows rule advanced section with context', async () => {
    const { ui, user } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?open=rule7');
    await ui.detailsloaded();
    expect(ui.ruleTitle('Python rule with context').get()).toBeInTheDocument();

    await user.click(ui.howToFixTab.get());

    expect(ui.contextSubtitle('Spring').get()).toBeInTheDocument();
    expect(screen.getByText('This is how to fix for spring')).toBeInTheDocument();

    await user.click(ui.contextRadio('Spring boot').get());
    expect(ui.contextSubtitle('Spring boot').get()).toBeInTheDocument();
    expect(screen.getByText('This is how to fix for spring boot')).toBeInTheDocument();

    await user.click(ui.contextRadio('coding_rules.description_context.other').get());
    expect(ui.otherContextTitle.get()).toBeInTheDocument();
  });

  it('should show CYAC notification for rule advanced section and removes it after user`s visit', async () => {
    const { ui, user } = getPageObjects();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule10');
    await ui.detailsloaded();
    await user.click(ui.moreInfoTab.get());

    expect(ui.caycNotificationButton.get()).toBeInTheDocument();

    // navigate away and come back
    await user.click(ui.howToFixTab.get());
    await user.click(ui.moreInfoTab.get());

    expect(ui.caycNotificationButton.query()).not.toBeInTheDocument();
  });

  it('should show CaYC notification for rule advanced section and removes it when user scrolls to the principles', async () => {
    const { ui, user } = getPageObjects();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule10');
    await ui.detailsloaded();
    await user.click(ui.moreInfoTab.get());
    expect(ui.caycNotificationButton.get()).toBeInTheDocument();

    fireEvent.scroll(screen.getByText('coding_rules.more_info.education_principles.title'));

    // navigate away and come back
    await user.click(ui.howToFixTab.get());
    await user.click(ui.moreInfoTab.get());

    expect(ui.caycNotificationButton.query()).not.toBeInTheDocument();
  });

  it('should not show notification for anonymous users', async () => {
    const { ui, user } = getPageObjects();
    renderCodingRulesApp(mockCurrentUser(), 'coding_rules?open=rule10');

    await ui.detailsloaded();
    await user.click(ui.moreInfoTab.get());

    expect(ui.caycNotificationButton.query()).not.toBeInTheDocument();
  });

  it('should show customized severity and prioritized rule', async () => {
    const { ui, user } = getPageObjects();
    renderCodingRulesApp(mockCurrentUser(), 'coding_rules?open=rule10');

    await ui.detailsloaded();
    await user.click(ui.moreInfoTab.get());

    expect(ui.caycNotificationButton.query()).not.toBeInTheDocument();
  });
});

it('can activate/change/deactivate rule in quality profile', async () => {
  const { ui, user } = getPageObjects();
  rulesHandler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule1', [Feature.PrioritizedRules]);
  expect(await ui.qpLink('QP Foo').find()).toBeInTheDocument();

  // Activate rule in quality profile
  expect(ui.prioritizedRuleCell.query()).not.toBeInTheDocument();
  await user.click(ui.activateButton.get());

  await user.click(ui.prioritizedSwitch.get());
  await user.click(ui.mqrSwitch.get());
  await user.click(ui.newSeveritySelect(SoftwareQuality.Maintainability).get());
  await user.click(byRole('option', { name: 'severity_impact.LOW' }).get());
  await user.click(ui.activateButton.get(ui.activateQPDialog.get()));
  expect(ui.qpLink('QP FooBar').get()).toBeInTheDocument();
  expect(ui.prioritizedRuleCell.get()).toBeInTheDocument();
  expect(ui.oldSeverityCustomizedCell.query()).not.toBeInTheDocument();
  expect(ui.newSeverityCustomizedCell.get()).toBeInTheDocument();
  await expect(ui.newSeverityCustomizedCell.get()).toHaveATooltipWithContent(
    'coding_rules.impact_customized.detailsoftware_quality.MAINTAINABILITYseverity_impact.MEDIUMseverity_impact.LOW',
  );

  // Activate last java rule
  await user.click(ui.activateButton.get());
  await user.type(ui.paramInput('1').get(), 'paramInput');
  await user.click(ui.activateButton.get(ui.activateQPDialog.get()));
  expect(ui.qpLink('QP FooBarBaz').get()).toBeInTheDocument();
  expect(ui.qpLink('QP FooBaz').get()).toBeInTheDocument();

  // Rule is activated in all quality profiles - show notification in dialog
  await user.click(ui.activateButton.get(screen.getByRole('main')));
  expect(ui.activaInAllQPs.get()).toBeInTheDocument();
  expect(ui.activateButton.get(ui.activateQPDialog.get())).toBeDisabled();
  await user.click(ui.cancelButton.get());

  // Change rule details in quality profile
  await user.click(ui.changeButton('QP FooBaz').get());
  await user.clear(ui.paramInput('1').get());
  await user.type(ui.paramInput('1').get(), 'New');
  await user.click(ui.mqrSwitch.get());
  await user.click(ui.newSeveritySelect(SoftwareQuality.Maintainability).get());
  await user.click(byRole('option', { name: 'severity_impact.BLOCKER' }).get());
  await user.click(ui.saveButton.get(ui.changeQPDialog.get()));
  expect(await ui.qualityProfileRow.findAt(5)).toHaveTextContent('QP FooBaz');
  expect(ui.qualityProfileRow.getAt(5)).toHaveTextContent('New');
  await expect(
    ui.newSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(5)),
  ).toHaveATooltipWithContent(
    'coding_rules.impact_customized.detailsoftware_quality.MAINTAINABILITYseverity_impact.MEDIUMseverity_impact.BLOCKER',
  );

  // Revert rule details in quality profile
  await user.click(ui.revertToParentDefinitionButton.get());
  await user.click(ui.yesButton.get());
  expect(await ui.qualityProfileRow.findAt(5)).toHaveTextContent('QP FooBaz');
  expect(await ui.qualityProfileRow.findAt(5)).not.toHaveTextContent('New');
  expect(ui.newSeverityCustomizedCell.query(ui.qualityProfileRow.getAt(5))).not.toBeInTheDocument();

  // Deactivate rule in quality profile
  await user.click(ui.deactivateInQPButton('QP FooBar').get());
  await user.click(ui.yesButton.get());
  expect(ui.qpLink('QP FooBar').query()).not.toBeInTheDocument();
});

it('can activate/change/deactivate rule in quality profile for legacy mode', async () => {
  const { ui, user } = getPageObjects();
  settingsHandler.set(SettingsKey.MQRMode, 'true');
  rulesHandler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule1', [Feature.PrioritizedRules]);
  await ui.detailsloaded();
  expect(ui.qpLink('QP Foo').get()).toBeInTheDocument();

  // Activate rule in quality profile
  expect(ui.prioritizedRuleCell.query()).not.toBeInTheDocument();
  await user.click(ui.activateButton.get());

  await user.click(ui.prioritizedSwitch.get());
  await user.click(ui.oldSeveritySelect.get());
  await user.click(byRole('option', { name: 'severity.MINOR' }).get());
  await user.click(ui.activateButton.get(ui.activateQPDialog.get()));
  expect(ui.qpLink('QP FooBar').get()).toBeInTheDocument();
  expect(ui.prioritizedRuleCell.get()).toBeInTheDocument();
  expect(ui.newSeverityCustomizedCell.query()).not.toBeInTheDocument();
  expect(ui.oldSeverityCustomizedCell.get()).toBeInTheDocument();
  expect(ui.oldSeverityCustomizedCell.get()).toHaveTextContent('severity.MAJORseverity.MINOR');

  await user.click(ui.changeButton('QP FooBar').get());
  await user.click(ui.oldSeveritySelect.get());
  await user.click(
    byRole('option', { name: /coding_rules.custom_severity.severity_with_recommended/ }).get(),
  );
  await user.click(ui.saveButton.get(ui.changeQPDialog.get()));
  expect(ui.prioritizedRuleCell.get()).toBeInTheDocument();
  expect(ui.oldSeverityCustomizedCell.query()).not.toBeInTheDocument();
  expect(ui.newSeverityCustomizedCell.query()).not.toBeInTheDocument();

  // Activate last java rule
  await user.click(ui.activateButton.get());
  await user.type(ui.paramInput('1').get(), 'paramInput');
  await user.click(ui.activateButton.get(ui.activateQPDialog.get()));
  expect(ui.qpLink('QP FooBarBaz').get()).toBeInTheDocument();
  expect(ui.qpLink('QP FooBaz').get()).toBeInTheDocument();

  // Change rule details in quality profile
  await user.click(ui.changeButton('QP FooBaz').get());
  await user.click(ui.oldSeveritySelect.get());
  await user.click(byRole('option', { name: 'severity.BLOCKER' }).get());
  await user.click(ui.saveButton.get(ui.changeQPDialog.get()));
  expect(await ui.qualityProfileRow.findAt(5)).toHaveTextContent('QP FooBaz');
  expect(ui.oldSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(5))).toBeInTheDocument();
  expect(ui.oldSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(5))).toHaveTextContent(
    'severity.MAJORseverity.BLOCKER',
  );

  // Revert rule details in quality profile
  await user.click(ui.revertToParentDefinitionButton.get());
  await user.click(ui.yesButton.get());
  expect(await ui.qualityProfileRow.findAt(5)).toHaveTextContent('QP FooBaz');
  expect(ui.oldSeverityCustomizedCell.query(ui.qualityProfileRow.getAt(5))).not.toBeInTheDocument();
});

it('should show multiple customized severities', async () => {
  const { ui, user } = getPageObjects();
  rulesHandler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule10', [Feature.PrioritizedRules]);
  await ui.detailsloaded();

  expect(ui.newSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(1))).toBeInTheDocument();
  await expect(
    ui.newSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(1)),
  ).toHaveATooltipWithContent(
    'coding_rules.impact_customized.detailsoftware_quality.RELIABILITYseverity_impact.HIGHseverity_impact.INFO' +
      'coding_rules.impact_customized.detailsoftware_quality.MAINTAINABILITYseverity_impact.LOWseverity_impact.MEDIUM',
  );

  expect(ui.newSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(2))).toBeInTheDocument();
  await expect(
    ui.newSeverityCustomizedCell.get(ui.qualityProfileRow.getAt(2)),
  ).toHaveATooltipWithContent(
    'coding_rules.impact_customized.detailsoftware_quality.RELIABILITYseverity_impact.HIGHseverity_impact.BLOCKER',
  );

  await user.click(ui.changeButton('QP Bar').get());
  await user.click(ui.mqrSwitch.get());
  await user.click(ui.newSeveritySelect(SoftwareQuality.Reliability).get());
  await user.click(
    byRole('option', { name: /coding_rules.custom_severity.severity_with_recommended/ }).get(),
  );
  await user.click(ui.newSeveritySelect(SoftwareQuality.Maintainability).get());
  await user.click(
    byRole('option', { name: /coding_rules.custom_severity.severity_with_recommended/ }).get(),
  );
  await user.click(ui.saveButton.get(ui.changeQPDialog.get()));
  expect(ui.newSeverityCustomizedCell.query(ui.qualityProfileRow.getAt(1))).not.toBeInTheDocument();
});

it('can deactivate an inherrited rule', async () => {
  const { ui, user } = getPageObjects();
  rulesHandler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule11');
  await ui.detailsloaded();

  // Should show 2 deactivate buttons: one for the parent, one for the child profile.
  expect(ui.deactivateInQPButton('QP FooBarBaz').get()).toBeInTheDocument();
  expect(ui.deactivateInQPButton('QP FooBaz').get()).toBeInTheDocument();

  // Deactivate rule in inherited quality profile
  await user.click(ui.deactivateInQPButton('QP FooBaz').get());
  await user.click(ui.yesButton.get());
  expect(ui.qpLink('QP FooBaz').query()).not.toBeInTheDocument();
});

it('cannot deactivate an inherrited rule if the setting is false', async () => {
  const { ui } = getPageObjects();
  rulesHandler.setIsAdmin();
  settingsHandler.set(SettingsKey.QPAdminCanDisableInheritedRules, 'false');
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule11');
  await ui.detailsloaded();

  // Should show 1 deactivate button: one for the parent, none for the child profile.
  expect(ui.deactivateInQPButton('QP FooBarBaz').get()).toBeInTheDocument();
  expect(ui.deactivateInQPButton('QP FooBaz').query()).not.toBeInTheDocument();
});

it('can extend the rule description', async () => {
  const { ui, user } = getPageObjects();
  rulesHandler.setIsAdmin();
  renderCodingRulesApp(undefined, 'coding_rules?open=rule5');
  await ui.detailsloaded();
  expect(ui.ruleTitle('Awsome Python rule').get()).toBeInTheDocument();

  // Add
  await user.click(ui.extendDescriptionButton.get());
  await user.type(ui.extendDescriptionTextbox.get(), 'TEST DESC');
  await user.click(ui.saveButton.get());
  expect(await screen.findByText('TEST DESC')).toBeInTheDocument();

  // Edit
  await user.click(ui.extendDescriptionButton.get());
  await user.clear(ui.extendDescriptionTextbox.get());
  await user.type(ui.extendDescriptionTextbox.get(), 'NEW DESC');
  await user.click(ui.saveButton.get());
  expect(await screen.findByText('NEW DESC')).toBeInTheDocument();

  // Cancel
  await user.click(ui.extendDescriptionButton.get());
  await user.type(ui.extendDescriptionTextbox.get(), 'Difference');
  await user.click(ui.cancelButton.get());
  expect(await screen.findByText('NEW DESC')).toBeInTheDocument();

  // Remove
  await user.click(ui.extendDescriptionButton.get());
  await user.click(ui.removeButton.get());
  await user.click(ui.removeButton.get(screen.getByRole('dialog')));
  expect(screen.queryByText('NEW DESC')).not.toBeInTheDocument();
});

it('can set tags', async () => {
  const { ui, user } = getPageObjects();
  rulesHandler.setIsAdmin();
  renderCodingRulesApp(undefined, 'coding_rules?open=rule10');
  await ui.detailsloaded();

  await user.click(ui.tagsDropdown.get());

  RULE_TAGS_MOCK.forEach((tag) => {
    expect(ui.tagCheckbox(tag).get()).toBeInTheDocument();
  });

  expect(ui.tagCheckbox(RULE_TAGS_MOCK[0]).get()).toBeChecked();

  // Set tag
  await user.click(ui.tagCheckbox(RULE_TAGS_MOCK[1]).get());
  await user.keyboard('{Escape}');
  await expect(ui.tagsDropdown.byText('multi-threading').get()).toHaveATooltipWithContent(
    'multi-threading, awesome, cute',
  );

  await user.click(ui.tagsDropdown.get());

  // Search for specific tag
  await user.type(ui.tagSearch.get(), RULE_TAGS_MOCK[2]);
  expect(ui.tagCheckbox(RULE_TAGS_MOCK[2]).get()).toBeInTheDocument();
  expect(ui.tagCheckbox(RULE_TAGS_MOCK[1]).query()).not.toBeInTheDocument();
});
