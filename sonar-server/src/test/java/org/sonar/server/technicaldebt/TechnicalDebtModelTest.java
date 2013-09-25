/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.technicaldebt;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TechnicalDebtModelTest {

  private Model model;
  private TechnicalDebtModel technicalDebtModel;

  @Before
  public void setUpModel() {
    model = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    technicalDebtModel = new TechnicalDebtModel(model);
  }

  @Test
  public void shouldMergeWithEmptyModel() {
    Model with = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    efficiency.addChild(with.createCharacteristicByKey("ram-efficiency", "RAM Efficiency"));
    with.createCharacteristicByKey("usability", "Usability");

    ValidationMessages messages = ValidationMessages.create();

    technicalDebtModel.mergeWith(with, messages, mockRuleCache());

    assertThat(model.getCharacteristics()).hasSize(3);
    assertThat(model.getRootCharacteristics()).hasSize(2);
    assertThat(model.getCharacteristicByKey("ram-efficiency").getDepth()).isEqualTo(Characteristic.ROOT_DEPTH + 1);
    assertThat(messages.getErrors()).isEmpty();
  }

  @Test
  public void shouldNotUpdateExistingCharacteristics() {
    model.createCharacteristicByKey("efficiency", "Efficiency");

    Model with = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    with.createCharacteristicByKey("efficiency", "New efficiency");

    technicalDebtModel.mergeWith(with, ValidationMessages.create(), mockRuleCache());

    assertThat(model.getCharacteristics()).hasSize(1);
    assertThat(model.getRootCharacteristics()).hasSize(1);
    assertThat(model.getCharacteristicByKey("efficiency").getName()).isEqualTo("Efficiency");
  }

  @Test
  public void shouldWarnOnMissingRule() {
    Model with = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    Rule fooRule = Rule.create("foo", "bar", "Bar");
    Characteristic requirement = with.createCharacteristicByRule(fooRule);
    efficiency.addChild(requirement);

    ValidationMessages messages = ValidationMessages.create();

    technicalDebtModel.mergeWith(with, messages, mockRuleCache());

    assertThat(model.getCharacteristics()).hasSize(1);
    assertThat(model.getCharacteristicByKey("efficiency").getName()).isEqualTo("Efficiency");
    assertThat(model.getCharacteristicByRule(fooRule)).isNull();
    assertThat(messages.getWarnings()).hasSize(1);
    assertThat(messages.getWarnings().get(0)).contains("foo"); // warning: the rule foo does not exist
  }

  private RuleCache mockRuleCache() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(Lists.newArrayList(newRegexpRule()));
    return new RuleCache(ruleFinder);
  }

  private Rule newRegexpRule() {
    return Rule.create("checkstyle", "regexp", "Regular expression");
  }
}

