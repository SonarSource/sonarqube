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

package org.sonar.server.db.migrations.v43;

import java.sql.SQLException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.SqlStatement;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

/**
 * Used in the Active Record Migration 514
 *
 * @since 4.3
 */
public class IssueChangelogMigrationStep extends BaseDataChange {

  private final WorkDurationConvertor workDurationConvertor;
  private final System2 system2;
  private final Pattern pattern = Pattern.compile("technicalDebt=(\\d*)\\|(\\d*)", Pattern.CASE_INSENSITIVE);

  public IssueChangelogMigrationStep(Database database, System2 system2, PropertiesDao propertiesDao) {
    this(database, system2, new WorkDurationConvertor(propertiesDao));
  }

  @VisibleForTesting
  IssueChangelogMigrationStep(Database database, System2 system2, WorkDurationConvertor convertor) {
    super(database);
    this.workDurationConvertor = convertor;
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    workDurationConvertor.init();
    final Date now = new Date(system2.now());
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT ic.id, ic.change_data  FROM issue_changes ic " +
      "WHERE ic.change_type = 'diff' AND ic.change_data LIKE '%technicalDebt%'");
    massUpdate.update("UPDATE issue_changes SET change_data=?,updated_at=? WHERE id=?");
    massUpdate.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long id = row.getNullableLong(1);
        String changeData = row.getNullableString(2);

        update.setString(1, convertChangelog(changeData));
        update.setDate(2, now);
        update.setLong(3, id);
        return true;
      }
    });
  }

  @VisibleForTesting
  @CheckForNull
  String convertChangelog(@Nullable String data) {
    if (data == null) {
      return null;
    }
    Matcher matcher = pattern.matcher(data);
    StringBuffer sb = new StringBuffer();
    if (matcher.find()) {
      String replacement = "technicalDebt=";
      String oldValue = matcher.group(1);
      if (!Strings.isNullOrEmpty(oldValue)) {
        long oldDebt = workDurationConvertor.createFromLong(Long.parseLong(oldValue));
        replacement += Long.toString(oldDebt);
      }
      replacement += "|";
      String newValue = matcher.group(2);
      if (!Strings.isNullOrEmpty(newValue)) {
        long newDebt = workDurationConvertor.createFromLong(Long.parseLong(newValue));
        replacement += Long.toString(newDebt);
      }
      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

}
