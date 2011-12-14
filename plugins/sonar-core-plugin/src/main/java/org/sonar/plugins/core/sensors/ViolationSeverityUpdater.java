/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.batch.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.persistence.review.ReviewDao;
import org.sonar.persistence.review.ReviewDto;
import org.sonar.persistence.review.ReviewQuery;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    if (!ResourceUtils.isPersistable(resource)) {
      return;
    }
    Map<Integer, Violation> violationMap = filterViolationsPerPermanent(context.getViolations());
    if (!violationMap.isEmpty()) {
      Set<Integer> permanentIds = violationMap.keySet();
      List<ReviewDto> reviewDtos = selectReviewsWithManualSeverity(permanentIds);
      for (ReviewDto reviewDto : reviewDtos) {
        Violation violation = violationMap.get(reviewDto.getViolationPermanentId());
        if (violation != null) {
          violation.setSeverity(RulePriority.valueOf(reviewDto.getSeverity()));
        }
      }
    }
  }

  private List<ReviewDto> selectReviewsWithManualSeverity(Set<Integer> permanentIds) {
    ReviewQuery query = ReviewQuery.create()
      .setManualSeverity(Boolean.TRUE)
      .setViolationPermanentIds(Lists.newArrayList(permanentIds));
    return reviewDao.selectByQuery(query);
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
