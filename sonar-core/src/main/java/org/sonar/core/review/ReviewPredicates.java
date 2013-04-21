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
package org.sonar.core.review;

import com.google.common.base.Predicate;
import org.apache.commons.lang.ArrayUtils;

import javax.annotation.Nullable;

/**
 * @since 2.14
 */
public final class ReviewPredicates {

  private ReviewPredicates() {
  }

  public static Predicate<ReviewDto> status(String... statuses) {
    return new StatusPredicate(statuses);
  }

  public static Predicate<ReviewDto> resolution(String... resolutions) {
    return new ResolutionPredicate(resolutions);
  }

  public static Predicate<ReviewDto> manualViolation() {
    return ManualViolationPredicate.INSTANCE;
  }

  public static Predicate<ReviewDto> manualSeverity() {
    return ManualSeverityPredicate.INSTANCE;
  }

  private static final class StatusPredicate implements Predicate<ReviewDto> {
    private String[] statuses;

    private StatusPredicate(String... statuses) {
      this.statuses = statuses;
    }

    public boolean apply(@Nullable ReviewDto review) {
      return review!=null && ArrayUtils.contains(statuses, review.getStatus());
    }
  }

  private static final class ResolutionPredicate implements Predicate<ReviewDto> {
    private String[] resolutions;

    private ResolutionPredicate(String... resolutions) {
      this.resolutions = resolutions;
    }

    public boolean apply(@Nullable ReviewDto review) {
      return review!=null && ArrayUtils.contains(resolutions, review.getResolution());
    }
  }

  private static final class ManualViolationPredicate implements Predicate<ReviewDto> {
    private static final ManualViolationPredicate INSTANCE = new ManualViolationPredicate();

    private ManualViolationPredicate() {
    }

    public boolean apply(@Nullable ReviewDto review) {
      return review!=null && review.isManualViolation();
    }
  }

  private static final class ManualSeverityPredicate implements Predicate<ReviewDto> {
    private static final ManualSeverityPredicate INSTANCE = new ManualSeverityPredicate();

    private ManualSeverityPredicate() {
    }

    public boolean apply(@Nullable ReviewDto review) {
      return review!=null && review.isManualSeverity();
    }
  }
}
