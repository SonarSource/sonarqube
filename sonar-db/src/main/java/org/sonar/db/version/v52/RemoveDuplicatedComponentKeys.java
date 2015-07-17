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
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.Select;
import org.sonar.db.version.Upsert;

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

  public RemoveDuplicatedComponentKeys(Database db) {
    super(db);
  }

  @Override
  public void execute(final Context context) throws SQLException {
    Upsert componentUpdate = context.prepareUpsert("DELETE FROM projects WHERE id=?");
    Upsert issuesUpdate = context.prepareUpsert("UPDATE issues SET component_uuid=?, project_uuid=? WHERE component_uuid=?");

    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();
    try {
      RemoveDuplicatedComponentHandler handler = new RemoveDuplicatedComponentHandler(context, componentUpdate, issuesUpdate);
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
      componentUpdate.close();
      issuesUpdate.close();
    }
  }

  private class RemoveDuplicatedComponentHandler implements Select.RowHandler {
    private final Context context;
    private final Upsert componentUpdate;
    private final Upsert issuesUpdate;

    private boolean isEmpty = true;

    public RemoveDuplicatedComponentHandler(Context context, Upsert componentUpdate, Upsert issuesUpdate) {
      this.context = context;
      this.componentUpdate = componentUpdate;
      this.issuesUpdate = issuesUpdate;
    }

    @Override
    public void handle(Select.Row row) throws SQLException {
      List<Component> components = context
        .prepareSelect("SELECT p.id, p.uuid, p.project_uuid, p.enabled FROM projects p WHERE p.kee=? ORDER BY id")
        .setString(1, row.getString(1))
        .list(ComponentRowReader.INSTANCE);
      // We keep the enabled component or the last component of the list
      Component refComponent = FluentIterable.from(components).firstMatch(EnabledComponent.INSTANCE).or(components.get(components.size() - 1));
      for (Component componentToRemove : FluentIterable.from(components).filter(Predicates.not(new MatchComponentId(refComponent.id)))) {
        componentUpdate
          .setLong(1, componentToRemove.id)
          .addBatch();
        issuesUpdate
          .setString(1, refComponent.uuid)
          .setString(2, refComponent.projectUuid)
          .setString(3, componentToRemove.uuid)
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
      return input.enabled;
    }
  }

  private static class MatchComponentId implements Predicate<Component> {

    private final long id;

    public MatchComponentId(long id) {
      this.id = id;
    }

    @Override
    public boolean apply(@Nonnull Component input) {
      return input.id == this.id;
    }
  }

  private enum ComponentRowReader implements Select.RowReader<Component> {
    INSTANCE;

    @Override
    public Component read(Select.Row row) throws SQLException {
      return new Component(row.getLong(1), row.getString(2), row.getString(3), row.getBoolean(4));
    }
  }

  private static class Component {
    private final long id;
    private final String uuid;
    private final String projectUuid;
    private final boolean enabled;

    public Component(long id, String uuid, String projectUuid, boolean enabled) {
      this.id = id;
      this.uuid = uuid;
      this.projectUuid = projectUuid;
      this.enabled = enabled;
    }
  }
}
