/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.version.v52;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.sonar.core.util.ProgressLogger;
import org.sonar.db.Database;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.Select;
import org.sonar.db.version.Upsert;
import org.sonar.db.version.v53.Component;
import org.sonar.db.version.v53.Migration53Mapper;

/**
 * Remove all duplicated component that have the same keys.
 * For each duplicated component key :
 * <ul>
 * <li>Only the enabled one or last one (with the latest id) is kept</li>
 * <li>When deleting a component, all its issues are linked to the remaining component</li>
 * </ul>
 */
public class RemoveDuplicatedComponentKeys extends BaseDataChange {

  private final AtomicLong counter = new AtomicLong(0L);
  private final DbClient dbClient;

  public RemoveDuplicatedComponentKeys(Database db, DbClient dbClient) {
    super(db);
    this.dbClient = dbClient;
  }

  @Override
  public void execute(final Context context) throws SQLException {
    Upsert componentUpdate = context.prepareUpsert("DELETE FROM projects WHERE id=?");
    Upsert issuesUpdate = context.prepareUpsert("UPDATE issues SET component_uuid=?, project_uuid=? WHERE component_uuid=?");

    DbSession readSession = dbClient.openSession(false);
    Migration53Mapper mapper = readSession.getMapper(Migration53Mapper.class);

    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();
    try {
      RemoveDuplicatedComponentHandler handler = new RemoveDuplicatedComponentHandler(mapper, componentUpdate, issuesUpdate);
      context.prepareSelect(
        "SELECT p.kee, COUNT(p.kee) FROM projects p " +
          "GROUP BY p.kee " +
          "HAVING COUNT(p.kee) > 1")
        .scroll(handler);
      if (!handler.isEmpty) {
        componentUpdate.execute().commit();
        issuesUpdate.execute().commit();
      }
      progress.log();
    } finally {
      progress.stop();
      dbClient.closeSession(readSession);
      componentUpdate.close();
      issuesUpdate.close();
    }
  }

  private class RemoveDuplicatedComponentHandler implements Select.RowHandler {
    private final Migration53Mapper mapper;
    private final Upsert componentUpdate;
    private final Upsert issuesUpdate;

    private boolean isEmpty = true;

    public RemoveDuplicatedComponentHandler(Migration53Mapper mapper, Upsert componentUpdate, Upsert issuesUpdate) {
      this.mapper = mapper;
      this.componentUpdate = componentUpdate;
      this.issuesUpdate = issuesUpdate;
    }

    @Override
    public void handle(Select.Row row) throws SQLException {
      String componentKey = row.getString(1);
      List<Component> components = mapper.selectComponentsByKey(componentKey);
      // We keep the enabled component or the last component of the list
      Component refComponent = FluentIterable.from(components).firstMatch(EnabledComponent.INSTANCE).or(components.get(components.size() - 1));
      for (Component componentToRemove : FluentIterable.from(components).filter(Predicates.not(new MatchComponentId(refComponent.getId())))) {
        componentUpdate
          .setLong(1, componentToRemove.getId())
          .addBatch();
        issuesUpdate
          .setString(1, refComponent.getUuid())
          .setString(2, refComponent.getProjectUuid())
          .setString(3, componentToRemove.getUuid())
          .addBatch();
        counter.getAndIncrement();
        isEmpty = false;
      }
    }

    public boolean isEmpty() {
      return isEmpty;
    }
  }

  private enum EnabledComponent implements Predicate<Component> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Component input) {
      return input.isEnabled();
    }
  }

  private static class MatchComponentId implements Predicate<Component> {

    private final long id;

    public MatchComponentId(long id) {
      this.id = id;
    }

    @Override
    public boolean apply(@Nonnull Component input) {
      return input.getId() == this.id;
    }
  }

}
