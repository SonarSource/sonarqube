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
package org.sonar.core.technicaldebt.functions;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;
import org.sonar.core.technicaldebt.TechnicalDebtRequirement;
import org.sonar.core.technicaldebt.WorkUnit;

import java.util.Collection;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LinearFunctionTest {

  private TechnicalDebtRequirement requirement;
  private Function function;

  @Before
  public void before() {
    Settings settings = new Settings();
    settings.setProperty(TechnicalDebtConverter.PROPERTY_HOURS_IN_DAY, 8);
    function = new LinearFunction(new TechnicalDebtConverter(settings));

    requirement = mock(TechnicalDebtRequirement.class);
    when(requirement.getRemediationFactor()).thenReturn(WorkUnit.createInDays(3.14));
  }

  @Test
  public void zero_if_no_violations() {
    assertThat(function.costInHours(requirement, Collections.<Violation>emptyList())).isEqualTo(0.0);
  }

  @Test
  public void count_every_violation() {
    Collection<Violation> violations = Lists.newArrayList();

    Rule rule = Rule.create("checkstyle", "foo", "Foo");
    violations.add(new Violation(rule));
    assertThat(function.costInHours(requirement, violations)).isEqualTo(3.14);

    violations.add(new Violation(rule));
    assertThat(function.costInHours(requirement, violations)).isEqualTo(3.14 * 2);
  }

  @Test
  public void use_points_when_available() {
    Collection<Violation> violations = Lists.newArrayList();

    Rule rule = Rule.create("checkstyle", "foo", "Foo");
    violations.add(new Violation(rule).setCost(20.5));
    violations.add(new Violation(rule).setCost(3.8));
    violations.add(new Violation(rule));
    assertThat(function.costInHours(requirement, violations)).isEqualTo(3.14 * (20.5 + 3.8 + 1));
  }

  @Test
  public void cost_in_minutes() {
    when(requirement.getRemediationFactor()).thenReturn(WorkUnit.create(10d, WorkUnit.MINUTES));
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setEffortToFix(2.0);
    assertThat(function.costInMinutes(requirement, issue)).isEqualTo(20L);
  }

  @Test
  public void cost_in_minutes_use_default_cost_when_no_effort_to_fix_on_issue() {
    when(requirement.getRemediationFactor()).thenReturn(WorkUnit.create(10d, WorkUnit.MINUTES));
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setEffortToFix(null);
    assertThat(function.costInMinutes(requirement, issue)).isEqualTo(10L);
  }
}
