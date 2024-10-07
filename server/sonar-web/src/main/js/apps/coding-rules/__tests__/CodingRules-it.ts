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
import { screen, within } from '@testing-library/react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import CodingRulesServiceMock from '../../../api/mocks/CodingRulesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { QP_2, RULE_10, RULE_7, RULE_9 } from '../../../api/mocks/data/ids';
import {
  IMPACT_SEVERITIES,
  ISSUE_TYPES,
  SEVERITIES,
  SOFTWARE_QUALITIES,
} from '../../../helpers/constants';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
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
    expect(ui.ruleSoftwareQuality(SoftwareQuality.Maintainability).getAll().length).toBeGreaterThan(
      1,
    );

    // Renders clean code categories and software qualities facets
    SOFTWARE_QUALITIES.map((quality) => `software_quality.${quality}`).forEach((name) =>
      expect(ui.facetItem(name).get()).toBeInTheDocument(),
    );
    IMPACT_SEVERITIES.map((severity) => `severity_impact.${severity}`).forEach((name) =>
      expect(ui.facetItem(name).get()).toBeInTheDocument(),
    );
    expect(
      ui.facetItem('coding_rules.facet.security_hotspots.show_only').get(),
    ).toBeInTheDocument();

    // Renders language facets
    ['JavaScript', 'Java', 'C'].forEach((name) =>
      expect(ui.facetItem(name).get()).toBeInTheDocument(),
    );

    // Other facets are collapsed
    [
      ui.tagsFacet,
      ui.cleanCodeCategoriesFacet,
      ui.repositoriesFacet,
      ui.statusesFacet,
      ui.standardsFacet,
      ui.availableSinceFacet,
      ui.templateFacet,
      ui.qpFacet,
    ].forEach((facet) => {
      expect(facet.get()).toHaveAttribute('aria-expanded', 'false');
    });

    // Standard facets are hidden
    [ui.standardSeveritiesFacet, ui.typeFacet].forEach((facet) => {
      expect(facet.query()).not.toBeInTheDocument();
    });
  });

  it('renders correctly in Standard mode', async () => {
    const { ui } = getPageObjects();
    settingsHandler.set(SettingsKey.MQRMode, 'false');
    renderCodingRulesApp();

    await ui.appLoaded();

    // Renders list
    rulesHandler
      .allRulesName()
      .forEach((name) => expect(ui.ruleListItemLink(name).get()).toBeInTheDocument());

    // Renders clean code categories and software qualities facets
    ISSUE_TYPES.map((type) => `issue.type.${type}`).forEach((name) =>
      expect(ui.facetItem(name).get()).toBeInTheDocument(),
    );
    SEVERITIES.map((severity) => `severity.${severity}`).forEach((name) =>
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
      ui.statusesFacet,
      ui.standardsFacet,
      ui.availableSinceFacet,
      ui.templateFacet,
      ui.qpFacet,
    ].forEach((facet) => {
      expect(facet.get()).toHaveAttribute('aria-expanded', 'false');
    });

    // MQR facets are hidden
    [
      ui.severetiesFacet,
      ui.securityHotspotFacet,
      ui.softwareQualitiesFacet,
      ui.cleanCodeCategoriesFacet,
    ].forEach((facet) => {
      expect(facet.query()).not.toBeInTheDocument();
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
      await user.click(ui.dateInputMonthSelect.byRole('combobox').get());
      await user.click(byRole('option', { name: 'Nov' }).get());

      const yearSelector = within(ui.dateInputYearSelect.get()).getByRole('combobox');

      await user.click(yearSelector);
      await user.click(ui.dateInputYearSelect.byRole('combobox').get());
      await user.click(byRole('option', { name: '2022' }).get());

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

    it('filter by quality profile, tag and search by tag, does not show prioritized rule', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);

      // Filter by quality profile
      await user.click(ui.qpFacet.get());
      await user.click(ui.facetItem('QP Foo Java').get());
      expect(ui.getAllRuleListItems()).toHaveLength(1);

      expect(ui.prioritizedRuleFacet.query()).not.toBeInTheDocument();

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
      await user.click(ui.cleanCodeCategoriesFacet.get());
      await user.click(
        await ui.facetItem('issue.clean_code_attribute_category.INTENTIONAL').find(),
      );

      expect(ui.getAllRuleListItems()).toHaveLength(9);

      // Filter by software quality
      await user.click(ui.facetItem('software_quality.MAINTAINABILITY').get());
      expect(ui.getAllRuleListItems()).toHaveLength(8);

      // Filter by severity
      await user.click(ui.facetItem(/severity_impact.HIGH/).get());
      expect(ui.getAllRuleListItems()).toHaveLength(4);
    });

    it('filter by type and severity in standard mode', async () => {
      const { ui, user } = getPageObjects();
      settingsHandler.set(SettingsKey.MQRMode, 'false');
      renderCodingRulesApp(mockCurrentUser());
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);
      // Filter by type
      await user.click(await ui.facetItem('issue.type.BUG').find());

      expect(ui.getAllRuleListItems()).toHaveLength(7);

      // Filter by severity
      await user.click(ui.facetItem(/severity.MAJOR/).get());
      expect(ui.getAllRuleListItems()).toHaveLength(5);
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

    it('filter by quality profileand prioritizedRule', async () => {
      const { ui, user } = getPageObjects();
      renderCodingRulesApp(mockCurrentUser(), undefined, [Feature.PrioritizedRules]);
      await ui.appLoaded();

      expect(ui.getAllRuleListItems()).toHaveLength(11);

      expect(ui.prioritizedRuleFacet.get()).toHaveAttribute('aria-disabled', 'true');

      // Filter by quality profile
      await user.click(ui.qpFacet.get());
      await user.click(ui.facetItem('QP Bar Python').get());
      expect(ui.getAllRuleListItems()).toHaveLength(4);

      // Filter by prioritized rule
      expect(ui.prioritizedRuleFacet.get()).not.toHaveAttribute('aria-disabled', 'true');
      await user.click(ui.prioritizedRuleFacet.get());
      await user.click(ui.facetItem('coding_rules.filters.prioritizedRule.true').get());
      expect(ui.getAllRuleListItems()).toHaveLength(1);

      // Filter by non-prioritized rule
      await user.click(ui.facetItem('coding_rules.filters.prioritizedRule.false').get());
      expect(ui.getAllRuleListItems()).toHaveLength(3);

      await user.click(ui.facetClear('clear-coding_rules.facet.prioritizedRule').get());
      expect(ui.getAllRuleListItems()).toHaveLength(4);
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

  describe('old severity', () => {
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
      expect(ui.oldSeveritySelect.get(ui.activateQPDialog.get())).toHaveValue(
        'coding_rules.custom_severity.severity_with_recommended.severity.MAJOR',
      );
      expect(ui.prioritizedSwitch.get(ui.activateQPDialog.get())).not.toBeChecked();
      await user.click(ui.oldSeveritySelect.get());
      await user.click(byRole('option', { name: 'severity.MINOR' }).get());
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MAJOR');

      await user.click(ui.prioritizedSwitch.get(ui.activateQPDialog.get()));
      await user.click(ui.activateButton.get(ui.activateQPDialog.get()));

      expect(ui.activateButton.getAll()).toHaveLength(1);
      expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();
      expect(ui.deactivateButton.getAll()).toHaveLength(1);

      // Change Rule for qp
      await user.click(ui.changeButton('QP Bar').get());
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue('severity.MINOR');
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MAJOR');
      expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).toBeChecked();
      await user.click(ui.oldSeveritySelect.get());
      await user.click(byRole('option', { name: 'severity.BLOCKER' }).get());
      await user.click(ui.prioritizedSwitch.get(ui.changeQPDialog.get()));
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MAJOR');
      await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

      // Check that new severity is saved
      await user.click(ui.changeButton('QP Bar').get());
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue('severity.BLOCKER');
      expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).not.toBeChecked();
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MAJOR');
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
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue('severity.MAJOR');
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MINOR');
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
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue(
        'coding_rules.custom_severity.severity_with_recommended.severity.MINOR',
      );
      expect(ui.notRecommendedSeverity.query()).not.toBeInTheDocument();
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
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue('severity.MAJOR');
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MINOR');
      expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).not.toBeChecked();
      await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

      expect(ui.revertToParentDefinitionButton.query()).not.toBeInTheDocument();
    });
  });

  describe('new severity', () => {
    it('can activate/change specific rule with multiple impacts for quality profile', async () => {
      const { ui, user } = getPageObjects();
      rulesHandler.setIsAdmin();
      renderCodingRulesApp(mockLoggedInUser(), undefined, []);
      await ui.appLoaded();

      await user.click(ui.qpFacet.get());
      await user.click(ui.facetItem('QP Bar Python').get());
      await user.click(ui.qpInactiveRadio.get(ui.facetItem('QP Bar Python').get()));

      // Activate Rule for qp
      await user.click(ui.activateButton.getAll()[1]);

      await user.click(ui.mqrSwitch.get(ui.activateQPDialog.get()));

      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'coding_rules.custom_severity.severity_with_recommended.severity_impact.MEDIUM',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toHaveValue(
        'coding_rules.custom_severity.severity_with_recommended.severity_impact.LOW',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toBeDisabled();
      await user.click(ui.newSeveritySelect(SoftwareQuality.Maintainability).get());
      await user.click(byRole('option', { name: 'severity_impact.LOW' }).get());
      await user.click(ui.newSeveritySelect(SoftwareQuality.Security).get());
      await user.click(byRole('option', { name: 'severity_impact.MEDIUM' }).get());
      expect(ui.notRecommendedSeverity.getAll()).toHaveLength(2);
      expect(ui.notRecommendedSeverity.getAt(0)).toHaveTextContent('severity_impact.LOW');
      expect(ui.notRecommendedSeverity.getAt(1)).toHaveTextContent('severity_impact.MEDIUM');

      await user.click(ui.activateButton.get(ui.activateQPDialog.get()));

      expect(ui.activateButton.getAll()).toHaveLength(1);
      expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();
      expect(ui.deactivateButton.getAll()).toHaveLength(1);

      await user.click(ui.changeButton('QP Bar').get());
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue('severity.MINOR');
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MAJOR');
      await user.click(ui.mqrSwitch.get());

      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'severity_impact.LOW',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toHaveValue(
        'severity_impact.MEDIUM',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toBeDisabled();
      expect(ui.notRecommendedSeverity.getAll()).toHaveLength(2);
      expect(ui.notRecommendedSeverity.getAt(0)).toHaveTextContent('severity_impact.LOW');
      expect(ui.notRecommendedSeverity.getAt(1)).toHaveTextContent('severity_impact.MEDIUM');
      await user.click(ui.newSeveritySelect(SoftwareQuality.Security).get());
      await user.click(byRole('option', { name: 'severity_impact.BLOCKER' }).get());
      expect(ui.notRecommendedSeverity.getAll()).toHaveLength(2);
      expect(ui.notRecommendedSeverity.getAt(0)).toHaveTextContent('severity_impact.LOW');
      expect(ui.notRecommendedSeverity.getAt(1)).toHaveTextContent('severity_impact.MEDIUM');
      await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

      // Check that new severity is saved
      await user.click(ui.changeButton('QP Bar').get());
      await user.click(ui.mqrSwitch.get());
      expect(ui.oldSeveritySelect.get(ui.changeQPDialog.get())).toHaveValue('severity.MINOR');
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity.MAJOR');
      await user.click(ui.mqrSwitch.get());

      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'severity_impact.LOW',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toHaveValue(
        'severity_impact.BLOCKER',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toBeDisabled();

      await user.click(ui.cancelButton.get(ui.changeQPDialog.get()));
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
      await user.click(ui.mqrSwitch.get(ui.changeQPDialog.get()));
      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'severity_impact.MEDIUM',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toHaveValue(
        'severity_impact.INFO',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toBeDisabled();
      expect(ui.notRecommendedSeverity.getAll()).toHaveLength(2);
      expect(ui.notRecommendedSeverity.getAt(0)).toHaveTextContent('severity_impact.HIGH');
      expect(ui.notRecommendedSeverity.getAt(1)).toHaveTextContent('severity_impact.LOW');
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
      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'coding_rules.custom_severity.severity_with_recommended.severity_impact.LOW',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toHaveValue(
        'severity_impact.BLOCKER',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toBeDisabled();
      expect(ui.notRecommendedSeverity.getAll()).toHaveLength(1);
      expect(ui.notRecommendedSeverity.getAt(0)).toHaveTextContent('severity_impact.HIGH');
      expect(ui.prioritizedSwitch.get(ui.changeQPDialog.get())).not.toBeChecked();
      await user.click(ui.cancelButton.get(ui.changeQPDialog.get()));
    });

    it('should not make rule overriden if no changes were done', async () => {
      const { ui, user } = getPageObjects();
      settingsHandler.set(SettingsKey.QPAdminCanDisableInheritedRules, 'false');
      rulesHandler.setIsAdmin();
      renderCodingRulesApp(mockLoggedInUser(), undefined, []);
      await ui.appLoaded();

      await user.click(ui.qpFacet.get());
      await user.click(ui.facetItem('QP Bar Python').get());

      // filter out everything except INHERITED rule
      await user.type(ui.searchInput.get(), RULE_9);
      expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();

      // Check that severity is reflected correctly
      await user.click(ui.changeButton('QP Bar').get());
      await user.click(ui.mqrSwitch.get(ui.changeQPDialog.get()));
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toHaveValue(
        'severity_impact.MEDIUM',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toBeDisabled();
      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toBeDisabled();
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity_impact.LOW');
      await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

      expect(ui.revertToParentDefinitionButton.query()).not.toBeInTheDocument();
    });

    it('should ignore excessive activation impacts', async () => {
      const { ui, user } = getPageObjects();
      settingsHandler.set(SettingsKey.QPAdminCanDisableInheritedRules, 'false');
      rulesHandler.setIsAdmin();
      renderCodingRulesApp(mockLoggedInUser(), undefined, []);
      await ui.appLoaded();

      await user.click(ui.qpFacet.get());
      await user.click(ui.facetItem('QP Bar Python').get());

      await user.type(ui.searchInput.get(), RULE_7);
      expect(ui.changeButton('QP Bar').get()).toBeInTheDocument();

      await user.click(ui.changeButton('QP Bar').get());
      await user.click(ui.mqrSwitch.get(ui.changeQPDialog.get()));
      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'severity_impact.MEDIUM',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toBeDisabled();
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toBeDisabled();
      expect(ui.notRecommendedSeverity.get()).toBeInTheDocument();
      expect(ui.notRecommendedSeverity.get()).toHaveTextContent('severity_impact.LOW');
      await user.click(ui.newSeveritySelect(SoftwareQuality.Maintainability).get());
      await user.click(
        byRole('option', {
          name: 'coding_rules.custom_severity.severity_with_recommended.severity_impact.LOW',
        }).get(),
      );
      await user.click(ui.saveButton.get(ui.changeQPDialog.get()));

      await user.click(ui.changeButton('QP Bar').get());
      expect(ui.newSeveritySelect(SoftwareQuality.Maintainability).get()).toHaveValue(
        'coding_rules.custom_severity.severity_with_recommended.severity_impact.LOW',
      );
      expect(ui.newSeveritySelect(SoftwareQuality.Security).get()).toBeDisabled();
      expect(ui.newSeveritySelect(SoftwareQuality.Reliability).get()).toBeDisabled();
      expect(ui.notRecommendedSeverity.query()).not.toBeInTheDocument();
    });
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
      'coding_rules#languages=c,js|impactSoftwareQualities=MAINTAINABILITY|cleanCodeAttributeCategories=INTENTIONAL',
    );
    await ui.appLoaded();
    await user.click(ui.cleanCodeCategoriesFacet.get());
    expect(
      await ui.facetItem('issue.clean_code_attribute_category.INTENTIONAL').find(),
    ).toBeChecked();

    expect(ui.facetItem('software_quality.MAINTAINABILITY').get()).toBeChecked();

    // Only 2 rules shown
    expect(screen.getByText('x_of_y_shown.2.2')).toBeInTheDocument();
  });

  it('should handle hash parameters in STANDARD mode', async () => {
    const { ui } = getPageObjects();

    settingsHandler.set(SettingsKey.MQRMode, 'false');

    renderCodingRulesApp(
      mockLoggedInUser(),
      'coding_rules#languages=c,js|severities=MAJOR|types=BUG',
    );
    await ui.appLoaded();

    expect(ui.facetItem(/issue.type.BUG/).get()).toBeChecked();
    expect(ui.facetItem(/severity.MAJOR/).get()).toBeChecked();

    // Only 2 rules shown
    expect(screen.getByText('x_of_y_shown.2.2')).toBeInTheDocument();
  });
});
