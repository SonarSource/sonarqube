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

import userEvent from '@testing-library/user-event';
import {
  byLabelText,
  byPlaceholderText,
  byRole,
  byTestId,
  byText,
} from '~sonar-aligned/helpers/testSelector';
import { Profile } from '../../api/quality-profiles';
import { renderAppRoutes } from '../../helpers/testReactTestingUtils';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/clean-code-taxonomy';
import { Feature } from '../../types/features';
import { IssueSeverity, IssueType } from '../../types/issues';
import { CurrentUser } from '../../types/users';
import routes from './routes';

const selectors = {
  loading: byText('loading'),

  // List
  rulesList: byRole('list', { name: 'list_of_rules' }),
  ruleListItemLink: (name: string) => byRole('link', { name }),
  getAllRuleListItems: () =>
    byLabelText('list_of_rules')
      .byRole('listitem')
      .getAll()
      .filter((item) => !!item.getAttribute('data-rule')),
  currentListItem: byRole('listitem', { current: true }),

  // Filters
  filtersHeading: byRole('heading', { name: 'filters' }),
  searchInput: byRole('searchbox', { name: 'search.search_for_rules' }),
  clearAllFiltersButton: byRole('button', { name: 'clear_all_filters' }),

  // Facets
  cleanCodeCategoriesFacet: byRole('button', {
    name: 'coding_rules.facet.cleanCodeAttributeCategories',
  }),
  languagesFacet: byRole('button', { name: 'issues.facet.languages' }),
  typeFacet: byRole('button', { name: 'coding_rules.facet.types' }),
  tagsFacet: byRole('button', { name: 'coding_rules.facet.tags' }),
  repositoriesFacet: byRole('button', { name: 'coding_rules.facet.repositories' }),
  softwareQualitiesFacet: byRole('button', { name: 'coding_rules.facet.impactSoftwareQualities' }),
  severetiesFacet: byRole('button', { name: 'coding_rules.facet.impactSeverities' }),
  standardSeveritiesFacet: byRole('button', { name: 'coding_rules.facet.severities' }),
  statusesFacet: byRole('button', { name: 'coding_rules.facet.statuses' }),
  standardsFacet: byRole('button', { name: 'issues.facet.standards' }),
  securityHotspotFacet: byRole('button', { name: 'coding_rules.filters.security_hotspots' }),
  standardsOwasp2017Top10Facet: byText('issues.facet.owaspTop10'),
  standardsOwasp2021Top10Facet: byText('issues.facet.owaspTop10_2021'),
  standardsCweFacet: byText('issues.facet.cwe'),
  standardsCvssFacet: byText('issues.facet.cvss'),
  availableSinceFacet: byRole('button', { name: 'coding_rules.facet.available_since' }),
  templateFacet: byRole('button', { name: 'coding_rules.facet.template' }),
  qpFacet: byRole('button', { name: 'coding_rules.facet.qprofile' }),
  prioritizedRuleFacet: byRole('button', { name: 'coding_rules.facet.prioritizedRule' }),
  facetClear: (name: string) => byTestId(name),
  facetSearchInput: (name: string) => byLabelText(name),
  facetItem: (name: string | RegExp) => byRole('checkbox', { name, hidden: true }),
  availableSinceDateField: byPlaceholderText('date'),
  qpActiveRadio: byRole('radio', { name: `active` }),
  qpInactiveRadio: byRole('radio', { name: `inactive` }),
  dateInputMonthSelect: byTestId('month-select'),
  dateInputYearSelect: byTestId('year-select'),

  // Bulk change
  bulkChangeButton: byRole('button', { name: 'bulk_change' }),
  activateIn: byRole('menuitem', { name: 'coding_rules.activate_in' }),
  deactivateIn: byRole('menuitem', { name: 'coding_rules.deactivate_in' }),
  bulkChangeDialog: (count: number, activate = true) =>
    byRole('dialog', {
      name: `coding_rules.${
        activate ? 'ac' : 'deac'
      }tivate_in_quality_profile (${count} coding_rules._rules)`,
    }),
  activateInSelect: byRole('combobox', { name: 'coding_rules.activate_in' }),
  deactivateInSelect: byRole('combobox', { name: 'coding_rules.deactivate_in' }),
  noQualityProfiles: byText('coding_rules.bulk_change.no_quality_profile'),
  bulkApply: byRole('button', { name: 'apply' }),
  bulkSuccessMessage: (qpName: string, langName: string, count: number) =>
    byText(`coding_rules.bulk_change.success.${qpName}.${langName}.${count}`),
  bulkWarningMessage: (qpName: string, langName: string, count: number) =>
    byText(`coding_rules.bulk_change.warning.${qpName}.${langName}.${count}.1`),
  bulkClose: byRole('button', { name: 'close' }),

  // Rule details
  ruleTitle: (name: string) => byRole('heading', { level: 1, name }),
  externalDescription: (ruleName: string) => byText(`issue.external_issue_description.${ruleName}`),
  introTitle: byText(/Introduction to this rule/),
  rootCause: byText('Root cause'),
  howToFix: byText('This is how to fix'),
  resourceLink: byRole('link', { name: 'Awsome Reading' }),
  contextRadio: (name: string) => byRole('radio', { name }),
  contextSubtitle: (name: string) => byText(`coding_rules.description_context.subtitle.${name}`),
  otherContextTitle: byText('coding_rules.context.others.title'),
  caycNotificationButton: byRole('button', { name: 'coding_rules.more_info.scroll_message' }),
  extendDescriptionButton: byRole('button', { name: 'coding_rules.extend_description' }),
  extendDescriptionTextbox: byRole('textbox', { name: 'coding_rules.extend_description' }),
  prioritizedRuleCell: byRole('cell', { name: /coding_rules.prioritized_rule.title/ }),
  oldSeverityCustomizedCell: byRole('cell', { name: /coding_rules.severity_customized.message/ }),
  newSeverityCustomizedCell: byRole('cell', {
    name: /coding_rules.impact_customized.message/,
  }).byText('coding_rules.impact_customized.message'),
  qualityProfileRow: byRole('table', { name: 'coding_rules.quality_profiles' }).byRole('row'),
  saveButton: byRole('button', { name: 'save' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  removeButton: byRole('button', { name: 'remove' }),
  ruleCleanCodeAttributeCategory: (category: CleanCodeAttributeCategory) =>
    byText(`rule.clean_code_attribute_category.${category}`),
  ruleCleanCodeAttribute: (attribute: CleanCodeAttribute) =>
    byText(new RegExp(`rule\\.clean_code_attribute\\.${attribute}$`)),
  ruleSoftwareQuality: (quality: SoftwareQuality) => byText(`software_quality.${quality}`),
  ruleSoftwareQualityPill: (quality: SoftwareQuality, severity: SoftwareImpactSeverity) =>
    byRole('button', { name: `software_quality.${quality} severity_impact.${severity}` }),
  ruleIssueTypePill: (issueType: IssueType) => byRole('banner').byText(`issue.type.${issueType}`),
  ruleIssueTypePillSeverity: (severity: IssueSeverity) =>
    byRole('banner').byLabelText(`severity.${severity}`),

  // Rule tags
  tagsDropdown: byLabelText(/tags_list_x/).byRole('button'),
  tagCheckbox: (tag: string) => byLabelText(tag),
  tagSearch: byRole('searchbox', { name: 'search.search_for_tags' }),

  // Rule tabs
  whyIssueTab: byRole('tab', {
    name: 'coding_rules.description_section.title.root_cause',
  }),
  whatRiskTab: byRole('tab', {
    name: 'coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT',
  }),
  assessTab: byRole('tab', {
    name: 'coding_rules.description_section.title.assess_the_problem',
  }),
  moreInfoTab: byRole('tab', {
    name: /coding_rules.description_section.title.more_info/,
  }),
  howToFixTab: byRole('tab', {
    name: 'coding_rules.description_section.title.how_to_fix',
  }),

  // Rule Quality Profiles
  qpLink: (name: string) => byLabelText(name),
  activateButton: byRole('button', { name: 'coding_rules.activate', hidden: true }),
  deactivateButton: byRole('button', {
    name: /coding_rules.deactivate_in_quality_profile/,
    hidden: true,
  }),
  qualityProfileSelect: byRole('combobox', { name: 'coding_rules.quality_profile' }),
  oldSeveritySelect: byRole('combobox', { name: 'coding_rules.custom_severity.choose_severity' }),
  newSeveritySelect: (quality: SoftwareQuality) =>
    byRole('combobox', { name: `software_quality.${quality}` }),
  notRecommendedSeverity: byText('coding_rules.custom_severity.not_recommended'),
  prioritizedSwitch: byRole('checkbox', { name: 'coding_rules.prioritized_rule.switch_label' }),
  activateQPDialog: byRole('dialog', { name: 'coding_rules.activate_in_quality_profile' }),
  changeButton: (profile: string) =>
    byRole('button', { name: `coding_rules.change_details_x.${profile}` }),
  changeQPDialog: byRole('dialog', { name: 'coding_rules.change_details' }),
  deactivateInQPButton: (profile: string) =>
    byRole('button', { name: `coding_rules.deactivate_in_quality_profile_x.${profile}` }),
  revertToParentDefinitionButton: byRole('button', {
    name: 'coding_rules.revert_to_parent_definition',
  }),
  activaInAllQPs: byText('coding_rules.active_in_all_profiles'),
  yesButton: byRole('button', { name: 'yes' }),
  paramInput: (param: string) => byRole('textbox', { name: param }),

  // Custom rules
  customRuleSectionTitle: byRole('heading', { level: 2, name: 'coding_rules.custom_rules' }),
  createCustomRuleButton: byRole('button', { name: 'coding_rules.create' }),
  editCustomRuleButton: byRole('button', { name: 'edit' }),
  deleteCustomRuleButton: (rule: string) =>
    byRole('button', { name: `coding_rules.delete_rule_x.${rule}` }),
  customRuleItemLink: (name: string) => byRole('link', { name }),

  // Custom rule form
  createCustomRuleDialog: byRole('dialog', { name: 'coding_rules.create_custom_rule' }),
  updateCustomRuleDialog: byRole('dialog', { name: 'coding_rules.update_custom_rule' }),
  deleteCustomRuleDialog: byRole('alertdialog', { name: 'coding_rules.delete_rule' }),
  ruleNameTextbox: byRole('textbox', { name: 'name' }),
  keyTextbox: byRole('textbox', { name: 'key' }),
  cctIssueTypeSelect: byRole('combobox', { name: 'coding_rules.custom.type.label' }),
  standardIssueTypeSelect: byRole('combobox', { name: 'type' }),
  cleanCodeCategorySelect: byRole('combobox', { name: 'category' }),
  cleanCodeAttributeSelect: byRole('combobox', { name: 'attribute' }),
  cleanCodeQualityCheckbox: (quality: SoftwareQuality) =>
    byRole('group', { name: `coding_rules.custom_rule.software_quality_x.${quality}` }).byRole(
      'checkbox',
    ),
  cleanCodeSeveritySelect: (quality: SoftwareQuality) =>
    byRole('group', { name: `coding_rules.custom_rule.software_quality_x.${quality}` }).byRole(
      'combobox',
      { name: 'severity' },
    ),
  standardSeveritySelect: byRole('combobox', { name: 'severity' }),
  statusSelect: byRole('combobox', { name: 'coding_rules.filters.status' }),
  descriptionTextbox: byRole('textbox', { name: 'description' }),
  createButton: byRole('button', { name: 'create' }),
  reactivateButton: byRole('button', { name: 'coding_rules.reactivate' }),
  deleteButton: byRole('button', { name: 'delete' }),
  softwareQualitiesSection: byText('coding_rules.software_qualities.label'),
  cleanCodeAttributeSection: byText('coding_rules.cct_attribute.label'),
};

export function getPageObjects() {
  const user = userEvent.setup();

  const ui = {
    ...selectors,

    async listLoaded() {
      expect(await selectors.filtersHeading.find()).toBeInTheDocument();
      expect(await selectors.rulesList.find()).toBeInTheDocument();
    },

    async facetsLoaded() {
      expect(await selectors.filtersHeading.find()).toBeInTheDocument();
    },

    async detailsloaded() {
      expect(await byRole('heading', { level: 1 }).find()).toBeInTheDocument();
      await ui.facetsLoaded();
    },

    async bulkActivate(rulesCount: number, profile: Profile) {
      await user.click(ui.bulkChangeButton.get());
      await user.click(ui.activateIn.get());

      const dialog = ui.bulkChangeDialog(rulesCount).get();
      expect(dialog).toBeInTheDocument();

      await user.click(ui.activateInSelect.get());
      await user.click(byText(`${profile.name} - ${profile.languageName}`).get(dialog));

      await user.click(ui.bulkApply.get());
    },

    async bulkDeactivate(rulesCount: number, profile: Profile) {
      await user.click(ui.bulkChangeButton.get());
      await user.click(ui.deactivateIn.get());

      const dialog = ui.bulkChangeDialog(rulesCount, false).get();
      expect(dialog).toBeInTheDocument();

      await user.click(ui.deactivateInSelect.get());
      await user.click(byText(`${profile.name} - ${profile.languageName}`).get(dialog));

      await user.click(ui.bulkApply.get());
    },
  };

  return {
    ui,
    user,
  };
}

export function renderCodingRulesApp(
  currentUser?: CurrentUser,
  navigateTo?: string,
  featureList?: Feature[],
) {
  renderAppRoutes('coding_rules', routes, {
    navigateTo,
    currentUser,
    featureList,
    languages: {
      js: { key: 'js', name: 'JavaScript' },
      java: { key: 'java', name: 'Java' },
      c: { key: 'c', name: 'C' },
      py: { key: 'py', name: 'Python' },
    },
  });
}
