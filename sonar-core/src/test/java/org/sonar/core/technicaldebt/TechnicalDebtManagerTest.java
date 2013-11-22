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
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.io.FileNotFoundException;
import java.io.FileReader;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TechnicalDebtManagerTest extends AbstractDbUnitTestCase {

  private static final String[] EXCLUDED_COLUMNS = new String[]{"parent_id", "function_key", "factor_value", "factor_unit", "offset_value", "offset_unit", "created_at", "updated_at"};

  private TechnicalDebtManager manager;
  private TechnicalDebtModelRepository technicalDebtModelRepository = mock(TechnicalDebtModelRepository.class);

  @Before
  public void initAndMerge() throws Exception {
    technicalDebtModelRepository = mock(TechnicalDebtModelRepository.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("technical-debt")).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/fake-default-model.xml").getPath()));

    manager = new TechnicalDebtManager(getSessionFactory(), new DefaultModelFinder(getSessionFactory()), technicalDebtModelRepository, new TechnicalDebtXMLImporter());
  }

  @Test
  public void create_only_default_model_on_first_execution_when_no_plugin() throws Exception {
    setupData("empty");

    manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();

    checkTables("create_default_model_on_first_execution", EXCLUDED_COLUMNS, "quality_models", "characteristics", "characteristic_edges");
  }

  @Test
  public void create_model_with_requirements_from_plugin_on_first_execution() throws Exception {
    setupData("empty");

    addPluginModel("java", "fake-java-model.xml");

    TechnicalDebtRuleCache technicalDebtRuleCache = mock(TechnicalDebtRuleCache.class);
    Rule rule1 = Rule.create("checkstyle", "import", "Regular expression");
    rule1.setId(1);
    when(technicalDebtRuleCache.getRule("checkstyle", "import")).thenReturn(rule1);
    Rule rule2 = Rule.create("checkstyle", "export", "Regular expression");
    rule2.setId(2);
    when(technicalDebtRuleCache.getRule("checkstyle", "export")).thenReturn(rule2);

    manager.initAndMergePlugins(ValidationMessages.create(), technicalDebtRuleCache);
    getSession().commit();

    checkTables("create_model_with_requirements_from_plugin_on_first_execution", EXCLUDED_COLUMNS, "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void add_new_requirements_from_plugin() throws Exception {
    setupData("add_new_requirements_from_plugin");

    addPluginModel("java", "fake-java-model.xml");

    manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();

    checkTables("add_new_requirements_from_plugin", EXCLUDED_COLUMNS, "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void disable_requirements_on_removed_rules() throws Exception {
    setupData("disable_requirements_on_removed_rules");

    addPluginModel("java", "fake-java-model.xml");

    manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();

    checkTables("disable_requirements_on_removed_rules", EXCLUDED_COLUMNS, "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void fail_when_plugin_defines_characteristics_not_defined_in_default_model() throws Exception {
    setupData("fail_when_plugin_defines_characteristics_not_defined_in_default_model");

    addPluginModel("java", "fake-java-model-adding-unknown-characteristic.xml");

    try {
      manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
      getSession().commit();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
    checkTables("fail_when_plugin_defines_characteristics_not_defined_in_default_model", EXCLUDED_COLUMNS, "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void recreate_previously_deleted_characteristic_from_default_model_when_plugin_define_requirements_on_it() throws Exception {
    setupData("recreate_previously_deleted_characteristic_from_default_model_when_plugin_define_requirements_on_it");

    addPluginModel("java", "fake-java-model.xml");

    manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();

    checkTables("recreate_previously_deleted_characteristic_from_default_model_when_plugin_define_requirements_on_it", EXCLUDED_COLUMNS, "quality_models", "characteristics", "characteristic_edges", "characteristic_properties");
  }

  @Test
  public void provided_plugin_should_not_override_default_characteristics_name() throws FileNotFoundException {
    Model model = manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
    getSession().commit();
    // Default model values
    assertThat(model.getCharacteristicByKey("PORTABILITY").getName()).isEqualTo("Portability");
    assertThat(model.getCharacteristicByKey("COMPILER_RELATED_PORTABILITY").getName()).isEqualTo("Compiler related portability");
    assertThat(model.getCharacteristicByKey("HARDWARE_RELATED_PORTABILITY").getName()).isEqualTo("Hardware related portability");
    assertThat(model.getCharacteristicByKey("MAINTAINABILITY").getName()).isEqualTo("Maintainability");

    // Plugin has renamed it, but the value should stay as defined by default model
    assertThat(model.getCharacteristicByKey("READABILITY").getName()).isEqualTo("Readability");
  }

  @Test
  public void no_failure_on_unknown_rule() throws FileNotFoundException {
    setupData("empty");

    addPluginModel("java", "fake-java-model.xml");

    TechnicalDebtRuleCache technicalDebtRuleCache = mock(TechnicalDebtRuleCache.class);
    Rule rule1 = Rule.create("checkstyle", "import", "Regular expression");
    rule1.setId(1);
    when(technicalDebtRuleCache.getRule("checkstyle", "import")).thenReturn(rule1);
    Rule rule2 = Rule.create("checkstyle", "export", "Regular expression");
    rule2.setId(2);
    when(technicalDebtRuleCache.getRule("checkstyle", "export")).thenReturn(rule2);

    ValidationMessages messages = ValidationMessages.create();
    manager.initAndMergePlugins(messages, technicalDebtRuleCache);
    getSession().commit();

    assertThat(messages.getWarnings()).hasSize(1);
    assertThat(messages.getWarnings().get(0)).isEqualTo("Rule not found: [repository=checkstyle, key=ConstantNameCheck]");
  }

  @Test
  public void fail_when_adding_characteristic_not_existing_in_default_characteristics() throws FileNotFoundException {
    setupData("empty");

    addPluginModel("java", "fake-default-model-with-addtionnal-characteristic.xml");

    try {
      manager.initAndMergePlugins(ValidationMessages.create(), defaultRuleCache());
      getSession().commit();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The characteristic : SUB_ONE cannot be used as it's not available in default ones.");
    }
  }

  private TechnicalDebtRuleCache defaultRuleCache() {
    return new TechnicalDebtRuleCache(new DefaultRuleFinder(getSessionFactory()));
  }

  private void addPluginModel(String pluginKey, String xmlFile) throws FileNotFoundException {
    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(ImmutableList.of(pluginKey));
    when(technicalDebtModelRepository.createReaderForXMLFile(pluginKey)).thenReturn(
      new FileReader(Resources.getResource(TechnicalDebtManagerTest.class, "TechnicalDebtManagerTest/" + xmlFile).getPath()));
  }


}
