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
import { fireEvent, screen, within } from '@testing-library/react';
import selectEvent from 'react-select-event';
import CodingRulesServiceMock, { RULE_TAGS_MOCK } from '../../../api/mocks/CodingRulesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { QP_2, RULE_1, RULE_10, RULE_9 } from '../../../api/mocks/data/ids';
import { CLEAN_CODE_CATEGORIES, SOFTWARE_QUALITIES } from '../../../helpers/constants';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
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

describe('Rules app list', () => {
  it('renders correctly', async () => {
    const { ui } = getPageObjects();
    renderCodingRulesApp();

    await ui.appLoaded();

    // Renders list
    rulesHandler
      .allRulesName()
      .forEach((name) => expect(ui.ruleListItemLink(name).get()).toBeInTheDocument());

    // Render clean code attributes.
    expect(
      ui.ruleCleanCodeAttributeCategory(CleanCodeAttributeCategory.Intentional).getAll().length,
    ).toBeGreaterThan(1);
    expect(ui.ruleSoftwareQuality(SoftwareQuality.Maintainability).getAll().length).toBeGreaterThan(
      1,
    );

    // Renders clean code categories and software qualities facets
    CLEAN_CODE_CATEGORIES.map(
      (category) => `issue.clean_code_attribute_category.${category}`,
    ).forEach((name) => expect(ui.facetItem(name).get()).toBeInTheDocument());

    SOFTWARE_QUALITIES.map((quality) => `software_quality.${quality}`).forEach((name) =>
      expect(ui.facetItem(name).get()).toBeInTheDocument(),
    );

    // Renders language facets
    ['JavaScript', 'Java', 'C'].forEach((name) =>
      expect(ui.facetItem(name).get()).toBeInTheDocument(),
    );

    // Other facets are collapsed
    [
      ui.tagsFacet,
      ui.repositoriesFacet,
      ui.severetiesFacet,
      ui.statusesFacet,
      ui.standardsFacet,
      ui.availableSinceFacet,
      ui.templateFacet,
      ui.qpFacet,
      ui.typeFacet,
    ].forEach((facet) => {
      expect(facet.get()).toHaveAttribute('aria-expanded', 'false');
    });
  });

  describe('filtering', () => {
    it('combine facet filters', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);

      // Filter by language facet
      await user.type(ui.facetSearchInput('search.search_for_languages').get(), 'ja');
      await user.click(ui.facetItem('JavaScript').get());
      expect(ui.getAllRuleListItems()).toHaveLength(2);
      // Clear language facet and search box, and filter by python language
      await user.clear(ui.facetSearchInput('search.search_for_languages').get());
      await user.click(ui.facetItem('Python').get());
      expect(ui.getAllRuleListItems()).toHaveLength(6);

      // Filter by date facet
      await user.click(await ui.availableSinceFacet.find());
      await user.click(screen.getByPlaceholderText('date'));
      const monthSelector = within(ui.dateInputMonthSelect.get()).getByRole('combobox');

      await user.click(monthSelector);
      await selectEvent.select(ui.dateInputMonthSelect.byRole('combobox').get(), 'Nov');

      const yearSelector = within(ui.dateInputYearSelect.get()).getByRole('combobox');

      await user.click(yearSelector);
      await selectEvent.select(ui.dateInputYearSelect.byRole('combobox').get(), '2022');
      await user.click(await screen.findByText('1', { selector: 'button' }));

      expect(ui.getAllRuleListItems()).toHaveLength(1);
    });

    it('filter by repository', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);

      // Filter by repository
      await user.click(ui.repositoriesFacet.get());
      await user.click(ui.facetItem('Repository 1').get());
      expect(ui.getAllRuleListItems()).toHaveLength(2);

      // Search second repository
      await user.type(ui.facetSearchInput('search.search_for_repositories').get(), 'y 2');
      await user.click(ui.facetItem('Repository 2').get());
      expect(ui.getAllRuleListItems()).toHaveLength(1);
    });

    it('filter by quality profile, tag and search by tag', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);

      // Filter by quality profile
      await user.click(ui.qpFacet.get());
      await user.click(ui.facetItem('QP Foo Java').get());
      expect(ui.getAllRuleListItems()).toHaveLength(1);

      // Filter by tag
      await user.click(ui.facetClear('clear-coding_rules.facet.qprofile').get()); // Clear quality profile facet
      await user.click(ui.tagsFacet.get());
      await user.click(ui.facetItem('awesome').get());
      expect(ui.getAllRuleListItems()).toHaveLength(5);

      // Search by tag
      await user.type(ui.facetSearchInput('search.search_for_tags').get(), 'te');
      expect(ui.facetItem('cute').get()).toHaveAttribute('aria-disabled', 'true');
    });

    it('filter by clean code category, software quality and severity', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);
      // Filter by clean code category
      await user.click(ui.facetItem('issue.clean_code_attribute_category.INTENTIONAL').get());

      expect(ui.getAllRuleListItems()).toHaveLength(10);

      // Filter by software quality
      await user.click(ui.facetItem('software_quality.MAINTAINABILITY').get());
      expect(ui.getAllRuleListItems()).toHaveLength(10);

      // Filter by severity
      await user.click(ui.severetiesFacet.get());
      await user.click(ui.facetItem(/severity.HIGH/).get());
      expect(ui.getAllRuleListItems()).toHaveLength(9);
    });

    it('filter by standards', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);
      await user.click(ui.standardsFacet.get());
      await user.click(ui.facetItem('Buffer Overflow').get());
      expect(ui.getAllRuleListItems()).toHaveLength(6);

      await user.click(ui.standardsOwasp2021Top10Facet.get());
      await user.click(ui.facetItem('A2 - Cryptographic Failures').get());
      await user.click(ui.standardsOwasp2021Top10Facet.get()); // Close facet
      expect(ui.getAllRuleListItems()).toHaveLength(5);

      await user.click(ui.standardsOwasp2017Top10Facet.get());
      await user.click(ui.facetItem('A3 - Sensitive Data Exposure').get());
      expect(ui.getAllRuleListItems()).toHaveLength(4);

      await user.click(ui.standardsCweFacet.get());
      await user.click(ui.facetItem('CWE-102 - Struts: Duplicate Validation Forms').get());
      expect(ui.getAllRuleListItems()).toHaveLength(3);

      await user.type(ui.facetSearchInput('search.search_for_cwe').get(), 'Certificate');
      await user.click(
        ui.facetItem('CWE-297 - Improper Validation of Certificate with Host Mismatch').get(),
      );
      expect(ui.getAllRuleListItems()).toHaveLength(2);

      await user.click(ui.facetClear('clear-issues.facet.standards').get());
      expect(ui.getAllRuleListItems()).toHaveLength(11);
    });

    it('filters by search', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      await user.type(ui.searchInput.get(), 'Python');
      expect(ui.getAllRuleListItems()).toHaveLength(4);

      await user.clear(ui.searchInput.get());
      await user.type(ui.searchInput.get(), 'Hot hotspot');
      expect(ui.getAllRuleListItems()).toHaveLength(1);
    });
  });

  describe('bulk change', () => {
    it('no quality profile for bulk change based on language search', async () => {
      const { ui, user } = getPageObjects();
      rulesHandler.setIsAdmin();
      renderCodingRulesApp(mockLoggedInUser());
      await ui.appLoaded();

      await user.click(ui.facetItem('C').get());
      await user.click(ui.bulkChangeButton.get());
      await user.click(ui.activateIn.get());

      const dialog = ui.bulkChangeDialog(1);
      expect(dialog.get()).toBeInTheDocument();

      await user.click(ui.activateInSelect.get());

      expect(ui.noQualityProfiles.get(dialog.get())).toBeInTheDocument();
    });

    it('should be able to bulk activate quality profile', async () => {
      const { ui, user } = getPageObjects();
      rulesHandler.setIsAdmin();
      renderCodingRulesApp(mockLoggedInUser());
      await ui.appLoaded();

      const [selectQPSuccess, selectQPWarning] = rulesHandler.allQualityProfile('java');

      const rulesCount = rulesHandler.allRulesCount();

      await ui.bulkActivate(rulesCount, selectQPSuccess);

      expect(
        ui.bulkSuccessMessage(selectQPSuccess.name, selectQPSuccess.languageName, rulesCount).get(),
      ).toBeInTheDocument();

      await user.click(ui.bulkClose.get());

      // Try bulk change when quality profile has warnning.
      rulesHandler.activateWithWarning();

      await ui.bulkActivate(rulesCount, selectQPWarning);
      expect(
        ui
          .bulkWarningMessage(selectQPWarning.name, selectQPWarning.languageName, rulesCount - 1)
          .get(),
      ).toBeInTheDocument();
    });

    it('should be able to bulk deactivate quality profile', async () => {
      const { ui } = getPageObjects();
      rulesHandler.setIsAdmin();
      renderCodingRulesApp(mockLoggedInUser());
      await ui.appLoaded();

      const [selectQP] = rulesHandler.allQualityProfile('java');
      const rulesCount = rulesHandler.allRulesCount();

      await ui.bulkDeactivate(rulesCount, selectQP);

      expect(
        ui.bulkSuccessMessage(selectQP.name, selectQP.languageName, rulesCount).get(),
      ).toBeInTheDocument();
    });
  });

  it('can activate/change/deactivate specific rule for quality profile', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), undefined, [Feature.PrioritizedRules]);
    await ui.appLoaded();

    await user.click(ui.qpFacet.get());
    await user.click(ui.facetItem('QP Bar Python').get());

    // Only 4 rules are activated in selected QP
    expect(ui.getAllRuleListItems()).toHaveLength(4);

    // Switch to inactive rules
    await user.click(ui.qpInactiveRadio.get(ui.facetItem('QP Bar Python').get()));
    expect(ui.getAllRuleListItems()).toHaveLength(2);
    expect(ui.activateButton.getAll()).toHaveLength(2);
    expect(ui.changeButton(QP_2).query()).not.toBeInTheDocument();

    // Activate Rule for qp
    await user.click(ui.activateButton.getAll()[0]);
    expect(ui.selectValue.get(ui.activateQPDialog.get())).toHaveTextContent('severity.MAJOR');
    expect(ui.prioritizedSwitch.get(ui.activateQPDialog.get())).not.toBeChecked();
    await selectEvent.select(ui.oldSeveritySelect.get(), 'severity.MINOR');
    await user.click(ui.prioritizedSwitch.get(ui.activateQPDialog.get()));
    await user.click(ui.activateButton.get(ui.activateQPDialog.get()));

    expect(ui.activateButton.getAll()).toHaveLength(1);
    expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();
    expect(ui.deactivateButton.getAll()).toHaveLength(1);

    // Change Rule for qp
    await user.click(ui.changeButton('QP Bar').get());
    expect(ui.selectValue.get(ui.changeQPDialog.get())).toHaveTextContent('severity.MINOR');
    expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).toBeChecked();
    await selectEvent.select(ui.oldSeveritySelect.get(), 'severity.BLOCKER');
    await user.click(ui.prioritizedSwitch.get(ui.changeQPDialog.get()));
    await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

    // Check that new severity is saved
    await user.click(ui.changeButton('QP Bar').get());
    expect(ui.selectValue.get(ui.changeQPDialog.get())).toHaveTextContent('severity.BLOCKER');
    expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).not.toBeChecked();
    await user.click(ui.cancelButton.get(ui.changeQPDialog.get()));

    // Deactivate activated rule
    await user.click(ui.deactivateButton.get());
    await user.click(ui.yesButton.get());
    expect(ui.deactivateButton.query()).not.toBeInTheDocument();
    expect(ui.activateButton.getAll()).toHaveLength(2);
  });

  it('can revert to parent definition specific rule for quality profile', async () => {
    const { ui, user } = getPageObjects();
    settingsHandler.set(SettingsKey.QPAdminCanDisableInheritedRules, 'false');
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), undefined, [Feature.PrioritizedRules]);
    await ui.appLoaded();

    await user.click(ui.qpFacet.get());
    await user.click(ui.facetItem('QP Bar Python').get());

    // Only 4 rules are activated in selected QP
    expect(ui.getAllRuleListItems()).toHaveLength(4);

    // 3 rules have deactivate button and 1 rule has revert to parent definition button
    expect(ui.deactivateButton.getAll()).toHaveLength(3);
    expect(ui.revertToParentDefinitionButton.get()).toBeInTheDocument();

    await user.type(ui.searchInput.get(), RULE_10);

    // Only 1 rule left after search
    expect(ui.getAllRuleListItems()).toHaveLength(1);
    expect(ui.revertToParentDefinitionButton.get()).toBeInTheDocument();
    expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();

    // Check that severity is reflected correctly
    await user.click(ui.changeButton('QP Bar').get());
    expect(ui.selectValue.get(ui.changeQPDialog.get())).toHaveTextContent('severity.MAJOR');
    expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).toBeChecked();
    await user.click(ui.cancelButton.get(ui.changeQPDialog.get()));

    await user.click(ui.revertToParentDefinitionButton.get());
    await user.click(ui.yesButton.get());

    expect(ui.getAllRuleListItems()).toHaveLength(1);
    expect(ui.revertToParentDefinitionButton.query()).not.toBeInTheDocument();
    expect(ui.deactivateButton.get()).toBeInTheDocument();
    expect(ui.deactivateButton.get()).toBeDisabled();
    expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();

    // Check that severity is reflected correctly
    await user.click(ui.changeButton('QP Bar').get());
    expect(ui.selectValue.get(ui.changeQPDialog.get())).toHaveTextContent('severity.MINOR');
    expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).not.toBeChecked();
    await user.click(ui.cancelButton.get(ui.changeQPDialog.get()));
  });

  it('should not make rule overriden if no changes were done', async () => {
    const { ui, user } = getPageObjects();
    settingsHandler.set(SettingsKey.QPAdminCanDisableInheritedRules, 'false');
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), undefined, [Feature.PrioritizedRules]);
    await ui.appLoaded();

    await user.click(ui.qpFacet.get());
    await user.click(ui.facetItem('QP Bar Python').get());

    // filter out everything except INHERITED rule
    await user.type(ui.searchInput.get(), RULE_9);

    // Only 1 rule left after search
    expect(ui.getAllRuleListItems()).toHaveLength(1);
    expect(ui.deactivateButton.get()).toBeInTheDocument();
    expect(ui.deactivateButton.get()).toBeDisabled();
    expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();

    // Check that severity is reflected correctly
    await user.click(ui.changeButton('QP Bar').get());
    expect(ui.selectValue.get(ui.changeQPDialog.get())).toHaveTextContent('severity.MAJOR');
    expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).not.toBeChecked();
    await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

    expect(ui.revertToParentDefinitionButton.query()).not.toBeInTheDocument();
  });

  it('should not show prioritized rule switcher if feature is not enabled', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser());
    await ui.appLoaded();

    await user.click(ui.qpFacet.get());
    await user.click(ui.facetItem('QP Bar Python').get());

    await user.click(ui.changeButton('QP Bar').getAll()[0]);
    expect(ui.prioritizedSwitch.query()).not.toBeInTheDocument();
  });

  it('can not deactivate rules for quality profile if setting is false', async () => {
    const { ui } = getPageObjects();
    rulesHandler.setIsAdmin();
    settingsHandler.set(SettingsKey.QPAdminCanDisableInheritedRules, 'false');
    renderCodingRulesApp(
      mockLoggedInUser(),
      'coding_rules?activation=true&tags=cute&qprofile=' + QP_2,
    );
    await ui.appLoaded();

    // Only rule 9 is shown (inherited, activated)
    expect(ui.getAllRuleListItems()).toHaveLength(1);
    expect(ui.deactivateButton.get()).toBeDisabled();
  });

  it('navigates by keyboard', async () => {
    const { user, ui } = getPageObjects();
    renderCodingRulesApp();
    await ui.appLoaded();

    expect(
      ui.ruleListItemLink('Awsome java rule').get(ui.currentListItem.get()),
    ).toBeInTheDocument();

    await user.keyboard('{ArrowDown}');
    expect(ui.ruleListItemLink('Hot hotspot').get(ui.currentListItem.get())).toBeInTheDocument();

    await user.keyboard('{ArrowUp}');
    expect(
      ui.ruleListItemLink('Awsome java rule').get(ui.currentListItem.get()),
    ).toBeInTheDocument();

    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('heading', { level: 1, name: 'Awsome java rule' })).toBeInTheDocument();

    await user.keyboard('{ArrowLeft}');
    expect(
      ui.ruleListItemLink('Awsome java rule').get(ui.currentListItem.get()),
    ).toBeInTheDocument();
  });
});

