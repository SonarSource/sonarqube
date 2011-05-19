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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Statement;

import junit.framework.ComparisonFailure;

import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.test.persistence.DatabaseTestCase;

public class CloseReviewsDecoratorTest extends DatabaseTestCase {

  @Test
  public void testShouldExecuteOnProject() throws Exception {
    Project project = mock(Project.class);
    when(project.isLatestAnalysis()).thenReturn(true);
    CloseReviewsDecorator reviewsDecorator = new CloseReviewsDecorator(null, null);
    assertTrue(reviewsDecorator.shouldExecuteOnProject(project));
  }

  @Test
  public void shouldCloseReviewWithoutCorrespondingViolation() throws Exception {
    setupData("fixture");

    CloseReviewsDecorator reviewsDecorator = new CloseReviewsDecorator(null, null);
    String sqlRequest = reviewsDecorator.generateUpdateOnResourceSqlRequest(666, 222);

    Statement stmt = getConnection().createStatement();
    int count = stmt.executeUpdate(sqlRequest);

    assertThat(count, is(1));
    assertTables("shouldCloseReviewWithoutCorrespondingViolation", new String[] { "reviews" }, new String[] { "updated_at" });

    try {
      assertTables("shouldCloseReviewWithoutCorrespondingViolation", new String[] { "reviews" });
      fail("'updated_at' columns are identical whereas they should be different.");
    } catch (ComparisonFailure e) {
      // "updated_at" column must be different, so the comparison should raise this exception
    }
  }
}
