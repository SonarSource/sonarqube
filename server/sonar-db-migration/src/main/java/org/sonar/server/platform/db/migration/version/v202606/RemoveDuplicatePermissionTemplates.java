/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * Removes case-insensitive duplicate templates, retaining the oldest one. Permission template assignments and template
 * characteristics owned by removed templates are also removed, as they are when a template is deleted normally.
 */
public class RemoveDuplicatePermissionTemplates extends DataChange {

  private static final String SELECT_TEMPLATES_SQL = "SELECT uuid, UPPER(name) FROM permission_templates " +
    "ORDER BY UPPER(name), CASE WHEN created_at IS NULL THEN 1 ELSE 0 END, created_at, uuid";
  private static final String DELETE_USER_PERMISSIONS_SQL = "DELETE FROM perm_templates_users WHERE template_uuid = ?";
  private static final String DELETE_GROUP_PERMISSIONS_SQL = "DELETE FROM perm_templates_groups WHERE template_uuid = ?";
  private static final String DELETE_CHARACTERISTICS_SQL = "DELETE FROM perm_tpl_characteristics WHERE template_uuid = ?";
  private static final String REPLACE_DEFAULT_TEMPLATE_SQL = "UPDATE internal_properties SET text_value = ? " +
    "WHERE kee IN ('defaultTemplate.prj', 'defaultTemplate.app', 'defaultTemplate.port') AND text_value = ?";
  private static final String DELETE_TEMPLATE_SQL = "DELETE FROM permission_templates WHERE uuid = ?";

  public RemoveDuplicatePermissionTemplates(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<Template> templates = context.prepareSelect(SELECT_TEMPLATES_SQL)
      .list(row -> new Template(row.getString(1), row.getString(2)));

    Template previousTemplate = null;
    for (Template template : templates) {
      if (previousTemplate != null && template.upperCaseName().equals(previousTemplate.upperCaseName())) {
        replaceDefaultTemplate(context, previousTemplate.uuid(), template.uuid());
        deleteTemplateAndDependencies(context, template.uuid());
      } else {
        previousTemplate = template;
      }
    }
  }

  private static void replaceDefaultTemplate(Context context, String retainedTemplateUuid, String removedTemplateUuid) throws SQLException {
    context.prepareUpsert(REPLACE_DEFAULT_TEMPLATE_SQL)
      .setString(1, retainedTemplateUuid)
      .setString(2, removedTemplateUuid)
      .execute()
      .commit();
  }

  private static void deleteTemplateAndDependencies(Context context, String templateUuid) throws SQLException {
    delete(context, DELETE_USER_PERMISSIONS_SQL, templateUuid);
    delete(context, DELETE_GROUP_PERMISSIONS_SQL, templateUuid);
    delete(context, DELETE_CHARACTERISTICS_SQL, templateUuid);
    delete(context, DELETE_TEMPLATE_SQL, templateUuid);
  }

  private static void delete(Context context, String sql, String templateUuid) throws SQLException {
    context.prepareUpsert(sql)
      .setString(1, templateUuid)
      .execute()
      .commit();
  }

  private record Template(String uuid, String upperCaseName) {
  }
}
