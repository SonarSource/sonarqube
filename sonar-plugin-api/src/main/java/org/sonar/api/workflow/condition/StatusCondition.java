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

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sonar.api.workflow.Review;
import org.sonar.api.workflow.WorkflowContext;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;

/**
 * @since 3.1
 */
@Beta
public final class StatusCondition extends Condition {
  private final Set<String> statuses;

  public StatusCondition(Set<String> statuses) {
    super(false);
    Preconditions.checkNotNull(statuses);
    Preconditions.checkArgument(!statuses.isEmpty(), "No statuses defined");
    this.statuses = statuses;
  }

  public StatusCondition(String... statuses) {
    this(Sets.newLinkedHashSet(Arrays.asList(statuses)));
  }

  @Override
  public boolean doVerify(@Nullable Review review, WorkflowContext context) {
    return review != null && statuses.contains(review.getStatus());
  }

  @VisibleForTesting
  Set<String> getStatuses() {
    return ImmutableSet.copyOf(statuses);
  }
}
