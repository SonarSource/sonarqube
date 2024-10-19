/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v102;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class AddUserConsentRequiredIfGithubAutoProvisioningEnabled extends DataChange {

  private static final Logger LOG = LoggerFactory.getLogger(AddUserConsentRequiredIfGithubAutoProvisioningEnabled.class);
  @VisibleForTesting
  static final String PROVISIONING_GITHUB_ENABLED_PROP_KEY = "provisioning.github.enabled";

  @VisibleForTesting
  static final String PROP_KEY = "sonar.auth.github.userConsentForPermissionProvisioningRequired";

  private static final String INSERT_QUERY = """
    insert into properties (uuid, prop_key, is_empty, created_at)
    values (?, ?, ?, ?)
    """;

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AddUserConsentRequiredIfGithubAutoProvisioningEnabled(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }
  @Override
  protected void execute(DataChange.Context context) throws SQLException {
    if (!isGithubAutoProvisioningEnabled(context)) {
      return;
    }
    if (isUserConsentAlreadyRequired(context)) {
      return;
    }
    LOG.warn("Automatic synchronization was previously activated for GitHub. It requires user consent to continue working as new " +
             " features were added with the synchronization. Please read the upgrade notes.");
    Upsert upsert = context.prepareUpsert(INSERT_QUERY);
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, PROP_KEY)
      .setBoolean(3, true)
      .setLong(4, system2.now())
      .execute()
      .commit();
  }

  private static boolean isUserConsentAlreadyRequired(Context context) throws SQLException {
    return Optional.ofNullable(context.prepareSelect("select count(*) from properties where prop_key = ?")
      .setString(1, PROP_KEY)
      .get(t -> 1 == t.getInt(1)))
      .orElseThrow();
  }

  private static boolean isGithubAutoProvisioningEnabled(Context context) throws SQLException {
    return Optional.ofNullable(context.prepareSelect("select count(*) from internal_properties where kee = ? and text_value = ?")
      .setString(1, PROVISIONING_GITHUB_ENABLED_PROP_KEY)
      .setString(2, "true")
      .get(t -> 1 == t.getInt(1)))
      .orElseThrow();
  }

}
