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
package org.sonar.core.review;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ReviewPredicatesTest {
  @Test
  public void testStatusPredicate() {
    Predicate<ReviewDto> predicate = ReviewPredicates.status(ReviewDto.STATUS_REOPENED);
    Collection<ReviewDto> filtered = Collections2.filter(Lists.<ReviewDto>newArrayList(
        new ReviewDto().setStatus(ReviewDto.STATUS_OPEN),
        new ReviewDto().setStatus(ReviewDto.STATUS_REOPENED),
        new ReviewDto().setStatus(ReviewDto.STATUS_REOPENED)), predicate);

    assertThat(filtered.size(), is(2));
  }

  @Test
  public void testResolutionPredicate() {
    Predicate<ReviewDto> predicate = ReviewPredicates.resolution(ReviewDto.RESOLUTION_FALSE_POSITIVE);
    Collection<ReviewDto> filtered = Collections2.filter(Lists.<ReviewDto>newArrayList(
        new ReviewDto().setResolution(null),
        new ReviewDto().setResolution(ReviewDto.RESOLUTION_FALSE_POSITIVE),
        new ReviewDto().setResolution(ReviewDto.RESOLUTION_FIXED)), predicate);

    assertThat(filtered.size(), is(1));
  }

  @Test
  public void testManualViolationPredicate() {
    Predicate<ReviewDto> predicate = ReviewPredicates.manualViolation();
    Collection<ReviewDto> filtered = Collections2.filter(Lists.<ReviewDto>newArrayList(
        new ReviewDto().setManualViolation(false),
        new ReviewDto().setManualViolation(false),
        new ReviewDto().setManualViolation(true)), predicate);

    assertThat(filtered.size(), is(1));
  }

  @Test
  public void testManualSeverityPredicate() {
    Predicate<ReviewDto> predicate = ReviewPredicates.manualSeverity();
    Collection<ReviewDto> filtered = Collections2.filter(Lists.<ReviewDto>newArrayList(
        new ReviewDto().setManualSeverity(false),
        new ReviewDto().setManualSeverity(false),
        new ReviewDto().setManualSeverity(true)), predicate);

    assertThat(filtered.size(), is(1));
  }
}
