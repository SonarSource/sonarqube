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

import org.sonar.api.batch.Event;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;
import org.sonar.plugins.dbcleaner.api.PurgeUtils;

import java.util.List;

import javax.persistence.Query;

public final class PurgeEventOrphans extends Purge {

  public PurgeEventOrphans(DatabaseSession session) {
    super(session);
  }

  public void purge(PurgeContext context) {
    String selectEventsSql = "SELECT e.id FROM " + Event.class.getSimpleName() + " e WHERE (";
    selectEventsSql += "e.resourceId IS NOT NULL AND NOT EXISTS(FROM " + ResourceModel.class.getSimpleName()
        + " r WHERE r.id=e.resourceId)";
    selectEventsSql += ") OR (";
    selectEventsSql += "e.snapshot IS NULL";
    selectEventsSql += ")";

    Query query = getSession().createQuery(selectEventsSql);
    final List<Integer> eventIds = query.getResultList();
    PurgeUtils.executeQuery(getSession(), "", eventIds, "DELETE FROM " + Event.class.getSimpleName() + " WHERE id in (:ids)");
  }
}
