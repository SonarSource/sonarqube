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
package org.sonar.core.review;

import com.google.common.base.Predicate;
import org.apache.commons.lang.ArrayUtils;

/**
 * @since 2.14
 */
public final class ReviewPredicates {

  private ReviewPredicates() {
  }

  public static class StatusPredicate implements Predicate<ReviewDto> {
    private String[] statuses;

    public static StatusPredicate create(String... statuses) {
      return new StatusPredicate(statuses);
    }

    private StatusPredicate(String... statuses) {
      this.statuses = statuses;
    }

    public boolean apply(ReviewDto review) {
      return ArrayUtils.contains(statuses, review.getStatus());
    }
  }

  public static class ResolutionPredicate implements Predicate<ReviewDto> {
    private String[] resolutions;

    public static ResolutionPredicate create(String... resolutions) {
      return new ResolutionPredicate(resolutions);
    }

    private ResolutionPredicate(String... resolutions) {
      this.resolutions = resolutions;
    }

    public boolean apply(ReviewDto review) {
      return ArrayUtils.contains(resolutions, review.getResolution());
    }
  }

  public static class ManualViolationPredicate implements Predicate<ReviewDto> {
    private static final ManualViolationPredicate INSTANCE = new ManualViolationPredicate();

    public static ManualViolationPredicate create() {
      return INSTANCE;
    }

    private ManualViolationPredicate() {
    }

    public boolean apply(ReviewDto review) {
      return review.isManualViolation();
    }
  }

  public static class ManualSeverityPredicate implements Predicate<ReviewDto> {
    private static final ManualSeverityPredicate INSTANCE = new ManualSeverityPredicate();

    public static ManualSeverityPredicate create() {
      return INSTANCE;
    }

    private ManualSeverityPredicate() {
    }

    public boolean apply(ReviewDto review) {
      return review.isManualSeverity();
    }
  }
}
