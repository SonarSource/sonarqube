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
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.core.technicaldebt.TechnicalDebtRequirement;
import org.sonar.core.technicaldebt.WorkUnit;
import org.sonar.core.technicaldebt.WorkUnitConverter;

import java.util.Collection;
import java.util.Collections;

public class LinearWithOffsetFunctionTest {

  private TechnicalDebtRequirement requirement;
  private Function function = new LinearWithOffsetFunction(new WorkUnitConverter(new Settings()));

  @Before
  public void before() {
    requirement = Mockito.mock(TechnicalDebtRequirement.class);
    Mockito.when(requirement.getRemediationFactor()).thenReturn(WorkUnit.createInDays(3.14));
    Mockito.when(requirement.getOffset()).thenReturn(WorkUnit.createInDays(2.12));
  }

  @Test
  public void zeroIfNoViolations() {
    Assert.assertThat(function.costInHours(requirement, Collections.<Violation>emptyList()), Is.is(0.0));
  }

  @Test
  public void countEveryViolation() {
    Collection<Violation> violations = Lists.newArrayList();

    Rule rule = Rule.create("checkstyle", "foo", "Foo");
    violations.add(new Violation(rule));
    Assert.assertThat(function.costInHours(requirement, violations), Is.is(2.12 + 3.14));

    violations.add(new Violation(rule));
    Assert.assertThat(function.costInHours(requirement, violations), Is.is(2.12 + 3.14 * 2));
  }
}
