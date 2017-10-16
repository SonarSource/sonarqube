/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.license;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.util.Objects.requireNonNull;

public class LicenseCommitMock implements LicenseCommit {
  private static final String LICENSE_PROPERTY_KEY = "sonarsource.license";

  private final DbClient dbClient;

  public LicenseCommitMock(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void update(String newLicense) {
    requireNonNull(newLicense, "newLicense can't be null");

    if (newLicense.contains("failMe")) {
      throw new IllegalArgumentException("Invalid license");
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().save(dbSession, LICENSE_PROPERTY_KEY, newLicense);
    }
  }

  @Override
  public void delete() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.internalPropertiesDao().saveAsEmpty(dbSession, LICENSE_PROPERTY_KEY);
    }
  }
}
