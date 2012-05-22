/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.review.workflow.condition;

import org.junit.Test;
import org.sonar.core.review.workflow.review.DefaultReview;
import org.sonar.core.review.workflow.review.DefaultWorkflowContext;
import org.sonar.core.review.workflow.review.Review;

import static org.fest.assertions.Assertions.assertThat;

public class ResolutionConditionTest {
  @Test
  public void getResolutions() {
    ResolutionCondition condition = new ResolutionCondition("", "RESOLVED");
    assertThat(condition.getResolutions()).containsOnly("", "RESOLVED");
  }

  @Test
  public void doVerify_review_has_resolution() {
    Condition condition = new ResolutionCondition("", "RESOLVED");
    Review review = new DefaultReview().setResolution("");
    assertThat(condition.doVerify(review, new DefaultWorkflowContext())).isTrue();
  }

  @Test
  public void doVerify_review_does_not_have_resolution() {
    Condition condition = new ResolutionCondition("", "RESOLVED");
    Review review = new DefaultReview().setResolution("OTHER");
    assertThat(condition.doVerify(review, new DefaultWorkflowContext())).isFalse();
  }
}
