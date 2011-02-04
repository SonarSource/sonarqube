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

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;
import org.sonar.plugins.dbcleaner.api.PurgeUtils;

import java.util.List;

import javax.persistence.Query;

/**
 * @since 2.1
 */
public final class PurgeOrphanResources extends Purge {

  public PurgeOrphanResources(DatabaseSession session) {
    super(session);
  }

  public void purge(PurgeContext context) {
    Query query = getSession().createQuery("SELECT r1.id FROM " + ResourceModel.class.getSimpleName() +
        " r1 WHERE r1.rootId IS NOT NULL AND NOT EXISTS(FROM " + ResourceModel.class.getSimpleName() + " r2 WHERE r1.rootId=r2.id)");
    List<Integer> idsToDelete = query.getResultList();
    if (idsToDelete.size() > 0) {
      PurgeUtils.executeQuery(getSession(), "", idsToDelete, "DELETE FROM " + ResourceModel.class.getSimpleName() + " WHERE id in (:ids)");
    }
  }
}
