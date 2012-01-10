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
package org.sonar.plugins.core.sensors;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewQuery;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.Mockito.*;

public class ManualViolationInjectorTest {

  @Test
  public void shouldInjectManualViolationsDefinedByReviews() {
    ReviewDao reviewDao = mock(ReviewDao.class);
    final Date reviewCreatedAt = DateUtils.parseDate("2011-12-25");
    ReviewDto reviewDto = new ReviewDto().setRuleId(3).setViolationPermanentId(100).setCreatedAt(reviewCreatedAt).setSeverity("BLOCKER");
    when(reviewDao.selectByQuery(Matchers.<ReviewQuery>anyObject())).thenReturn(Arrays.<ReviewDto>asList(reviewDto));
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findById(3)).thenReturn(new Rule());
    DecoratorContext context = mock(DecoratorContext.class);
    ManualViolationInjector injector = new ManualViolationInjector(reviewDao, ruleFinder);

    injector.decorate(new Project("key").setId(100), context);

    verify(context, times(1)).saveViolation(argThat(new BaseMatcher<Violation>() {
      public boolean matches(Object o) {
        Violation v = (Violation) o;
        return v.getPermanentId() == 100 && v.getRule() != null && v.isManual() && v.getCreatedAt().equals(reviewCreatedAt)
            && v.getSeverity().equals(RulePriority.BLOCKER);
      }

      public void describeTo(Description description) {
      }
    }));
  }
}
