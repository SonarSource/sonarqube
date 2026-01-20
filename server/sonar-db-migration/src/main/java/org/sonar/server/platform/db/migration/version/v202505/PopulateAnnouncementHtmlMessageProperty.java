/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202505;

import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.markdown.Markdown;
import org.sonar.server.platform.db.migration.step.DataChange;

public class PopulateAnnouncementHtmlMessageProperty extends DataChange {

  private static final String SOURCE_PROPERTY = "sonar.announcement.message";
  private static final String TARGET_PROPERTY = "sonar.announcement.htmlMessage";

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public PopulateAnnouncementHtmlMessageProperty(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    final String targetValue = context.prepareSelect("select text_value from properties where prop_key = ?")
      .setString(1, TARGET_PROPERTY)
      .get(r -> r.getString(1));

    if (StringUtils.isEmpty(targetValue)) {
      final String sourceValue = context.prepareSelect("select text_value from properties where prop_key = ?")
        .setString(1, SOURCE_PROPERTY)
        .get(r -> r.getString(1));

      if (!StringUtils.isEmpty(sourceValue)) {
        final String htmlValue = Markdown.convertToHtml(sourceValue);
        // Check if the row exists (Oracle treats empty strings as NULL, so we can't rely on targetValue)
        final boolean targetPropertyExists = propertyExists(context);

        if (targetPropertyExists) {
          updatedValue(context, htmlValue);
        } else {
          insertValue(context, htmlValue);
        }
      }
    }
  }

  private static void updatedValue(final Context context, final String htmlValue) throws SQLException {
    context.prepareUpsert("update properties set text_value = ?, is_empty = ? where prop_key = ?")
      .setString(1, htmlValue)
      .setBoolean(2, false)
      .setString(3, TARGET_PROPERTY)
      .execute()
      .commit();
  }

  private void insertValue(final Context context, final String htmlValue) throws SQLException {
    context.prepareUpsert("""
        insert into properties (prop_key, is_empty, text_value, created_at, uuid)
        values (?, ?, ?, ?, ?)
      """)
      .setString(1, TARGET_PROPERTY)
      .setBoolean(2, false)
      .setString(3, htmlValue)
      .setLong(4, system2.now())
      .setString(5, uuidFactory.create())
      .execute()
      .commit();
  }

  private static boolean propertyExists(Context context) throws SQLException {
    Long count = context.prepareSelect("select count(*) from properties where prop_key = ?")
      .setString(1, TARGET_PROPERTY)
      .get(r -> r.getLong(1));
    return count != null && count > 0;
  }

}
