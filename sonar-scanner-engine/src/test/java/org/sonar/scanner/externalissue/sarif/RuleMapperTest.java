/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.externalissue.sarif;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.rule.internal.DefaultAdHocRule;
import org.sonar.core.sarif.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class RuleMapperTest {

  private static final String WARNING = "warning";
  private static final String RULE_ID = "test_rules_id";
  private static final String DRIVER_NAME = "driverName";

  @Mock
  private SensorContext sensorContext;

  @InjectMocks
  RuleMapper ruleMapper;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(sensorContext.newAdHocRule()).thenReturn(new DefaultAdHocRule());
  }

  @Test
  public void mapRule_shouldCorrectlyMapToNewAdHocRule() {
    Rule rule = Rule.builder().id(RULE_ID).build();
    NewAdHocRule expected = new DefaultAdHocRule()
      .severity(ResultMapper.DEFAULT_SEVERITY)
      .type(ResultMapper.DEFAULT_TYPE)
      .ruleId(RULE_ID)
      .engineId(DRIVER_NAME)
      .name(String.join(":", DRIVER_NAME, RULE_ID))
      .cleanCodeAttribute(ResultMapper.DEFAULT_CLEAN_CODE_ATTRIBUTE)
      .addDefaultImpact(ResultMapper.DEFAULT_SOFTWARE_QUALITY, org.sonar.api.issue.impact.Severity.MEDIUM);

    NewAdHocRule result = ruleMapper.mapRule(rule, DRIVER_NAME, WARNING, WARNING);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

}
