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
package org.sonar.plugins.core.technicaldebt.functions;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.core.technicaldebt.TechnicalDebtRequirement;
import org.sonar.plugins.core.technicaldebt.WorkUnit;
import org.sonar.plugins.core.technicaldebt.WorkUnitConverter;

import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LinearFunctionTest {

  private TechnicalDebtRequirement requirement;
  private Function function = new LinearFunction(new WorkUnitConverter(new PropertiesConfiguration()));

  @Before
  public void before() {
    requirement = mock(TechnicalDebtRequirement.class);
    when(requirement.getRemediationFactor()).thenReturn(WorkUnit.createInDays(3.14));
  }

  @Test
  public void zeroIfNoViolations() {
    assertThat(function.calculateCost(requirement, Collections.<Violation>emptyList()), is(0.0));
  }

  @Test
  public void countEveryViolation() {
    Collection<Violation> violations = Lists.newArrayList();

    Rule rule = Rule.create("checkstyle", "foo", "Foo");
    violations.add(new Violation(rule));
    assertThat(function.calculateCost(requirement, violations), is(3.14));

    violations.add(new Violation(rule));
    assertThat(function.calculateCost(requirement, violations), is(3.14 * 2));
  }

  @Test
  public void usePointsWhenAvailable() {
    Collection<Violation> violations = Lists.newArrayList();

    Rule rule = Rule.create("checkstyle", "foo", "Foo");
    violations.add(new Violation(rule).setCost(20.5));
    violations.add(new Violation(rule).setCost(3.8));
    violations.add(new Violation(rule));
    assertThat(function.calculateCost(requirement, violations), is(3.14 * (20.5 + 3.8 + 1)));
  }
}
