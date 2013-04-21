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
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.api.workflow.internal.DefaultWorkflowContext;

import static org.fest.assertions.Assertions.assertThat;

public class AdminRoleConditionTest {
  @Test
  public void verifiedIfAdminRole() {
    AdminRoleCondition condition = new AdminRoleCondition();
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    context.setIsAdmin(true);
    assertThat(condition.doVerify(new DefaultReview(), context)).isTrue();
  }

  @Test
  public void failIfNotAdminRole() {
    AdminRoleCondition condition = new AdminRoleCondition();
    DefaultWorkflowContext context = new DefaultWorkflowContext();
    context.setIsAdmin(false);
    assertThat(condition.doVerify(new DefaultReview(), context)).isFalse();
  }
}
