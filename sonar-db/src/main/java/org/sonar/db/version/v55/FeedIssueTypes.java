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
package org.sonar.db.version.v55;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.sql.SQLException;
import java.util.List;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.MassUpdate.Handler;
import org.sonar.db.version.Select.Row;
import org.sonar.db.version.SqlStatement;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.sonar.db.version.v55.TagsToType.tagsToType;

/**
 * Duplicates RuleTagsToTypeConverter from API on purpose. Db migration must be isolated and must not
 * depend on external code that may evolve over time.
 *
 * https://jira.sonarsource.com/browse/MMF-141
 */
public class FeedIssueTypes extends BaseDataChange {

  private static final Splitter TAG_SPLITTER = Splitter.on(',');
  private static final Joiner TAG_JOINER = Joiner.on(',').skipNulls();

  private final System2 system;

  public FeedIssueTypes(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("issues");
    update.select("SELECT id, tags FROM issues WHERE issue_type IS NULL OR issue_type=0");
    update.update("UPDATE issues SET issue_type=?, tags=?, updated_at=? WHERE id=?");
    update.execute(new MigrationHandler(system.now()));
  }

  private static final class MigrationHandler implements Handler {
    private final long now;

    public MigrationHandler(long now) {
      this.now = now;
    }

    @Override
    public boolean handle(Row row, SqlStatement update) throws SQLException {
      long id = row.getLong(1);

      // See algorithm to deduce type from tags in RuleTagsToTypeConverter
      List<String> tags = newArrayList(TAG_SPLITTER.split(defaultString(row.getNullableString(2))));
      RuleType type = tagsToType(tags);
      tags.remove("bug");
      tags.remove("security");

      update.setInt(1, type.getDbConstant());
      update.setString(2, TAG_JOINER.join(tags));
      update.setLong(3, now);
      update.setLong(4, id);
      return true;
    }
  }

}
