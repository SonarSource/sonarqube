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
package org.sonar.server.platform.db.migration.version.v71;

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

/**
 * Populate PROJECT_LINK2 data from PROJECT_LINK table and take the opportunity to do some cleanup :
 * - Ignore link that are not set on project (only take component with qualifier TRK and scope PRJ)
 * - Do not set a name on provided links (SONAR-10411)
 * - Do not copy link on Developer Connection (SONAR-10299)
 */
public class PopulateTableProjectLinks2 extends DataChange {

  private static final String TYPE_HOME_PAGE = "homepage";
  private static final String TYPE_CI = "ci";
  private static final String TYPE_ISSUE_TRACKER = "issue";
  private static final String TYPE_SOURCES = "scm";
  private static final String TYPE_SOURCES_DEV = "scm_dev";
  private static final List<String> PROVIDED_TYPES = ImmutableList.of(TYPE_HOME_PAGE, TYPE_CI, TYPE_ISSUE_TRACKER, TYPE_SOURCES);

  private static final String SCOPE_PROJECT = "PRJ";
  private static final String QUALIFIER_PROJECT = "TRK";

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public PopulateTableProjectLinks2(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT" +
      " p.component_uuid, p.link_type, p.name, p.href" +
      " from project_links p" +
      // Join on projects in order to sanitize orphans (if any)
      " inner join projects prj on prj.uuid=p.component_uuid and prj.scope=? and prj.qualifier=? " +
      " left outer join project_links2 p2" +
      " on p2.project_uuid=p.component_uuid " +
      "    and p2.href=p.href" +
      "    and p2.link_type=p.link_type" +
      "    and (p2.name=p.name or (p2.name is null and p.link_type in (?, ?, ?, ?)))" +
      " where" +
      " p2.uuid is null" +
      " order by p.id")
      .setString(1, SCOPE_PROJECT)
      .setString(2, QUALIFIER_PROJECT)
      .setString(3, TYPE_HOME_PAGE)
      .setString(4, TYPE_CI)
      .setString(5, TYPE_ISSUE_TRACKER)
      .setString(6, TYPE_SOURCES);
    massUpdate.update("insert into project_links2" +
      " (uuid, project_uuid, link_type, name, href, created_at, updated_at)" +
      " values " +
      " (?, ?, ?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("project links");
    massUpdate.execute((row, update) -> {
      String componentUuid = row.getString(1);
      String linkType = row.getString(2);
      String name = row.getString(3);
      String href = row.getString(4);

      // project link "developer connection" are removed
      if (linkType.equals(TYPE_SOURCES_DEV)) {
        return false;
      }

      update.setString(1, uuidFactory.create());
      update.setString(2, componentUuid);
      update.setString(3, linkType);
      // provided type don't need anymore a name, the UI will display it by getting the i18 bundle of the link_type value
      if (PROVIDED_TYPES.contains(linkType)) {
        update.setString(4, null);
      } else {
        update.setString(4, name);
      }
      update.setString(5, href);
      update.setLong(6, now);
      update.setLong(7, now);
      return true;
    });
  }
}
