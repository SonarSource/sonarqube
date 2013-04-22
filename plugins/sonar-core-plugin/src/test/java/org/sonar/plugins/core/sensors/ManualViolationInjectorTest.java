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
package org.sonar.plugins.core.sensors;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;

import java.util.Date;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ManualViolationInjectorTest {

  @Test
  public void shouldInjectManualViolationsDefinedByReviews() {
    final Date createdAt = DateUtils.parseDate("2011-12-25");
    ReviewDto review = new ReviewDto().setRuleId(3).setViolationPermanentId(100).setCreatedAt(createdAt).setSeverity(RulePriority.BLOCKER.toString());
    ReviewDao dao = mock(ReviewDao.class);
    when(dao.selectOpenByResourceId(eq(100L), (Predicate<ReviewDto>[])anyVararg())).thenReturn(Lists.newArrayList(review));
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findById(3)).thenReturn(new Rule());
    DecoratorContext context = mock(DecoratorContext.class);
    ManualViolationInjector injector = new ManualViolationInjector(dao, ruleFinder);

    injector.decorate(new Project("key").setId(100), context);

    verify(context, times(1)).saveViolation(argThat(new ArgumentMatcher<Violation>() {
      @Override
      public boolean matches(Object o) {
        Violation v = (Violation) o;
        return v.getPermanentId() == 100 && v.getRule() != null && v.isManual() && v.getCreatedAt().equals(createdAt)
            && v.getSeverity().equals(RulePriority.BLOCKER);
      }
    }));
  }
}
