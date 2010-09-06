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
package org.sonar.plugins.core.purges;

import org.sonar.core.purge.AbstractPurge;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.PurgeContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;

import javax.persistence.Query;
import java.util.List;

public class PurgeEventOrphans extends AbstractPurge {

  public PurgeEventOrphans(DatabaseSession session) {
    super(session);
  }

  public void purge(PurgeContext context) {
    Query query = getSession().createQuery("SELECT e.id FROM " + Event.class.getSimpleName() +
        " e WHERE e.resourceId IS NOT NULL AND NOT EXISTS(FROM " + ResourceModel.class.getSimpleName() + " r WHERE r.id=e.resourceId)");
    final List<Integer> eventIds = query.getResultList();
    executeQuery(eventIds, "DELETE FROM " + Event.class.getSimpleName() + " WHERE id in (:ids)");
  }
}
