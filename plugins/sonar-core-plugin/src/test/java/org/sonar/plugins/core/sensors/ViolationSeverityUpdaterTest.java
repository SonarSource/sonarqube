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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;

import java.util.Arrays;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ViolationSeverityUpdaterTest {

  private Resource project;

  @Before
  public void setUp() {
    project = new Project("foo").setId(10);
  }

  @Test
  public void shouldUpdateSeverityFixedByEndUsers() {
    ReviewDao reviewDao = mock(ReviewDao.class);
    when(reviewDao.selectOpenByResourceId(anyInt(), (Predicate<ReviewDto>[])anyVararg())).thenReturn(Lists.newArrayList(
        new ReviewDto().setManualSeverity(true).setSeverity("BLOCKER").setViolationPermanentId(380)));
    DecoratorContext context = mock(DecoratorContext.class);
    Violation newViolation = Violation.create(new Rule(), project).setSeverity(RulePriority.MINOR);
    Violation unchangedViolation = Violation.create(new Rule(), project).setPermanentId(120).setSeverity(RulePriority.MINOR);
    Violation changedViolation = Violation.create(new Rule(), project).setPermanentId(380).setSeverity(RulePriority.MINOR);
    when(context.getViolations()).thenReturn(Arrays.<Violation>asList(newViolation, unchangedViolation, changedViolation));

    ViolationSeverityUpdater updater = new ViolationSeverityUpdater(reviewDao);
    updater.decorate(project, context);

    assertThat(newViolation.getSeverity(), Is.is(RulePriority.MINOR));
    assertThat(unchangedViolation.getSeverity(), Is.is(RulePriority.MINOR));
    assertThat(changedViolation.getSeverity(), Is.is(RulePriority.BLOCKER));
  }

  /**
   * Optimization
   */
  @Test
  public void shouldNotLoadReviewsIfNoTrackedViolations() {
    ReviewDao reviewDao = mock(ReviewDao.class);
    DecoratorContext context = mock(DecoratorContext.class);
    Violation newViolation = Violation.create(new Rule(), project).setSeverity(RulePriority.MINOR);
    when(context.getViolations()).thenReturn(Arrays.<Violation>asList(newViolation));

    ViolationSeverityUpdater updater = new ViolationSeverityUpdater(reviewDao);
    updater.decorate(project, context);

    assertThat(newViolation.getSeverity(), Is.is(RulePriority.MINOR));
    verifyZeroInteractions(reviewDao);
  }
}
