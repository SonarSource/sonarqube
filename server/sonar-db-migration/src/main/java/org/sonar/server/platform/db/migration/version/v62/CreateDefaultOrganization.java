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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;

public class CreateDefaultOrganization extends DataChange {
  private static final String KEY_DEFAULT_ORGANIZATION = "default-organization";
  private static final String INTERNAL_PROPERTY_DEFAULT_ORGANIZATION = "organization.default";

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public CreateDefaultOrganization(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    String uuid = createDefaultOrganization(context);
    saveDefaultOrganizationUuid(context, uuid);
  }

  private String createDefaultOrganization(Context context) throws SQLException {
    Select select = context.prepareSelect("select uuid from organizations where kee=?");
    select.setString(1, KEY_DEFAULT_ORGANIZATION);
    String uuid = select.get(row -> row.getNullableString(1));
    if (uuid == null) {
      uuid = uuidFactory.create();
      long now = system2.now();
      context.prepareUpsert("insert into organizations" +
        " (uuid, kee, name, created_at, updated_at)" +
        " values" +
        " (?, ?, ?, ?, ?)")
        .setString(1, uuid)
        .setString(2, KEY_DEFAULT_ORGANIZATION)
        .setString(3, "Default Organization")
        .setLong(4, now)
        .setLong(5, now)
        .execute()
        .commit();
    }
    return uuid;
  }

  private void saveDefaultOrganizationUuid(Context context, String uuid) throws SQLException {
    Select select = context.prepareSelect("select kee from internal_properties where kee=?");
    select.setString(1, INTERNAL_PROPERTY_DEFAULT_ORGANIZATION);
    if (select.get(row -> row.getNullableString(1)) == null) {
      context.prepareUpsert("insert into internal_properties" +
        " (kee, is_empty, text_value, created_at)" +
        " values" +
        " (?, ?, ?, ?)")
        .setString(1, INTERNAL_PROPERTY_DEFAULT_ORGANIZATION)
        .setBoolean(2, false)
        .setString(3, uuid)
        .setLong(4, system2.now())
        .execute()
        .commit();
    }
  }
}
