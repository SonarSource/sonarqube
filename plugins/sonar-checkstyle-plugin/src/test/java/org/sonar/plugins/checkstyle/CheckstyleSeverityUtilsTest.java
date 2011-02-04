/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.checkstyle;

import org.junit.Test;
import org.sonar.api.rules.RulePriority;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class CheckstyleSeverityUtilsTest {

  @Test
  public void testToSeverity() {
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.BLOCKER), is("error"));
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.CRITICAL), is("error"));
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.MAJOR), is("warning"));
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.MINOR), is("info"));
    assertThat(CheckstyleSeverityUtils.toSeverity(RulePriority.INFO), is("info"));
  }

  @Test
  public void testFromSeverity() {
    assertThat(CheckstyleSeverityUtils.fromSeverity("error"), is(RulePriority.BLOCKER));
    assertThat(CheckstyleSeverityUtils.fromSeverity("warning"), is(RulePriority.MAJOR));
    assertThat(CheckstyleSeverityUtils.fromSeverity("info"), is(RulePriority.INFO));
    assertThat(CheckstyleSeverityUtils.fromSeverity(""), nullValue());
  }
}
