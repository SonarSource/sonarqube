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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.server.startup.RegisterTechnicalDebtModel;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TechnicalDebtModelTest {

  private Model model;
  private TechnicalDebtModel technicalDebtModel;
  private List<Characteristic> defaultCharacteristics;

  @Before
  public void setUpModel() {
    model = Model.createByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    defaultCharacteristics = newArrayList();
    technicalDebtModel = new TechnicalDebtModel(model, defaultCharacteristics);
  }

  @Test
  public void merge_with_empty_model() {
    Model with = Model.createByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    Characteristic ramEfficiency = with.createCharacteristicByKey("ram-efficiency", "RAM Efficiency");
    efficiency.addChild(ramEfficiency);
    Characteristic usability = with.createCharacteristicByKey("usability", "Usability");

    ValidationMessages messages = ValidationMessages.create();

    defaultCharacteristics.addAll(newArrayList(efficiency, ramEfficiency, usability));
    technicalDebtModel.mergeWith(with, messages, mockRuleCache());

    assertThat(model.getCharacteristics()).hasSize(3);
    assertThat(model.getRootCharacteristics()).hasSize(2);
    assertThat(model.getCharacteristicByKey("ram-efficiency").getDepth()).isEqualTo(Characteristic.ROOT_DEPTH + 1);
    assertThat(messages.getErrors()).isEmpty();
  }

  @Test
  public void not_update_existing_characteristics() {
    model.createCharacteristicByKey("efficiency", "Efficiency");

    Model with = Model.createByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    with.createCharacteristicByKey("efficiency", "New efficiency");

    technicalDebtModel.mergeWith(with, ValidationMessages.create(), mockRuleCache());

    assertThat(model.getCharacteristics()).hasSize(1);
    assertThat(model.getRootCharacteristics()).hasSize(1);
    assertThat(model.getCharacteristicByKey("efficiency").getName()).isEqualTo("Efficiency");
  }

  @Test
  public void warn_on_missing_rule() {
    Model with = Model.createByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    Rule fooRule = Rule.create("foo", "bar", "Bar");
    Characteristic requirement = with.createCharacteristicByRule(fooRule);
    efficiency.addChild(requirement);

    ValidationMessages messages = ValidationMessages.create();

    defaultCharacteristics.add(efficiency);
    technicalDebtModel.mergeWith(with, messages, mockRuleCache());

    assertThat(model.getCharacteristics()).hasSize(1);
    assertThat(model.getCharacteristicByKey("efficiency").getName()).isEqualTo("Efficiency");
    assertThat(model.getCharacteristicByRule(fooRule)).isNull();
    assertThat(messages.getWarnings()).hasSize(1);
    assertThat(messages.getWarnings().get(0)).contains("foo"); // warning: the rule foo does not exist
  }

  @Test
  public void fail_when_adding_characteristic_not_existing_in_default_characteristics() {
    Model with = Model.createByName(RegisterTechnicalDebtModel.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    // usability is not available in default characteristics
    with.createCharacteristicByKey("usability", "Usability");

    ValidationMessages messages = ValidationMessages.create();

    defaultCharacteristics.add(efficiency);
    try {
      technicalDebtModel.mergeWith(with, messages, mockRuleCache());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(model.getCharacteristics()).hasSize(1);
  }

  private RuleCache mockRuleCache() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(newArrayList(newRegexpRule()));
    return new RuleCache(ruleFinder);
  }

  private Rule newRegexpRule() {
    return Rule.create("checkstyle", "regexp", "Regular expression");
  }
}

