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
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Maps;
import org.sonar.api.batch.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewPredicates;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Severity of violations can be explicitely changed by end-users. In this case the severity is fixed and must not be changed
 * by rule engines.
 *
 * @since 2.13
 */
@DependsUpon(DecoratorBarriers.START_VIOLATION_TRACKING)
@DependedUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class ViolationSeverityUpdater implements Decorator {

  private ReviewDao reviewDao;

  public ViolationSeverityUpdater(ReviewDao reviewDao) {
    this.reviewDao = reviewDao;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public Class dependsUponViolationTracking() {
    // permanent ids of violations have been updated, so we can link them with reviews
    return ViolationTrackingDecorator.class;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (resource.getId()==null) {
      return;
    }
    Map<Integer, Violation> violationMap = filterViolationsPerPermanent(context.getViolations());
    if (!violationMap.isEmpty()) {
      Collection<ReviewDto> reviews = selectReviewsWithManualSeverity(resource.getId());
      for (ReviewDto review : reviews) {
        Violation violation = violationMap.get(review.getViolationPermanentId());
        if (violation != null) {
          violation.setSeverity(RulePriority.valueOf(review.getSeverity()));
        }
      }
    }
  }

  private Collection<ReviewDto> selectReviewsWithManualSeverity(long resourceId) {
    return reviewDao.selectOpenByResourceId(resourceId, ReviewPredicates.manualSeverity());
  }

  private Map<Integer, Violation> filterViolationsPerPermanent(List<Violation> violations) {
    Map<Integer, Violation> result = Maps.newHashMap();
    for (Violation violation : violations) {
      if (violation.getPermanentId() != null) {
        result.put(violation.getPermanentId(), violation);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
