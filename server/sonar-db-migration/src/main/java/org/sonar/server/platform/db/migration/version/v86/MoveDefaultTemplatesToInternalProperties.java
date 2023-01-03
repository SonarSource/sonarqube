/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

public class MoveDefaultTemplatesToInternalProperties extends DataChange {

  private final System2 system2;

  public MoveDefaultTemplatesToInternalProperties(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    OrganizationDefaultTemplates organizationDefaultTemplates = context
      .prepareSelect("select org.default_perm_template_project, org.default_perm_template_port, org.default_perm_template_app from organizations org " +
        "where org.kee = 'default-organization'")
      .get(OrganizationDefaultTemplates::new);
    if (organizationDefaultTemplates == null) {
      throw new IllegalStateException("Default organization is missing");
    }
    Upsert updateInternalProperties = context.prepareUpsert("insert into internal_properties (kee, text_value, is_empty, created_at) values (?, ?, ?, ?)");

    Optional<String> defaultPermissionTemplateForProject = organizationDefaultTemplates.getDefaultPermissionTemplateForProject();
    if (defaultPermissionTemplateForProject.isPresent()) {
      insertInInternalProperties(context, updateInternalProperties, "defaultTemplate.prj", defaultPermissionTemplateForProject.get());
    } else {
      // If default permission templates are missing, they will be added during the startup task "RegisterPermissionTemplates"
      return;
    }

    organizationDefaultTemplates.getDefaultPermissionTemplateForPortfolio()
      .ifPresent(
        defaultPermissionTemplate -> insertInInternalProperties(context, updateInternalProperties, "defaultTemplate.port", defaultPermissionTemplate));
    organizationDefaultTemplates.getDefaultPermissionTemplateForApplication()
      .ifPresent(
        defaultPermissionTemplate -> insertInInternalProperties(context, updateInternalProperties, "defaultTemplate.app", defaultPermissionTemplate));
  }

  private void insertInInternalProperties(Context context, Upsert updateInternalProperties, String key, String value) {
    try {
      Integer isInternalPropertyAlreadyExists = context.prepareSelect("select count(1) from internal_properties ip where ip.kee = ?")
        .setString(1, key)
        .get(row -> row.getInt(1));
      if (isInternalPropertyAlreadyExists != null && isInternalPropertyAlreadyExists > 0) {
        return;
      }
      updateInternalProperties
        .setString(1, key)
        .setString(2, value)
        .setBoolean(3, false)
        .setLong(4, system2.now())
        .execute()
        .commit();
    } catch (SQLException throwables) {
      throw new IllegalStateException(throwables);
    }
  }

  private static class OrganizationDefaultTemplates {
    private final String defaultPermissionTemplateForProject;
    private final String defaultPermissionTemplateForPortfolio;
    private final String defaultPermissionTemplateForApplication;

    OrganizationDefaultTemplates(Select.Row row) throws SQLException {
      this.defaultPermissionTemplateForProject = row.getNullableString(1);
      this.defaultPermissionTemplateForPortfolio = row.getNullableString(2);
      this.defaultPermissionTemplateForApplication = row.getNullableString(3);
    }

    Optional<String> getDefaultPermissionTemplateForProject() {
      return Optional.ofNullable(defaultPermissionTemplateForProject);
    }

    Optional<String> getDefaultPermissionTemplateForPortfolio() {
      return Optional.ofNullable(defaultPermissionTemplateForPortfolio);
    }

    Optional<String> getDefaultPermissionTemplateForApplication() {
      return Optional.ofNullable(defaultPermissionTemplateForApplication);
    }
  }
}
