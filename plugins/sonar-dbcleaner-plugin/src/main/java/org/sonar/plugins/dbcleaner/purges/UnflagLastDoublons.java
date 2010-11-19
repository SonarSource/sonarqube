/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;
import org.sonar.plugins.dbcleaner.api.PurgeUtils;

import java.util.List;

import javax.persistence.Query;

/**
 * @since 1.11
 */
public final class UnflagLastDoublons extends Purge {

  public UnflagLastDoublons(DatabaseSession session) {
    super(session);
  }

  public void purge(PurgeContext context) {
    Query query = getSession().createQuery(
        "SELECT olds.id FROM " + Snapshot.class.getSimpleName() + " olds " +
            " where olds.last=true AND EXISTS (from " + Snapshot.class.getSimpleName() + " news WHERE news.last=true AND news.resourceId=olds.resourceId AND news.createdAt>olds.createdAt)");
    List<Integer> snapshotIds = query.getResultList();

    PurgeUtils.executeQuery(getSession(), "", snapshotIds, "UPDATE " + Snapshot.class.getSimpleName() + " SET last=false WHERE id in (:ids)");
  }
}
