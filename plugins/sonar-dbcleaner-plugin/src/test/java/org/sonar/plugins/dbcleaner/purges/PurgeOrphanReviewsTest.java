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
package org.sonar.plugins.dbcleaner.purges;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.sql.Statement;

import org.junit.Test;
import org.sonar.test.persistence.DatabaseTestCase;

public class PurgeOrphanReviewsTest extends DatabaseTestCase {

  @Test
  public void shouldCloseReviewWithoutCorrespondingViolation() throws Exception {
    setupData("purgeOrphanReviews");

    Statement stmt = getConnection().createStatement();
    int count = stmt.executeUpdate(new PurgeOrphanReviews(null).getDeleteReviewsSqlRequest());
    assertThat(count, is(1));

    count = stmt.executeUpdate(new PurgeOrphanReviews(null).getDeleteReviewCommentsSqlRequest());
    assertThat(count, is(1));

    assertTables("purgeOrphanReviews", "reviews");
  }

}
