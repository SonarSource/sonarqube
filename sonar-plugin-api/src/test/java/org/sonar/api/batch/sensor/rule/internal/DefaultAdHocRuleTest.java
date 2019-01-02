/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.rule.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.rules.RuleType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultAdHocRuleTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void store() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultAdHocRule rule = new DefaultAdHocRule(storage)
      .engineId("engine")
      .ruleId("ruleId")
      .name("name")
      .description("desc")
      .severity(Severity.BLOCKER)
      .type(RuleType.CODE_SMELL);
      rule.save();

    assertThat(rule.engineId()).isEqualTo("engine");
    assertThat(rule.ruleId()).isEqualTo("ruleId");
    assertThat(rule.name()).isEqualTo("name");
    assertThat(rule.description()).isEqualTo("desc");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);

    verify(storage).store(any(DefaultAdHocRule.class));
  }


  @Test
  public void description_is_optional() {
    SensorStorage storage = mock(SensorStorage.class);
    new DefaultAdHocRule(storage)
      .engineId("engine")
      .ruleId("ruleId")
      .name("name")
      .severity(Severity.BLOCKER)
      .type(RuleType.CODE_SMELL)
      .save();

    verify(storage).store(any(DefaultAdHocRule.class));
  }

  @Test
  public void fail_to_store_if_no_engine_id() {
    SensorStorage storage = mock(SensorStorage.class);
    NewAdHocRule rule = new DefaultAdHocRule(storage)
      .engineId(" ")
      .ruleId("ruleId")
      .name("name")
      .description("desc")
      .severity(Severity.BLOCKER)
      .type(RuleType.CODE_SMELL);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Engine id is mandatory");
    rule.save();
  }

  @Test
  public void fail_to_store_if_no_rule_id() {
    SensorStorage storage = mock(SensorStorage.class);
    NewAdHocRule rule = new DefaultAdHocRule(storage)
      .engineId("engine")
      .ruleId("  ")
      .name("name")
      .description("desc")
      .severity(Severity.BLOCKER)
      .type(RuleType.CODE_SMELL);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Rule id is mandatory");
    rule.save();
  }

  @Test
  public void fail_to_store_if_no_name() {
    SensorStorage storage = mock(SensorStorage.class);
    NewAdHocRule rule = new DefaultAdHocRule(storage)
      .engineId("engine")
      .ruleId("ruleId")
      .name("  ")
      .description("desc")
      .severity(Severity.BLOCKER)
      .type(RuleType.CODE_SMELL);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Name is mandatory");
    rule.save();
  }


  @Test
  public void fail_to_store_if_no_severity() {
    SensorStorage storage = mock(SensorStorage.class);
    NewAdHocRule rule = new DefaultAdHocRule(storage)
      .engineId("engine")
      .ruleId("ruleId")
      .name("name")
      .description("desc")
      .type(RuleType.CODE_SMELL);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Severity is mandatory");
    rule.save();
  }

  @Test
  public void fail_to_store_if_no_type() {
    SensorStorage storage = mock(SensorStorage.class);
    NewAdHocRule rule = new DefaultAdHocRule(storage)
      .engineId("engine")
      .ruleId("ruleId")
      .name("name")
      .description("desc")
      .severity(Severity.BLOCKER);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Type is mandatory");
    rule.save();
  }

}