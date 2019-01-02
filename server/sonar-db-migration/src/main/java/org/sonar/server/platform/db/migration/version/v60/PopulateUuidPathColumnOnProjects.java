/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v60;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.stream.Collectors.toCollection;

public class PopulateUuidPathColumnOnProjects extends DataChange {

  private static final Logger LOG = Loggers.get(PopulateUuidPathColumnOnProjects.class);
  private static final Joiner PATH_JOINER = Joiner.on('.');
  private static final Splitter PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();
  private static final String PATH_SEPARATOR = ".";
  private static final String ROOT_PATH = PATH_SEPARATOR;

  public PopulateUuidPathColumnOnProjects(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    // group upgrades by tree of component
    List<String> rootComponentUuids = context
      .prepareSelect("select distinct project_uuid from projects where uuid_path is null")
      .list(row -> row.getString(1));
    for (String rootUuid : rootComponentUuids) {
      handleRoot(rootUuid, context);
    }

    handleOrphans(context);
  }

  private static void handleRoot(String rootComponentUuid, Context context) throws SQLException {
    Relations relations = new Relations();
    context
      .prepareSelect("select s.id, s.path, s.component_uuid from snapshots s where s.root_component_uuid=? and s.islast=?")
      .setString(1, rootComponentUuid)
      .setBoolean(2, true)
      .scroll(row -> {
        long snapshotId = row.getLong(1);
        String snapshotPath = row.getString(2);
        String componentUuid = row.getString(3);
        relations.add(new Snapshot(snapshotId, snapshotPath, componentUuid));
      });

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select p.uuid, p.project_uuid from projects p where p.project_uuid=? and p.uuid_path is null").setString(1, rootComponentUuid);
    massUpdate.update("update projects set uuid_path=? where uuid=? and uuid_path is null");
    massUpdate.rowPluralName("components in tree of " + rootComponentUuid);
    massUpdate.execute((row, update) -> handleComponent(relations, row, update));
  }

  private static void handleOrphans(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid, project_uuid from projects where uuid_path is null");
    massUpdate.update("update projects set uuid_path=? where uuid=? and uuid_path is null");
    massUpdate.rowPluralName("orphan components");
    massUpdate.execute((row, update, updateIndex) -> {
      String uuid = row.getString(1);
      String rootUuid = row.getString(2);
      String path = uuid.equals(rootUuid) ? ROOT_PATH : (PATH_SEPARATOR + rootUuid + PATH_SEPARATOR);
      update.setString(1, path);
      update.setString(2, uuid);
      return true;
    });
  }

  private static boolean handleComponent(Relations relations, Select.Row row, SqlStatement update) throws SQLException {
    String componentUuid = row.getString(1);
    String rootComponentUuid = row.getString(2);

    if (componentUuid.equals(rootComponentUuid)) {
      // Root component, no need to use the table SNAPSHOTS.
      // Moreover it allows to support provisioned projects (zero analysis)
      update.setString(1, PATH_SEPARATOR);
      update.setString(2, componentUuid);
      return true;
    }

    Snapshot snapshot = relations.snapshotsByComponentUuid.get(componentUuid);
    if (snapshot == null) {
      LOG.trace("No UUID found for component UUID={}", componentUuid);
      return false;
    }

    List<String> componentUuidPath = Arrays.stream(snapshot.snapshotPath)
      .mapToObj(relations.snapshotsById::get)
      .filter(Objects::nonNull)
      .map(s -> s.componentUuid)
      .collect(toCollection(ArrayList::new));
    if (componentUuidPath.size() != snapshot.snapshotPath.length) {
      LOG.trace("Some component UUIDs not found for snapshots [{}]", snapshot.snapshotPath);
      return false;
    }

    update.setString(1, PATH_SEPARATOR + PATH_JOINER.join(componentUuidPath) + PATH_SEPARATOR);
    update.setString(2, componentUuid);
    return true;
  }

  private static final class Relations {
    private final Map<String, Snapshot> snapshotsByComponentUuid = new HashMap<>();
    private final Map<Long, Snapshot> snapshotsById = new HashMap<>();

    void add(Snapshot snapshot) {
      snapshotsByComponentUuid.put(snapshot.componentUuid, snapshot);
      snapshotsById.put(snapshot.id, snapshot);
    }
  }

  private static final class Snapshot {
    private static final long[] EMPTY_PATH = new long[0];
    private final long id;
    private final long[] snapshotPath;
    private final String componentUuid;

    public Snapshot(long id, String snapshotPath, String componentUuid) {
      this.id = id;
      this.snapshotPath = parsePath(snapshotPath);
      this.componentUuid = componentUuid;
    }

    // inputs: null (on Oracle), "", "1." or "1.2.3."
    private static long[] parsePath(@Nullable String snapshotPath) {
      if (snapshotPath == null) {
        return EMPTY_PATH;
      }
      return PATH_SPLITTER
        .splitToList(snapshotPath)
        .stream()
        .mapToLong(Long::parseLong)
        .toArray();
    }
  }
}
