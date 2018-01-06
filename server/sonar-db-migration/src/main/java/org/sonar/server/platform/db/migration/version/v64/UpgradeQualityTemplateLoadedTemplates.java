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
package org.sonar.server.platform.db.migration.version.v64;

import java.security.MessageDigest;
import java.sql.SQLException;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

public class UpgradeQualityTemplateLoadedTemplates extends DataChange {
  private static final String QUALITY_PROFILE_TYPE = "QUALITY_PROFILE";

  private final DefaultOrganizationUuidProvider defaultOrganizationUuid;

  public UpgradeQualityTemplateLoadedTemplates(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuid) {
    super(db);
    this.defaultOrganizationUuid = defaultOrganizationUuid;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String defaultOrganizationUuid = this.defaultOrganizationUuid.getAndCheck(context);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate
        .select("select id,kee from loaded_templates where template_type=?")
        .setString(1, QUALITY_PROFILE_TYPE);
    massUpdate.rowPluralName("loaded templates for quality profiles");
    massUpdate.update("update loaded_templates set template_type=?,kee=? where id=?");
    MessageDigest md5Digest = DigestUtils.getMd5Digest();
    massUpdate.execute((row, update) -> {
      int id = row.getInt(1);
      String key = row.getString(2);

      update.setString(1, computeLoadedTemplateType(key, md5Digest));
      update.setString(2, defaultOrganizationUuid);
      update.setInt(3, id);
      return true;
    });
  }

  private static String computeLoadedTemplateType(String currentKey, MessageDigest messageDigest) {
    return format("%s.%s", QUALITY_PROFILE_TYPE, encodeHexString(messageDigest.digest(currentKey.getBytes(UTF_8))));
  }
}
