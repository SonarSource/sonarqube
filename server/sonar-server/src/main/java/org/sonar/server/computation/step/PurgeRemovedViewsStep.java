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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.component.UuidWithProjectUuidDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.view.index.ViewIndex;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * This step will remove every Views and Sub Views from the index that do not exists in the db.
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
  public void execute(ComputationContext context) {
    if (context.getProject().qualifier().equals(Qualifiers.VIEW)) {
      purgeRemovedViews();
    }
  }

  private void purgeRemovedViews() {
    DbSession session = dbClient.openSession(false);
    try {
      List<UuidWithProjectUuidDto> uuidWithProjectUuidDtos = dbClient.componentDao().selectAllViewsAndSubViews(session);
      Set<String> viewUuidsInDb = newHashSet(Iterables.transform(uuidWithProjectUuidDtos, new Function<UuidWithProjectUuidDto, String>() {
        @Override
        public String apply(@Nullable UuidWithProjectUuidDto input) {
          return input != null ? input.getUuid() : null;
        }
      }));
      Set<String> viewUuidsInIndex = newHashSet(index.findAllViewUuids());
      Set<String> viewsToRemove = Sets.difference(viewUuidsInIndex, viewUuidsInDb);
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
