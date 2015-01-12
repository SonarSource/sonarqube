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
package org.sonar.server.db.migrations.v51;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

public class RenameComponentRelatedParamsInIssueFilters extends BaseDataChange {

  private static final char FIELD_SEPARATOR = '|';
  private static final String LIKE_PREFIX = "data like '%";
  private static final String LIKE_SUFFIX = "%' or ";
  private static final String COMPONENTS = "components=";
  private static final String COMPONENT_UUIDS = "componentUuids=";
  private static final String PROJECTS = "projects=";
  private static final String COMPONENT_ROOTS = "componentRoots=";
  private static final String COMPONENT_ROOT_UUIDS = "componentRootUuids=";

  private final System2 system;

  public RenameComponentRelatedParamsInIssueFilters(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final Date now = new Date(system.now());
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select id,data from issue_filters where " +
      LIKE_PREFIX + COMPONENTS + LIKE_SUFFIX +
      LIKE_PREFIX + PROJECTS + LIKE_SUFFIX +
      LIKE_PREFIX + COMPONENT_ROOTS + LIKE_SUFFIX +
      LIKE_PREFIX + COMPONENT_ROOT_UUIDS + "%'");
    massUpdate.update("update issue_filters set data=?, updated_at=? where id=?");
    massUpdate.rowPluralName("issue filters");
    massUpdate.execute(new RenameComponentRelatedParamsHandler(now));
  }

  private static final class RenameComponentRelatedParamsHandler implements MassUpdate.Handler {
    private final Date now;

    private RenameComponentRelatedParamsHandler(Date now) {
      this.now = now;
    }

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      String data = row.getString(2);
      String[] fields = StringUtils.split(data, FIELD_SEPARATOR);

      List<String> fieldsToKeep = Lists.newArrayList();
      SortedSet<String> components = Sets.newTreeSet();
      SortedSet<String> componentUuids = Sets.newTreeSet();
      Splitter componentSplitter = Splitter.on(",");
      Joiner componentJoiner = Joiner.on(",");
      for (String field : fields) {
        if (field.startsWith(COMPONENTS) || field.startsWith(COMPONENT_ROOTS)) {
          components.addAll(Lists.newArrayList(componentSplitter.split(field.substring(field.indexOf("=") + 1))));
        } else if (field.startsWith(COMPONENT_UUIDS) || field.startsWith(COMPONENT_ROOT_UUIDS)) {
          componentUuids.addAll(Lists.newArrayList(componentSplitter.split(field.substring(field.indexOf("=") + 1))));
        } else if (field.startsWith(PROJECTS)) {
          fieldsToKeep.add(field.replace(PROJECTS, "projectKeys="));
        } else {
          fieldsToKeep.add(field);
        }
      }
      if (!components.isEmpty()) {
        fieldsToKeep.add("componentKeys=" + componentJoiner.join(components));
      }
      if (!componentUuids.isEmpty()) {
        fieldsToKeep.add("componentUuids=" + componentJoiner.join(componentUuids));
      }

      update.setString(1, StringUtils.join(fieldsToKeep, FIELD_SEPARATOR));
      update.setDate(2, now);
      update.setLong(3, row.getLong(1));
      return true;
    }
  }
}
