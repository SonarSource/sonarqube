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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;

public class NewAdHocRuleTest {
  @org.junit.Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void fail_if_engine_id_is_not_set() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("'engine id' not expected to be null for an ad hoc rule");

    new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().build());
  }

  @Test
  public void test_equals_and_hashcode() {
    NewAdHocRule adHocRule1 = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build());
    NewAdHocRule adHocRule2 = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build());
    NewAdHocRule anotherAdHocRule = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("another").build());

    assertThat(adHocRule1).isEqualTo(adHocRule1);
    assertThat(adHocRule1).isEqualTo(adHocRule2);
    assertThat(adHocRule1).isNotEqualTo(null);
    assertThat(adHocRule1).isNotEqualTo(anotherAdHocRule);

    assertThat(adHocRule1.hashCode()).isEqualTo(adHocRule1.hashCode());
    assertThat(adHocRule1.hashCode()).isEqualTo(adHocRule2.hashCode());
    assertThat(adHocRule1.hashCode()).isNotEqualTo(anotherAdHocRule.hashCode());
  }
}
