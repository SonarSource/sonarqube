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

import com.google.common.collect.ImmutableList;
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
  private TechnicalDebtModelFinder technicalDebtModelFinder = mock(TechnicalDebtModelFinder.class);

  @Before
  public void init() throws Exception {
    technicalDebtModelFinder = mock(TechnicalDebtModelFinder.class);
    when(technicalDebtModelFinder.getContributingPluginList()).thenReturn(ImmutableList.of("java"));
    when(technicalDebtModelFinder.createReaderForXMLFile("java")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-java-model.xml").getPath()));
    // Mock default model
    when(technicalDebtModelFinder.createReaderForXMLFile("technical-debt")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-default-model.xml").getPath()));

    manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), technicalDebtModelFinder, new XMLImporter());
  }

  @Test
  public void create_only_default_model_on_first_execution_when_no_plugin() throws Exception {
    setupData("create_default_model_on_first_execution");

    TechnicalDebtModelFinder technicalDebtModelFinder = mock(TechnicalDebtModelFinder.class);
    when(technicalDebtModelFinder.createReaderForXMLFile("technical-debt")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-default-model.xml").getPath()));

    TechnicalDebtManager manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), technicalDebtModelFinder, new XMLImporter());
    manager.reset(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();

    checkTables("create_default_model_on_first_execution", "quality_models", "characteristics", "characteristic_edges");
  }

  @Test
  public void create_model_with_requirements_from_plugin_on_first_execution() throws Exception {
    setupData("create_model_with_requirements_from_plugin_on_first_execution");

    TechnicalDebtModelFinder technicalDebtModelFinder = mock(TechnicalDebtModelFinder.class);

    when(technicalDebtModelFinder.getContributingPluginList()).thenReturn(ImmutableList.of("java"));
    when(technicalDebtModelFinder.createReaderForXMLFile("java")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-java-model.xml").getPath()));
    when(technicalDebtModelFinder.createReaderForXMLFile("technical-debt")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-default-model.xml").getPath()));

    RuleCache ruleCache = mock(RuleCache.class);
    Rule rule1 = Rule.create("checkstyle", "import", "Regular expression");
    rule1.setId(1);
    when(ruleCache.getRule("checkstyle", "import")).thenReturn(rule1);
    Rule rule2 = Rule.create("checkstyle", "export", "Regular expression");
    rule2.setId(2);
    when(ruleCache.getRule("checkstyle", "export")).thenReturn(rule2);

    TechnicalDebtManager manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), technicalDebtModelFinder, new XMLImporter());
    manager.reset(ValidationMessages.create(), ruleCache);
    getSession().commit();

    checkTables("create_model_with_requirements_from_plugin_on_first_execution", "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void disable_characteristics_and_remove_requirements_when_resetting_model() throws Exception {
    setupData("disable_characteristics_and_remove_requirements_when_resetting_model");

    TechnicalDebtModelFinder technicalDebtModelFinder = mock(TechnicalDebtModelFinder.class);

    when(technicalDebtModelFinder.getContributingPluginList()).thenReturn(ImmutableList.of("java"));
    when(technicalDebtModelFinder.createReaderForXMLFile("java")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-java-model.xml").getPath()));
    when(technicalDebtModelFinder.createReaderForXMLFile("technical-debt")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-default-model.xml").getPath()));

    TechnicalDebtManager manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), technicalDebtModelFinder, new XMLImporter());
    manager.reset(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();

    checkTables("disable_characteristics_and_remove_requirements_when_resetting_model", "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void provided_plugin_should_not_override_default_model_when_resetting_model() throws FileNotFoundException {
    Model model = manager.reset(ValidationMessages.create(), defaultRuleCache());
    // Default model values
    assertThat(model.getCharacteristicByKey("PORTABILITY").getName()).isEqualTo("Portability");
    assertThat(model.getCharacteristicByKey("COMPILER_RELATED_PORTABILITY").getName()).isEqualTo("Compiler related portability");
    assertThat(model.getCharacteristicByKey("HARDWARE_RELATED_PORTABILITY").getName()).isEqualTo("Hardware related portability");
    assertThat(model.getCharacteristicByKey("MAINTAINABILITY").getName()).isEqualTo("Maintainability");

    // Plugin has renamed it, but the value should stay as defined by default model
    assertThat(model.getCharacteristicByKey("READABILITY").getName()).isEqualTo("Readability");
  }

  @Test
  public void no_failure_on_unknown_rule_when_resetting_model() {
    setupData("reset_model");

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(anyString(), anyString())).thenReturn(null);

    manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), technicalDebtModelFinder, new XMLImporter());

    ValidationMessages messages = ValidationMessages.create();
    Model model = manager.reset(messages, new RuleCache(ruleFinder));
    assertThat(model.getCharacteristics().size()).isGreaterThanOrEqualTo(3);
    assertThat(model.getCharacteristics().size()).isGreaterThan(model.getRootCharacteristics().size());
    List<Characteristic> hardwareControls = model.getCharacteristicByKey("HARDWARE_RELATED_PORTABILITY").getChildren();
    assertThat(hardwareControls.isEmpty()).isTrue();

    assertThat(messages.getWarnings()).hasSize(3);
  }

  private RuleCache defaultRuleCache() {
    return new RuleCache(new DefaultRuleFinder(getSessionFactory()));
  }

  private Rule newRegexpRule() {
    return Rule.create("checkstyle", "regexp", "Regular expression");
  }

}
