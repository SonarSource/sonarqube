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
package org.sonar.server.organization;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.property.InternalProperties;

import static java.lang.String.valueOf;

public class OrganizationFlagsImpl implements OrganizationFlags {

  public static final String FAILURE_MESSAGE_ENABLED = "Organization support is enabled";
  public static final String FAILURE_MESSAGE_DISABLED = "Organization support is disabled";

  private final DbClient dbClient;

  public OrganizationFlagsImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public boolean isEnabled(DbSession dbSession) {
    Optional<String> value = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.ORGANIZATION_ENABLED);
    return value.map("true"::equals).orElse(false);
  }

  @Override
  public void checkEnabled(DbSession dbSession) {
    if (!isEnabled(dbSession)) {
      throw new IllegalStateException(FAILURE_MESSAGE_DISABLED);
    }
  }

  @Override
  public void checkDisabled(DbSession dbSession) {
    if (isEnabled(dbSession)) {
      throw new IllegalStateException(FAILURE_MESSAGE_ENABLED);
    }
  }

  @Override
  public void enable(DbSession dbSession) {
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.ORGANIZATION_ENABLED, valueOf(true));
  }
}
