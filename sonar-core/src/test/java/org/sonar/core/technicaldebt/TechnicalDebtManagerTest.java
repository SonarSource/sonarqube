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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.Characteristic;
import org.sonar.api.technicaldebt.Requirement;
import org.sonar.api.technicaldebt.WorkUnit;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.MyBatis;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtManagerTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  TechnicalDebtModelRepository technicalDebtModelRepository;

  @Mock
  TechnicalDebtRuleCache ruleCache;

  @Mock
  TechnicalDebtModelFinder sqaleModelFinder;

  @Mock
  TechnicalDebtModelService service;

  @Mock
  TechnicalDebtXMLImporter xmlImporter;

  private TechnicalDebtModel defaultModel;

  private TechnicalDebtManager manager;

  @Before
  public void initAndMerge() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    defaultModel = new TechnicalDebtModel();
    Reader defaultModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("technical-debt")).thenReturn(defaultModelReader);
    when(xmlImporter.importXML(eq(defaultModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(defaultModel);

    manager = new TechnicalDebtManager(myBatis, service, sqaleModelFinder, technicalDebtModelRepository, xmlImporter);
  }

  @Test
  public void create_default_model_on_first_execution_when_no_plugin() throws Exception {
    Characteristic rootCharacteristic = new Characteristic().setKey("PORTABILITY");
    Characteristic characteristic = new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(rootCharacteristic);
    defaultModel.addRootCharacteristic(rootCharacteristic);

    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(Collections.<String>emptyList());
    when(sqaleModelFinder.findAll()).thenReturn(new TechnicalDebtModel());

    manager.initAndMergePlugins(ValidationMessages.create(), ruleCache);

    verify(service).create(eq(rootCharacteristic), eq(session));
    verify(service).create(eq(characteristic), eq(session));
    verifyNoMoreInteractions(service);
  }

  @Test
  public void create_model_with_requirements_from_plugin_on_first_execution() throws Exception {
    // Default model
    Characteristic defaultRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    Characteristic characteristic = new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);

    // No db model
    when(sqaleModelFinder.findAll()).thenReturn(new TechnicalDebtModel());

    // Java model
    TechnicalDebtModel javaModel = new TechnicalDebtModel();
    Characteristic javaRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    Characteristic javaCharacteristic = new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(javaRootCharacteristic);
    javaModel.addRootCharacteristic(javaRootCharacteristic);

    RuleKey ruleKey = RuleKey.of("checkstyle", "import");
    when(ruleCache.exists(ruleKey)).thenReturn(true);
    Requirement javaRequirement = new Requirement().setRuleKey(ruleKey)
      .setFunction("linear").setFactor(WorkUnit.create(30.0, WorkUnit.MINUTES)).setCharacteristic(javaCharacteristic);

    Reader javaModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
    when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

    manager.initAndMergePlugins(ValidationMessages.create(), ruleCache);

    verify(service).create(eq(defaultRootCharacteristic), eq(session));
    verify(service).create(eq(characteristic), eq(session));
    verify(service).create(eq(javaRequirement), eq(javaCharacteristic), eq(ruleCache), eq(session));
    verifyNoMoreInteractions(service);
  }

  @Test
  public void add_new_requirements_from_plugin() throws Exception {
    // Default model
    Characteristic defaultRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);

    // Db model
    TechnicalDebtModel dbModel = new TechnicalDebtModel();
    Characteristic dbRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    Characteristic dbCharacteristic = new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(dbRootCharacteristic);
    dbModel.addRootCharacteristic(dbRootCharacteristic);

    RuleKey ruleKey1 = RuleKey.of("checkstyle", "import");
    when(ruleCache.exists(ruleKey1)).thenReturn(true);
    // Existing requirement
    new Requirement().setRuleKey(ruleKey1)
      .setFunction("linear").setFactor(WorkUnit.create(30.0, WorkUnit.MINUTES)).setCharacteristic(dbCharacteristic);

    when(sqaleModelFinder.findAll()).thenReturn(dbModel);

    // Java model
    TechnicalDebtModel javaModel = new TechnicalDebtModel();
    Characteristic javaRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    Characteristic javaCharacteristic = new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(javaRootCharacteristic);
    javaModel.addRootCharacteristic(javaRootCharacteristic);

    RuleKey ruleKey2 = RuleKey.of("checkstyle", "export");
    when(ruleCache.exists(ruleKey2)).thenReturn(true);
    // New requirement
    Requirement javaRequirement = new Requirement().setRuleKey(ruleKey2)
      .setFunction("linear").setFactor(WorkUnit.create(1.0, WorkUnit.HOURS)).setCharacteristic(javaCharacteristic);

    Reader javaModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
    when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

    manager.initAndMergePlugins(ValidationMessages.create(), ruleCache);

    verify(service).create(eq(javaRequirement), eq(javaCharacteristic), eq(ruleCache), eq(session));
    verifyNoMoreInteractions(service);
  }

  @Test
  public void disable_requirements_on_not_existing_rules() throws Exception {
    // Default model
    Characteristic defaultRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);

    // Db model
    TechnicalDebtModel dbModel = new TechnicalDebtModel();
    Characteristic dbRootCharacteristic = new Characteristic().setKey("PORTABILITY");
    Characteristic dbCharacteristic = new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(dbRootCharacteristic);
    dbModel.addRootCharacteristic(dbRootCharacteristic);

    RuleKey ruleKey1 = RuleKey.of("checkstyle", "import");
    when(ruleCache.exists(ruleKey1)).thenReturn(false);
    // To be disabled as rule does not exists
    Requirement dbRequirement = new Requirement().setRuleKey(ruleKey1)
      .setFunction("linear").setFactor(WorkUnit.create(30.0, WorkUnit.MINUTES)).setCharacteristic(dbCharacteristic);

    when(sqaleModelFinder.findAll()).thenReturn(dbModel);

    TechnicalDebtModel result = manager.initAndMergePlugins(ValidationMessages.create(), ruleCache);

    verify(service).disable(eq(dbRequirement), eq(session));
    verifyNoMoreInteractions(service);
    assertThat(result.requirements()).isEmpty();
  }

  @Test
  public void fail_when_plugin_defines_characteristics_not_defined_in_default_model() throws Exception {
    try {
      // Default and db model
      Characteristic defaultRootCharacteristic = new Characteristic().setKey("PORTABILITY");
      new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setParent(defaultRootCharacteristic);
      defaultModel.addRootCharacteristic(defaultRootCharacteristic);
      when(sqaleModelFinder.findAll()).thenReturn(defaultModel);

      // Java model
      TechnicalDebtModel javaModel = new TechnicalDebtModel();
      Characteristic javaRootCharacteristic = new Characteristic().setKey("PORTABILITY");
      new Characteristic().setKey("NEW_CHARACTERISTIC").setParent(javaRootCharacteristic);
      javaModel.addRootCharacteristic(javaRootCharacteristic);

      Reader javaModelReader = mock(Reader.class);
      when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
      when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
      when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

      manager.initAndMergePlugins(ValidationMessages.create(), ruleCache);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The characteristic : NEW_CHARACTERISTIC cannot be used as it's not available in default characteristics.");
    } finally {
      verifyZeroInteractions(service);
    }
  }

  @Test
  public void provided_plugin_should_not_override_default_characteristics_name() throws FileNotFoundException {
    // Default and db model
    Characteristic defaultRootCharacteristic = new Characteristic().setKey("PORTABILITY").setName("Portability");
    new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setName("Compiler").setParent(defaultRootCharacteristic);
    defaultModel.addRootCharacteristic(defaultRootCharacteristic);
    when(sqaleModelFinder.findAll()).thenReturn(defaultModel);

    // Java model
    TechnicalDebtModel javaModel = new TechnicalDebtModel();
    Characteristic javaRootCharacteristic = new Characteristic().setKey("PORTABILITY").setName("New Portability Name");
    new Characteristic().setKey("COMPILER_RELATED_PORTABILITY").setName("New Compiler Name").setParent(javaRootCharacteristic);
    javaModel.addRootCharacteristic(javaRootCharacteristic);

    Reader javaModelReader = mock(Reader.class);
    when(technicalDebtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
    when(xmlImporter.importXML(eq(javaModelReader), any(ValidationMessages.class), eq(ruleCache))).thenReturn(javaModel);
    when(technicalDebtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));

    TechnicalDebtModel model = manager.initAndMergePlugins(ValidationMessages.create(), ruleCache);

    // Default model values
    assertThat(model.characteristicByKey("PORTABILITY").name()).isEqualTo("Portability");
    assertThat(model.characteristicByKey("COMPILER_RELATED_PORTABILITY").name()).isEqualTo("Compiler");

    // Plugin has renamed it, but the value should stay as defined by default model
    assertThat(model.characteristicByKey("PORTABILITY").name()).isEqualTo("Portability");

    verifyZeroInteractions(service);
  }

}
