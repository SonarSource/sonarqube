/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.computation.step;

import com.google.common.collect.Sets;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.view.index.ViewIndex;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * This step removes from index the views and sub-views that do not exist in db.
 * As it's executed on each analysis, it means that when the Views task is executed on every views, this step will be executed on each views !
 *
 * A more optimized approach would be to execute this step only once at this end of the Views task.
 */
public class PurgeRemovedViewsStep implements ComputationStep {

  private final DbClient dbClient;
  private final ViewIndex index;

  public PurgeRemovedViewsStep(ViewIndex index, DbClient dbClient) {
    this.index = index;
    this.dbClient = dbClient;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.VIEW};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(false);
    try {
      Set<String> viewUuidsInIndex = newHashSet(index.findAllViewUuids());
      Set<String> viewUuidInDb = newHashSet(dbClient.componentDao().selectExistingUuids(session, viewUuidsInIndex));
      Set<String> viewsToRemove = Sets.difference(viewUuidsInIndex, viewUuidInDb);
      index.delete(viewsToRemove);
    } finally {
      session.close();
    }
  }

  @Override
  public String getDescription() {
    return "Purge removed views";
  }
}
