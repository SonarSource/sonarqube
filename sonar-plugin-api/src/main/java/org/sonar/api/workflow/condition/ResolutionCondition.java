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
public final class ResolutionCondition extends Condition {
  private final Set<String> resolutions;

  public ResolutionCondition(Set<String> resolutions) {
    super(false);
    Preconditions.checkNotNull(resolutions);
    Preconditions.checkArgument(!resolutions.isEmpty(), "No resolutions defined");
    this.resolutions = resolutions;
  }

  public ResolutionCondition(String... resolutions) {
    this(Sets.newLinkedHashSet(Arrays.asList(resolutions)));
  }

  @Override
  public boolean doVerify(@Nullable Review review, WorkflowContext context) {
    return review != null && resolutions.contains(review.getResolution());
  }

  @VisibleForTesting
  Set<String> getResolutions() {
    return ImmutableSet.copyOf(resolutions);
  }
}
