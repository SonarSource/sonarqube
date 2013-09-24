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
package org.sonar.core.technicaldebt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TechnicalDebtManagerTest extends AbstractDbUnitTestCase {

  private TechnicalDebtManager manager;
  private TechnicalDebtModelFinder TechnicalDebtModelFinder;

  @Before
  public void init() throws Exception {
    TechnicalDebtModelFinder = mock(TechnicalDebtModelFinder.class);
    when(TechnicalDebtModelFinder.getContributingPluginList()).thenReturn(ImmutableList.of("java", "technical-debt"));
    when(TechnicalDebtModelFinder.createReaderForXMLFile("java")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-java-model.xml").getPath()));
    // Mock default sqale model
    when(TechnicalDebtModelFinder.createReaderForXMLFile("technical-debt")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-default-model.xml").getPath()));

    manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), TechnicalDebtModelFinder, new XMLImporter());
  }

  @Test
  public void reset_model() {
    setupData("reset_model");

    Model model = manager.resetModel(ValidationMessages.create(), defaultRuleCache());
    assertThat(model.getCharacteristics().size()).isGreaterThan(3);
    assertThat(model.getCharacteristics().size()).isGreaterThan(model.getRootCharacteristics().size());
    for (Characteristic portabilityCharacteristic : model.getCharacteristicByKey("PORTABILITY").getChildren()) {
      assertThat(portabilityCharacteristic.getName()).contains("portability");
      Characteristic requirement = portabilityCharacteristic.getChildren().get(0);
      assertThat(requirement).isNotNull();
      Rule rule = requirement.getRule();
      assertThat(rule).isNotNull();
      assertThat(rule.getName()).isEqualTo("Regular exp");
    }
    assertThat(model.getCharacteristicByKey("testability")).isNull();
    assertThat(model.getCharacteristicByKey("unit_testability")).isNull();
  }

  @Test
  public void provided_plugin_should_not_override_default_model_when_resetting_model() throws FileNotFoundException {
    setupData("reset_model");

    Model model = manager.resetModel(ValidationMessages.create(), defaultRuleCache());
    // Default model values
    assertThat(model.getCharacteristicByKey("PORTABILITY").getName()).isEqualTo("Portability");
    assertThat(model.getCharacteristicByKey("COMPILER_RELATED_PORTABILITY").getName()).isEqualTo("Compiler related portability");
    assertThat(model.getCharacteristicByKey("HARDWARE_RELATED_PORTABILITY").getName()).isEqualTo("Hardware related portability");
    assertThat(model.getCharacteristicByKey("MAINTAINABILITY").getName()).isEqualTo("Maintainability");

    // Plugin has renamed it the value stay as defined by default model
    assertThat(model.getCharacteristicByKey("READABILITY").getName()).isEqualTo("Readability");

    // Characteristic provided only by the plugin
    assertThat(model.getCharacteristicByKey("UNDERSTANDABILITY").getName()).isEqualTo("Understandability related maintainability");
  }

  @Test
  public void not_fail_if_unknown_rule_when_resetting_model() {
    setupData("reset_model");

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenReturn(null);

    manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()),
      TechnicalDebtModelFinder, new XMLImporter());

    Model model = manager.resetModel(ValidationMessages.create(), new RuleCache(ruleFinder));
    assertThat(model.getCharacteristics().size()).isGreaterThanOrEqualTo(3);
    assertThat(model.getCharacteristics().size()).isGreaterThan(model.getRootCharacteristics().size());
    List<Characteristic> hardwareControls = model.getCharacteristicByKey("HARDWARE_RELATED_PORTABILITY").getChildren();
    assertThat(hardwareControls.isEmpty()).isTrue();
  }

  @Test
  public void create_initial_model() {
    Model model = manager.createInitialModel(ValidationMessages.create(), defaultRuleCache());

    // Default model values
    assertThat(model.getCharacteristicByKey("PORTABILITY").getName()).isEqualTo("Portability");
    assertThat(model.getCharacteristicByKey("COMPILER_RELATED_PORTABILITY").getName()).isEqualTo("Compiler related portability");
    assertThat(model.getCharacteristicByKey("HARDWARE_RELATED_PORTABILITY").getName()).isEqualTo("Hardware related portability");
    assertThat(model.getCharacteristicByKey("MAINTAINABILITY").getName()).isEqualTo("Maintainability");

    // Plugin has renamed it the value stay as defined by default model
    assertThat(model.getCharacteristicByKey("READABILITY").getName()).isEqualTo("Readability");

    // Characteristic provided only by the plugin
    assertThat(model.getCharacteristicByKey("UNDERSTANDABILITY").getName()).isEqualTo("Understandability related maintainability");
  }

  @Test
  public void persist_merge() {
    setupData("persist_merge");

    Model with = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    Characteristic ramEfficiency = with.createCharacteristicByKey("ram-efficiency", "RAM Efficiency");
    efficiency.addChild(ramEfficiency);
    ramEfficiency.addChild(with.createCharacteristicByRule(newRegexpRule()));

    manager.merge(with, ValidationMessages.create(), defaultRuleCache());

    checkTables("persist_merge", "quality_models", "characteristics", "characteristic_edges");
  }

  @Test
  public void persist_merge_with_plugin_files() throws Exception {
    setupData("persist_merge");

    manager.merge(Lists.newArrayList("java"), ValidationMessages.create(), defaultRuleCache());

    Model model = (new DefaultModelFinder(getSessionFactory())).findByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    assertThat(model.getCharacteristics()).contains(Characteristic.createByKey("PORTABILITY", "Portability"));
  }

  @Test
  public void persist_restore() {
    setupData("persist_restore");

    Model with = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    Characteristic ramEfficiency = with.createCharacteristicByKey("ram-efficiency", "RAM Efficiency");
    efficiency.addChild(ramEfficiency);
    ramEfficiency.addChild(with.createCharacteristicByRule(newRegexpRule()));

    manager.restore(with, ValidationMessages.create(), defaultRuleCache());

    checkTables("persist_restore", "quality_models", "characteristics", "characteristic_edges");
  }

  @Test
  public void warn_when_restoring_unknown_rule() {
    setupData("warn_when_restoring_unknown_rule");

    Model with = Model.createByName(TechnicalDebtModelDefinition.TECHNICAL_DEBT_MODEL);
    Characteristic efficiency = with.createCharacteristicByKey("efficiency", "Efficiency");
    efficiency.addChild(with.createCharacteristicByRule(newRegexpRule()));

    ValidationMessages messages = ValidationMessages.create();
    manager.restore(with, messages, defaultRuleCache());

    checkTables("warn_when_restoring_unknown_rule", "quality_models", "characteristics", "characteristic_edges");
    assertThat(messages.getWarnings()).hasSize(1);
    assertThat(messages.getWarnings().get(0)).contains("regexp");
  }

  private RuleCache defaultRuleCache() {
    return new RuleCache(new DefaultRuleFinder(getSessionFactory()));
  }

  private Rule newRegexpRule() {
    return Rule.create("checkstyle", "regexp", "Regular expression");
  }
}