describe('Rule app details', () => {
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
  });

  it('can activate/change/deactivate rule in quality profile', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule1', [Feature.PrioritizedRules]);
    await ui.detailsloaded();
    expect(ui.qpLink('QP Foo').get()).toBeInTheDocument();

    // Activate rule in quality profile
    expect(ui.prioritizedYesCell.query()).not.toBeInTheDocument();
    await user.click(ui.activateButton.get());
    await selectEvent.select(ui.qualityProfileSelect.get(), 'QP FooBar');
    await user.click(ui.prioritizedSwitch.get());
    await user.click(ui.activateButton.get(ui.activateQPDialog.get()));
    expect(ui.qpLink('QP FooBar').get()).toBeInTheDocument();
    expect(ui.prioritizedYesCell.get()).toBeInTheDocument();

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
    await user.type(ui.paramInput('1').get(), 'New');
    await user.click(ui.saveButton.get(ui.changeQPDialog.get()));
    expect(screen.getByText('New')).toBeInTheDocument();

    // Revert rule details in quality profile
    await user.click(ui.revertToParentDefinitionButton.get());
    await user.click(ui.yesButton.get());
    expect(screen.queryByText('New')).not.toBeInTheDocument();

    // Deactivate rule in quality profile
    await user.click(ui.deactivateInQPButton('QP FooBar').get());
    await user.click(ui.yesButton.get());
    expect(ui.qpLink('QP FooBar').query()).not.toBeInTheDocument();
  });

  it('can deactivate an inherrited rule', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule1');
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
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule1');
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
});

describe('redirects', () => {
  it('should open with permalink', async () => {
    const { ui } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?rule_key=rule1');
    await ui.appLoaded();
    expect(ui.ruleListItemLink('Awsome java rule').get()).toBeInTheDocument();
    expect(ui.ruleListItemLink('Hot hotspot').query()).not.toBeInTheDocument();
  });

  it('should handle hash parameters', async () => {
    const { ui, user } = getPageObjects();

    renderCodingRulesApp(
      mockLoggedInUser(),
      'coding_rules#languages=c,js|types=BUG|cleanCodeAttributeCategories=INTENTIONAL',
    );
    expect(ui.facetItem('issue.clean_code_attribute_category.INTENTIONAL').get()).toBeChecked();

    await user.click(ui.typeFacet.get());
    expect(await ui.facetItem(/issue.type.BUG/).find()).toBeChecked();

    // Only 2 rules shown
    expect(screen.getByText('x_of_y_shown.2.2')).toBeInTheDocument();
  });
});
