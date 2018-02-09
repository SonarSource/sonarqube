/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.step.DataChange;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Populate the columns DEFAULT_PERM_TEMPLATE, DEFAULT_PERM_TEMPLATE_PROJECT and DEFAULT_PERM_TEMPLATE_VIEW of table
 * ORGANIZATIONS, for the default organization exclusively, from the properties holding the default permissions template
 * uuids. These properties are then deleted.
 *
 * <p>
 * This migration ensures it can run but failing if:
 * <ul>
 *   <li>there is more than one organizations (because we can't populate the column for those extra organizations)</li>
 *   <li>the global default permission template can't be found (because an organization must have at least this default template)</li>
 * </ul>
 * </p>
 */
public class PopulateDefaultPermTemplateColumnsOfOrganizations extends DataChange {
  private static final String DEFAULT_TEMPLATE_PROPERTY = "sonar.permission.template.default";
  private static final String DEFAULT_PROJECT_TEMPLATE_PROPERTY = "sonar.permission.template.TRK.default";
  private static final String DEFAULT_VIEW_TEMPLATE_PROPERTY = "sonar.permission.template.VW.default";
  private static final String DEFAULT_DEV_TEMPLATE_PROPERTY = "sonar.permission.template.DEV.default";

  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;
  private final UuidFactory uuidFactory;

  public PopulateDefaultPermTemplateColumnsOfOrganizations(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuid, UuidFactory uuidFactory) {
    super(db);
    this.defaultOrganizationUuid = defaultOrganizationUuid;
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String defaultOrganizationUuid = this.defaultOrganizationUuid.getAndCheck(context);
    ensureOnlyDefaultOrganizationExists(context, defaultOrganizationUuid);

    Map<String, String> defaultTemplateProperties = retrieveDefaultTemplateProperties(context);

    String globalDefaultTemplate = defaultTemplateProperties.get(DEFAULT_TEMPLATE_PROPERTY);
    if (globalDefaultTemplate == null || globalDefaultTemplate.isEmpty()) {
      // DB has just been created, default template of default organization will be populated by a startup task
      return;
    }
    String projectDefaultTemplate = updateKeeIfRequired(context,
      firstNonNull(defaultTemplateProperties.get(DEFAULT_PROJECT_TEMPLATE_PROPERTY), globalDefaultTemplate));
    String viewDefaultTemplate = updateKeeIfRequired(context,
      defaultTemplateProperties.get(DEFAULT_VIEW_TEMPLATE_PROPERTY));

    Loggers.get(PopulateDefaultPermTemplateColumnsOfOrganizations.class)
      .debug("Setting default templates on default organization '{}': project='{}', view='{}'",
        defaultOrganizationUuid, projectDefaultTemplate, viewDefaultTemplate);

    context.prepareUpsert("update organizations set" +
      " default_perm_template_project=?," +
      " default_perm_template_view=?" +
      " where" +
      " uuid=?")
      .setString(1, projectDefaultTemplate)
      .setString(2, viewDefaultTemplate)
      .setString(3, defaultOrganizationUuid)
      .execute()
      .commit();

    // delete properties
    context.prepareUpsert("delete from properties where prop_key in (?,?,?,?)")
      .setString(1, DEFAULT_TEMPLATE_PROPERTY)
      .setString(2, DEFAULT_PROJECT_TEMPLATE_PROPERTY)
      .setString(3, DEFAULT_VIEW_TEMPLATE_PROPERTY)
      .setString(4, DEFAULT_DEV_TEMPLATE_PROPERTY)
      .execute()
      .commit();
  }

  /**
   * SONAR-10407 Ensure that the kee length is not greater than 40
   * In this case, we must update the kee to a UUID
   */
  private String updateKeeIfRequired(Context context, @Nullable String kee) throws SQLException {
    if (kee == null || kee.length() <= VarcharColumnDef.UUID_SIZE) {
      return kee;
    }

    String newKee = uuidFactory.create();
    context.prepareUpsert("update permission_templates set kee=? where kee=?")
      .setString(1, newKee)
      .setString(2, kee)
      .execute();
    return newKee;
  }

  private static void ensureOnlyDefaultOrganizationExists(Context context, String defaultOrganizationUuid) throws SQLException {
    Integer otherOrganizationCount = context.prepareSelect("select count(*) from organizations where uuid <> ?")
      .setString(1, defaultOrganizationUuid)
      .get(row -> row.getInt(1));
    checkState(otherOrganizationCount == 0,
      "Can not migrate DB if more than one organization exists. Remove all organizations but the default one which uuid is '%s'",
      defaultOrganizationUuid);
  }

  private static Map<String, String> retrieveDefaultTemplateProperties(Context context) throws SQLException {
    Map<String, String> templates = new HashMap<>(3);
    context.prepareSelect("select prop_key,is_empty,text_value from properties where prop_key in (?,?,?)")
      .setString(1, DEFAULT_TEMPLATE_PROPERTY)
      .setString(2, DEFAULT_PROJECT_TEMPLATE_PROPERTY)
      .setString(3, DEFAULT_VIEW_TEMPLATE_PROPERTY)
      .scroll(row -> {
        String key = row.getString(1);
        boolean isEmpty = row.getBoolean(2);
        String textValue = row.getString(3);
        if (!isEmpty) {
          templates.put(key, textValue);
        }
      });
    return templates;
  }
}
