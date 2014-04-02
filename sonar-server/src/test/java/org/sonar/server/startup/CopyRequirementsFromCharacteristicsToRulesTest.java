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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.TestDatabase;
import org.sonar.core.technicaldebt.db.RequirementDao;
import org.sonar.server.rule.RuleRegistry;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CopyRequirementsFromCharacteristicsToRulesTest extends AbstractDaoTestCase {

  @ClassRule
  public static TestDatabase db = new TestDatabase().schema(CopyRequirementsFromCharacteristicsToRulesTest.class, "schema.sql");

  @Mock
  ServerUpgradeStatus status;

  @Mock
  System2 system2;

  @Mock
  RuleRegistry ruleRegistry;

  CopyRequirementsFromCharacteristicsToRules service;

  @Before
  public void setUp() throws Exception {
    when(system2.now()).thenReturn(DateUtils.parseDateTime("2014-03-13T19:10:03+0100").getTime());
    service = new CopyRequirementsFromCharacteristicsToRules(db.database(), new RequirementDao(getMyBatis()), ruleRegistry, status, system2);
  }

  @Test
  public void copy_requirements_from_characteristics_to_rules() throws Exception {
    setupData("requirements");
    db.prepareDbUnit(getClass(), "copy_requirements_from_characteristics_to_rules.xml");

    when(status.isUpgraded()).thenReturn(true);
    when(status.getInitialDbVersion()).thenReturn(498);

    service.start();

    db.assertDbUnit(getClass(), "copy_requirements_from_characteristics_to_rules_result.xml", "rules");
    verify(ruleRegistry).reindex();
  }

  @Test
  public void remove_requirements_data_from_characteristics() throws Exception {
    db.prepareDbUnit(getClass(), "remove_requirements_data_from_characteristics.xml");

    when(status.isUpgraded()).thenReturn(true);
    when(status.getInitialDbVersion()).thenReturn(498);

    service.start();

    db.assertDbUnit(getClass(), "remove_requirements_data_from_characteristics_result.xml", "characteristics");
    verify(ruleRegistry).reindex();
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
    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new CopyRequirementsFromCharacteristicsToRules.RuleRow()
      .setDefaultCharacteristicId(1).setCharacteristicId(1)
      .setDefaultFunction("LINEAR_OFFSET").setFunction("LINEAR_OFFSET")
      .setDefaultCoefficient("5h").setCoefficient("5h")
      .setDefaultOffset("10min").setOffset("10min")
    )).isTrue();

    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new CopyRequirementsFromCharacteristicsToRules.RuleRow()
      .setDefaultCharacteristicId(1).setCharacteristicId(2)
      .setDefaultFunction("LINEAR_OFFSET").setFunction("LINEAR_OFFSET")
      .setDefaultCoefficient("5h").setCoefficient("5h")
      .setDefaultOffset("10min").setOffset("10min")
    )).isFalse();

    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new CopyRequirementsFromCharacteristicsToRules.RuleRow()
      .setDefaultCharacteristicId(1).setCharacteristicId(1)
      .setDefaultFunction("LINEAR_OFFSET").setFunction("LINEAR_OFFSET")
      .setDefaultCoefficient("5h").setCoefficient("4h")
      .setDefaultOffset("10min").setOffset("5min")
    )).isFalse();

    assertThat(CopyRequirementsFromCharacteristicsToRules.isDebtDefaultValuesSameAsOverriddenValues(new CopyRequirementsFromCharacteristicsToRules.RuleRow()
      .setDefaultCharacteristicId(1).setCharacteristicId(1)
      .setDefaultFunction("CONSTANT_ISSUE").setFunction("LINEAR")
      .setDefaultCoefficient(null).setCoefficient("5h")
      .setDefaultOffset("10min").setOffset(null)
    )).isFalse();
  }
}
