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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ReviewWorkflowDecoratorTest extends AbstractDaoTestCase {
  private ReviewWorkflowDecorator decorator;
  private ReviewNotifications notifications;

  @Before
  public void init() {
    notifications = mock(ReviewNotifications.class);
    ResourcePersister persister = mock(ResourcePersister.class);
    Snapshot snapshot = new Snapshot();
    snapshot.setId(1);
    snapshot.setResourceId(100);
    when(persister.getSnapshot(any(Resource.class))).thenReturn(snapshot);

    decorator = new ReviewWorkflowDecorator(notifications, new ReviewDao(getMyBatis()), persister);
  }

  @Test
  public void shouldExecuteOnProject() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    assertThat(decorator.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldExecuteOnProject_not_if_past_inspection() {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(false);
    assertThat(decorator.shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void shouldCloseReviewsOnResolvedViolations() {
    setupData("shouldCloseReviewsOnResolvedViolations");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getViolations(any(ViolationQuery.class))).thenReturn(Collections.<Violation>emptyList());

    Resource resource = new JavaFile("org.foo.Bar");
    decorator.decorate(resource, context);

    verify(notifications, times(2)).notifyClosed(any(ReviewDto.class), any(Project.class), eq(resource));
    checkTables("shouldCloseReviewsOnResolvedViolations", new String[]{"updated_at"}, "reviews");
  }

  @Test
  public void shouldCloseResolvedManualViolations() {
    setupData("shouldCloseResolvedManualViolations");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getViolations(any(ViolationQuery.class))).thenReturn(Collections.<Violation>emptyList());

    Resource resource = new JavaFile("org.foo.Bar");
    decorator.decorate(resource, context);

    verify(notifications).notifyClosed(any(ReviewDto.class), any(Project.class), eq(resource));
    checkTables("shouldCloseResolvedManualViolations", new String[]{"updated_at"}, "reviews");
  }

  @Test
  public void shouldReopenViolations() {
    setupData("shouldReopenViolations");
    DecoratorContext context = mock(DecoratorContext.class);
    Violation violation = new Violation(new Rule());
    violation.setPermanentId(1000);
    when(context.getViolations(any(ViolationQuery.class))).thenReturn(Lists.newArrayList(violation));

    Resource resource = new JavaFile("org.foo.Bar");
    decorator.decorate(resource, context);

    verify(notifications).notifyReopened(any(ReviewDto.class), any(Project.class), eq(resource));
    checkTables("shouldReopenViolations", new String[]{"updated_at"}, "reviews");
  }

  @Test
  public void hasUpToDateInformation() {
    assertThat(ReviewWorkflowDecorator.hasUpToDateInformation(
      new ReviewDto().setTitle("Design").setLine(30),
      new Violation(new Rule()).setMessage("Design").setLineId(30)))
      .isTrue();


    // different title
    assertThat(ReviewWorkflowDecorator.hasUpToDateInformation(
      new ReviewDto().setTitle("Design").setLine(30),
      new Violation(new Rule()).setMessage("Other").setLineId(30))
    ).isFalse();

    // different line
    assertThat(ReviewWorkflowDecorator.hasUpToDateInformation(
      new ReviewDto().setTitle("Design").setLine(300),
      new Violation(new Rule()).setMessage("Design").setLineId(200)))
      .isFalse();

    assertThat(ReviewWorkflowDecorator.hasUpToDateInformation(
      new ReviewDto().setTitle("Design").setLine(300),
      new Violation(new Rule()).setMessage("Design").setLineId(null)))
      .isFalse();
  }
}
