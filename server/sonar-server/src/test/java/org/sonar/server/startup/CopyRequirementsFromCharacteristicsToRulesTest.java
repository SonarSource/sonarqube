/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
@RunWith(MockitoJUnitRunner.class)
public class CopyRequirementsFromCharacteristicsToRulesTest {

  @ClassRule
  public static DbTester db = new DbTester();

  @Mock
  System2 system2;

  DbClient dbClient;

  CopyRequirementsFromCharacteristicsToRules service;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDate("2014-03-13").getTime());
    dbClient = new DbClient(db.database(), db.myBatis(), new RuleDao(system2), new LoadedTemplateDao(db.myBatis()));
    service = new CopyRequirementsFromCharacteristicsToRules(dbClient, null);
  }

  @Test
  public void copy_requirements_from_characteristics_to_rules() throws Exception {
    db.prepareDbUnit(getClass(), "copy_requirements_from_characteristics_to_rules.xml");

    service.start();

    db.assertDbUnit(getClass(), "copy_requirements_from_characteristics_to_rules_result.xml", "rules", "loaded_templates");
  }

  /**
   * SONAR-5335
   */
  @Test
  public void convert_constant_issue_with_coeff_to_constant_issue_with_offset() throws Exception {
    db.prepareDbUnit(getClass(), "convert_constant_issue_with_coeff_to_constant_issue_with_offset.xml");

    service.start();

    db.assertDbUnit(getClass(), "convert_constant_issue_with_coeff_to_constant_issue_with_offset_result.xml", "rules");
  }

  @Test
  public void remove_requirements_data_from_characteristics() throws Exception {
    db.prepareDbUnit(getClass(), "remove_requirements_data_from_characteristics.xml");

    service.start();

    db.assertDbUnit(getClass(), "remove_requirements_data_from_characteristics_result.xml", "characteristics");
  }

  @Test
  public void do_nothing_when_already_executed() throws Exception {
    db.prepareDbUnit(getClass(), "do_nothing_when_already_executed.xml");

    service.start();

    db.assertDbUnit(getClass(), "do_nothing_when_already_executed_result.xml", "rules");
  }

  @Test
  public void convert_duration() throws Exception {
    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(1.0, "h")).isEqualTo("1h");
    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(15.0, "d")).isEqualTo("15d");
    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(5.0, "min")).isEqualTo("5min");
    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(5.0, "mn")).isEqualTo("5min");

    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(0.9, "h")).isEqualTo("1h");
    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(1.4, "h")).isEqualTo("1h");

    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(1.0, null)).isEqualTo("1d");
    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(null, "d")).isNull();

    assertThat(CopyRequirementsFromCharacteristicsToRules.convertDuration(0.0, "d")).isNull();
  }

  @Test
  public void is_debt_default_values_same_as_overridden_values() throws Exception {
    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new RuleDto()
        .setDefaultSubCharacteristicId(1).setSubCharacteristicId(1)
        .setDefaultRemediationFunction("LINEAR_OFFSET").setRemediationFunction("LINEAR_OFFSET")
        .setDefaultRemediationCoefficient("5h").setRemediationCoefficient("5h")
        .setDefaultRemediationOffset("10min").setRemediationOffset("10min")
    )).isTrue();

    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new RuleDto()
        .setDefaultSubCharacteristicId(1).setSubCharacteristicId(2)
        .setDefaultRemediationFunction("LINEAR_OFFSET").setRemediationFunction("LINEAR_OFFSET")
        .setDefaultRemediationCoefficient("5h").setRemediationCoefficient("5h")
        .setDefaultRemediationOffset("10min").setRemediationOffset("10min")
    )).isFalse();

    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new RuleDto()
        .setDefaultSubCharacteristicId(1).setSubCharacteristicId(1)
        .setDefaultRemediationFunction("LINEAR_OFFSET").setRemediationFunction("LINEAR_OFFSET")
        .setDefaultRemediationCoefficient("5h").setRemediationCoefficient("4h")
        .setDefaultRemediationOffset("10min").setRemediationOffset("5min")
    )).isFalse();

    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new RuleDto()
        .setDefaultSubCharacteristicId(1).setSubCharacteristicId(1)
        .setDefaultRemediationFunction("CONSTANT_ISSUE").setRemediationFunction("LINEAR")
        .setDefaultRemediationCoefficient(null).setRemediationCoefficient("5h")
        .setDefaultRemediationOffset("10min").setRemediationOffset(null)
    )).isFalse();
  }
}
