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

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;

/**
 * Purge Review that are attached to projects that have been deleted.
 * 
 * @since 2.8
 */
public final class PurgeOrphanReviews extends Purge {

  private static final Logger LOG = LoggerFactory.getLogger(PurgeOrphanReviews.class);

  public PurgeOrphanReviews(DatabaseSession session) {
    super(session);
  }

  public void purge(PurgeContext context) {
    DatabaseSession session = getSession();
    
    // delete reviews
    Query query = session.createNativeQuery(getDeleteReviewsSqlRequest());
    int rowDeleted = query.executeUpdate();
    LOG.debug("- {} reviews deleted.", rowDeleted);
    
    // and delete review comments
    query = session.createNativeQuery(getDeleteReviewCommentsSqlRequest());
    rowDeleted = query.executeUpdate();
    LOG.debug("- {} review comments deleted.", rowDeleted);

    session.commit();
  }

  protected String getDeleteReviewsSqlRequest() {
    return "DELETE FROM reviews WHERE project_id not in (SELECT id FROM projects WHERE scope = 'PRJ' and qualifier = 'TRK')";
  }

  protected String getDeleteReviewCommentsSqlRequest() {
    return "DELETE FROM review_comments WHERE review_id not in (SELECT id FROM reviews)";
  }
}
