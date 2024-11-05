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

import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import CodingRulesServiceMock from '../../../api/mocks/CodingRulesServiceMock';
import SettingsServiceMock from '../../../api/mocks/SettingsServiceMock';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
import { getPageObjects, renderCodingRulesApp } from '../utils-tests';

const rulesHandler = new CodingRulesServiceMock();
const settingsHandler = new SettingsServiceMock();

jest.mock('../../../helpers/l10nBundle', () => {
  const bundle = jest.requireActual('../../../helpers/l10nBundle');
  return {
    ...bundle,
    getIntl: () => ({ formatMessage: jest.fn() }),
  };
});

afterEach(() => {
  rulesHandler.reset();
  settingsHandler.reset();
});

describe('custom rule', () => {
  it('can create custom rule', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser());
    await ui.facetsLoaded();

    await user.click(await ui.templateFacet.find());
    await user.click(ui.facetItem('coding_rules.filters.template.is_template').get());

    // Shows only one template rule
    expect(ui.getAllRuleListItems()).toHaveLength(1);

    // Show template rule details
    await user.click(ui.ruleListItemLink('Template rule').get());
    expect(ui.ruleTitle('Template rule').get()).toBeInTheDocument();
    expect(ui.customRuleSectionTitle.get()).toBeInTheDocument();

    // Create custom rule
    await user.click(ui.createCustomRuleButton.get());
    await user.type(ui.ruleNameTextbox.get(), 'New Custom Rule');
    expect(ui.keyTextbox.get()).toHaveValue('New_Custom_Rule');
    await user.clear(ui.keyTextbox.get());
    await user.type(ui.keyTextbox.get(), 'new_custom_rule');

    await user.click(ui.cleanCodeCategorySelect.get());
    await user.click(
      byRole('option', { name: 'rule.clean_code_attribute_category.CONSISTENT' }).get(),
    );

    await user.click(ui.cleanCodeAttributeSelect.get());
    await user.click(byRole('option', { name: 'rule.clean_code_attribute.IDENTIFIABLE' }).get());

    await user.click(ui.cleanCodeCategorySelect.get());
    await user.click(
      byRole('option', { name: 'rule.clean_code_attribute_category.INTENTIONAL' }).get(),
    );

    // Setting default clean code category of a template should set corresponding attribute
    expect(
      ui.createCustomRuleDialog.byText('rule.clean_code_attribute.CLEAR').get(),
    ).toBeInTheDocument();

    // Set software qualities
    expect(ui.cleanCodeQualityCheckbox(SoftwareQuality.Maintainability).get()).toBeChecked();
    // Uncheck all software qualities - should see error message
    await user.click(ui.cleanCodeQualityCheckbox(SoftwareQuality.Maintainability).get());
    expect(
      ui.createCustomRuleDialog.byText('coding_rules.custom_rule.select_software_quality').get(),
    ).toBeInTheDocument();

    await user.click(ui.cleanCodeQualityCheckbox(SoftwareQuality.Reliability).get());

    await user.click(ui.cleanCodeSeveritySelect(SoftwareQuality.Reliability).get());
    await user.click(
      byRole('option', { name: 'severity_impact.MEDIUM severity_impact.MEDIUM' }).get(),
    );

    expect(ui.createCustomRuleDialog.byText('severity_impact.MEDIUM').get()).toBeInTheDocument();

    await user.click(ui.statusSelect.get());
    await user.click(byRole('option', { name: 'rules.status.BETA' }).get());

    await user.type(ui.descriptionTextbox.get(), 'Some description for custom rule');
    await user.type(ui.paramInput('1').get(), 'Default value');

    await user.click(ui.createButton.get());

    // Verify the rule is created
    expect(ui.customRuleItemLink('New Custom Rule').get()).toBeInTheDocument();
  });

  it('can reactivate custom rule', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule8');
    await ui.detailsloaded();

    // Try create custom rule with existing rule with removed status
    await user.click(ui.createCustomRuleButton.get());
    await user.type(ui.ruleNameTextbox.get(), 'Reactivate custom Rule');
    await user.clear(ui.keyTextbox.get());
    await user.type(ui.keyTextbox.get(), 'rule12');
    await user.type(ui.descriptionTextbox.get(), 'Some description for custom rule');

    await user.click(ui.createButton.get());

    expect(byText('coding_rules.reactivate.help').get()).toBeInTheDocument();

    // Reactivate rule
    await user.click(ui.reactivateButton.get());
    // Verify the rule is reactivated
    expect(ui.customRuleItemLink('Reactivate custom Rule').get()).toBeInTheDocument();
  });

  it('can edit custom rule', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule9');
    await ui.detailsloaded();

    await user.click(ui.editCustomRuleButton.get());

    // Change name and description of custom rule
    await user.clear(ui.ruleNameTextbox.get());
    await user.type(ui.ruleNameTextbox.get(), 'Updated custom rule name');
    await user.type(ui.descriptionTextbox.get(), 'Some description for custom rule');

    await user.click(ui.saveButton.get(ui.updateCustomRuleDialog.get()));

    expect(ui.ruleTitle('Updated custom rule name').get()).toBeInTheDocument();
  });

  it('can delete custom rule', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule9');
    await ui.detailsloaded();

    await user.click(ui.deleteButton.get());
    await user.click(ui.deleteButton.get(ui.deleteCustomRuleDialog.get()));

    // Shows the list of rules, custom rule should not be included
    expect(ui.ruleListItemLink('Custom Rule based on rule8').query()).not.toBeInTheDocument();
  });

  it('can delete custom rule from template page', async () => {
    const { ui, user } = getPageObjects();
    rulesHandler.setIsAdmin();
    renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule8');
    await ui.detailsloaded();

    await user.click(ui.deleteCustomRuleButton('Custom Rule based on rule8').get());
    await user.click(ui.deleteButton.get(ui.deleteCustomRuleDialog.get()));
    expect(ui.customRuleItemLink('Custom Rule based on rule8').query()).not.toBeInTheDocument();
  });

  it('anonymous user cannot modify custom rule', async () => {
    const { ui } = getPageObjects();
    renderCodingRulesApp(undefined, 'coding_rules?open=rule9');
    await ui.facetsLoaded();

    expect(ui.editCustomRuleButton.query()).not.toBeInTheDocument();
    expect(ui.deleteButton.query()).not.toBeInTheDocument();
  });
});
