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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.workflow.Review;
import org.sonar.api.workflow.internal.DefaultReview;
import org.sonar.api.workflow.internal.DefaultWorkflowContext;

import static org.fest.assertions.Assertions.assertThat;

public class StatusConditionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void failIfNoStatus() {
    thrown.expect(IllegalArgumentException.class);
    new StatusCondition();
  }


  @Test
  public void getStatuses() {
    StatusCondition condition = new StatusCondition("OPEN", "CLOSED");
    assertThat(condition.getStatuses()).containsOnly("OPEN", "CLOSED");
  }

  @Test
  public void doVerify_review_has_status() {
    Condition condition = new StatusCondition("OPEN", "CLOSED");
    Review review = new DefaultReview().setStatus("CLOSED");
    assertThat(condition.doVerify(review, new DefaultWorkflowContext())).isTrue();
  }

  @Test
  public void doVerify_review_does_not_have_status() {
    Condition condition = new StatusCondition("OPEN", "CLOSED");
    Review review = new DefaultReview().setStatus("OTHER");
    assertThat(condition.doVerify(review, new DefaultWorkflowContext())).isFalse();
  }
}
