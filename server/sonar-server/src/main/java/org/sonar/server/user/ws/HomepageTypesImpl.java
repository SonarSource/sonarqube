/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.user.ws;

import java.util.EnumSet;
import java.util.List;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.process.ProcessProperties;
import org.sonar.server.organization.OrganizationFlags;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.sonar.server.user.ws.HomepageTypes.Type.ISSUES;
import static org.sonar.server.user.ws.HomepageTypes.Type.MY_ISSUES;
import static org.sonar.server.user.ws.HomepageTypes.Type.MY_PROJECTS;
import static org.sonar.server.user.ws.HomepageTypes.Type.ORGANIZATION;
import static org.sonar.server.user.ws.HomepageTypes.Type.PROJECT;
import static org.sonar.server.user.ws.HomepageTypes.Type.PROJECTS;
import static org.sonar.server.user.ws.HomepageTypes.Type.values;

public class HomepageTypesImpl implements HomepageTypes, Startable {

  private static final EnumSet<Type> ON_SONARQUBE = EnumSet.of(PROJECTS, PROJECT, ISSUES, ORGANIZATION);
  private static final EnumSet<Type> ON_SONARCLOUD = EnumSet.of(PROJECT, MY_PROJECTS, MY_ISSUES, ORGANIZATION);

  private final Configuration configuration;
  private final OrganizationFlags organizationFlags;
  private final DbClient dbClient;

  private List<Type> types;

  public HomepageTypesImpl(Configuration configuration, OrganizationFlags organizationFlags, DbClient dbClient) {
    this.configuration = configuration;
    this.organizationFlags = organizationFlags;
    this.dbClient = dbClient;
  }

  @Override
  public List<Type> getTypes() {
    checkState(types != null, "Homepage types have not been initialized yet");
    return types;
  }

  @Override
  public Type getDefaultType() {
    return isOnSonarCloud() ? MY_PROJECTS : PROJECTS;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      boolean isOnSonarCloud = isOnSonarCloud();
      boolean isOrganizationEnabled = isOrganizationEnabled(dbSession);
      this.types = stream(values())
        .filter(type -> (isOnSonarCloud && ON_SONARCLOUD.contains(type)) || (!isOnSonarCloud && ON_SONARQUBE.contains(type)))
        .filter(type -> isOrganizationEnabled || !(type.equals(ORGANIZATION)))
        .collect(toList());
    }
  }

  private boolean isOrganizationEnabled(DbSession dbSession) {
    return organizationFlags.isEnabled(dbSession);
  }

  private Boolean isOnSonarCloud() {
    return configuration.getBoolean(ProcessProperties.Property.SONARCLOUD_ENABLED.getKey()).orElse(false);
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
