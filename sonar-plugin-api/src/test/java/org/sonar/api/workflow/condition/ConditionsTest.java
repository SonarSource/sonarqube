/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.workflow.condition;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ConditionsTest {
  @Test
  public void not() {
    StatusCondition target = new StatusCondition("OPEN");
    Condition not = Conditions.not(target);
    assertThat(not).isInstanceOf(NotCondition.class);
    assertThat(((NotCondition) not).getCondition()).isSameAs(target);
  }

  @Test
  public void hasReviewProperty() {
    Condition condition = Conditions.hasReviewProperty("foo");
    assertThat(condition).isInstanceOf(HasReviewPropertyCondition.class);
    assertThat(((HasReviewPropertyCondition) condition).getPropertyKey()).isEqualTo("foo");
  }

  @Test
  public void hasProjectProperty() {
    Condition condition = Conditions.hasProjectProperty("foo");
    assertThat(condition).isInstanceOf(HasProjectPropertyCondition.class);
    assertThat(((HasProjectPropertyCondition) condition).getPropertyKey()).isEqualTo("foo");
  }

  @Test
  public void hasAdminRole() {
    Condition condition = Conditions.hasAdminRole();
    assertThat(condition).isInstanceOf(AdminRoleCondition.class);
  }

  @Test
  public void statuses() {
    Condition condition = Conditions.statuses("OPEN", "CLOSED");
    assertThat(condition).isInstanceOf(StatusCondition.class);
    assertThat(((StatusCondition) condition).getStatuses()).containsOnly("OPEN", "CLOSED");
  }

  @Test
  public void resolutions() {
    Condition condition = Conditions.resolutions("", "RESOLVED");
    assertThat(condition).isInstanceOf(ResolutionCondition.class);
    assertThat(((ResolutionCondition) condition).getResolutions()).containsOnly("", "RESOLVED");
  }
}
