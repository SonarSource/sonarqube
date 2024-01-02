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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class LogMessageIfSonarScimEnabledPresentProperty extends DataChange {

  private static final Logger LOG = LoggerFactory.getLogger(LogMessageIfSonarScimEnabledPresentProperty.class);
  public static final String SONAR_SCIM_ENABLED = "sonar.scim.enabled";
  private static final String SCIM_DOC_URL = "https://docs.sonarsource.com/sonarqube/10.1/instance-administration/authentication/saml/scim/overview/";

  public LogMessageIfSonarScimEnabledPresentProperty(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    context.prepareSelect("select * from properties where prop_key = ?")
      .setString(1, SONAR_SCIM_ENABLED)
      .scroll(row -> LOG.warn("'{}' property is defined but not read anymore." +
        " Please read the upgrade notes for the instruction to upgrade. User provisioning is deactivated until reactivated" +
        " from the SonarQube Administration Interface (\"General->Authentication\"). See documentation: {}", SONAR_SCIM_ENABLED,
        SCIM_DOC_URL));
  }
}
