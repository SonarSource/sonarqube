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

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.sonar.core.review.workflow.review.Review;
import org.sonar.core.review.workflow.review.WorkflowContext;

/**
 * @since 3.1
 */
@Beta
public final class HasReviewPropertyCondition extends Condition {

  private final String propertyKey;

  public HasReviewPropertyCondition(String propertyKey) {
    super(false);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(propertyKey));
    this.propertyKey = propertyKey;
  }

  public String getPropertyKey() {
    return propertyKey;
  }

  @Override
  public boolean doVerify(Review review, WorkflowContext context) {
    return !Strings.isNullOrEmpty(review.getProperties().get(propertyKey));
  }
}
