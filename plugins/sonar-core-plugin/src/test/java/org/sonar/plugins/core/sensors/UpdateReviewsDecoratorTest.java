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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import com.google.common.collect.Lists;

public class UpdateReviewsDecoratorTest extends AbstractDbUnitTestCase {

  private UpdateReviewsDecorator reviewsDecorator;
  private ViolationTrackingDecorator violationTrackingDecorator;
  private Resource<?> resource;

  @Before
  public void setUp() {
    resource = mock(File.class);
    ResourcePersister resourcePersister = mock(ResourcePersister.class);
    Snapshot snapshot = new Snapshot();
    snapshot.setResourceId(1);
    when(resourcePersister.getSnapshot(resource)).thenReturn(snapshot);
    
    Project project = mock(Project.class);
    when(project.getRoot()).thenReturn(project);
    
    violationTrackingDecorator = mock(ViolationTrackingDecorator.class);
    
    reviewsDecorator = new UpdateReviewsDecorator(resourcePersister, getSession(), violationTrackingDecorator);
  }

  @Test
  public void testShouldExecuteOnProject() throws Exception {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    assertTrue(reviewsDecorator.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldCloseReviewWithoutCorrespondingViolation() throws Exception {
    Violation v1 = mock(Violation.class);
    when(v1.getMessage()).thenReturn("message 1");
    when(v1.getLineId()).thenReturn(1);
    Violation v2 = mock(Violation.class);
    when(v2.getMessage()).thenReturn("message 2");
    when(v2.getLineId()).thenReturn(2);
    Violation v3 = mock(Violation.class);
    when(v3.getMessage()).thenReturn("message 3");
    when(v3.getLineId()).thenReturn(3);
    Violation v4 = mock(Violation.class);
    when(v4.getMessage()).thenReturn("message 4");
    when(v4.getLineId()).thenReturn(4);    
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getViolations()).thenReturn(Lists.newArrayList(v1,v2,v3,v4));
    
    RuleFailureModel rf1 = mock(RuleFailureModel.class);
    when(rf1.getPermanentId()).thenReturn(1);
    when(violationTrackingDecorator.getReferenceViolation(v1)).thenReturn(rf1);
    RuleFailureModel rf2 = mock(RuleFailureModel.class);
    when(rf2.getPermanentId()).thenReturn(2);
    when(violationTrackingDecorator.getReferenceViolation(v2)).thenReturn(rf2);
    RuleFailureModel rf3 = mock(RuleFailureModel.class);
    when(rf3.getPermanentId()).thenReturn(3);
    when(violationTrackingDecorator.getReferenceViolation(v3)).thenReturn(rf3);
    RuleFailureModel rf4 = mock(RuleFailureModel.class);
    when(rf4.getPermanentId()).thenReturn(4);
    when(violationTrackingDecorator.getReferenceViolation(v4)).thenReturn(rf4);
    
    setupData("fixture");
    
    reviewsDecorator.decorate(resource, context);
    
    
    checkTables("shouldUpdateReviews", new String[] { "updated_at" }, new String[] { "reviews" });
  }
}
