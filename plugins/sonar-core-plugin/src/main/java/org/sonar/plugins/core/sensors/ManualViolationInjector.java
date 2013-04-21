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

import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewPredicates;

import java.util.Collection;

@DependedUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class ManualViolationInjector implements Decorator {

  private ReviewDao reviewDao;
  private RuleFinder ruleFinder;

  public ManualViolationInjector(ReviewDao reviewDao, RuleFinder ruleFinder) {
    this.reviewDao = reviewDao;
    this.ruleFinder = ruleFinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (resource.getId() != null) {
      Collection<ReviewDto> openReviews = reviewDao.selectOpenByResourceId(resource.getId(),
          ReviewPredicates.status(ReviewDto.STATUS_OPEN),
          ReviewPredicates.manualViolation());
      for (ReviewDto openReview : openReviews) {
        if (openReview.getRuleId() == null) {
          LoggerFactory.getLogger(getClass()).warn("No rule is defined on the review with id: " + openReview.getId());
        }
        if (openReview.getViolationPermanentId() == null) {
          LoggerFactory.getLogger(getClass()).warn("Permanent id of manual violation is missing on the review with id: " + openReview.getId());
        }
        Violation violation = Violation.create(ruleFinder.findById(openReview.getRuleId()), resource);
        violation.setManual(true);
        violation.setLineId(openReview.getLine());
        violation.setPermanentId(openReview.getViolationPermanentId());
        violation.setSwitchedOff(false);
        violation.setCreatedAt(openReview.getCreatedAt());
        violation.setMessage(openReview.getTitle());
        violation.setSeverity(RulePriority.valueOf(openReview.getSeverity()));
        context.saveViolation(violation);
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
