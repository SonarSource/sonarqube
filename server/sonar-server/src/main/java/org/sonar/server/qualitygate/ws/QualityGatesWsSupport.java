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

package org.sonar.server.qualitygate.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;
import static org.sonar.server.ws.WsUtils.checkFound;

public class QualityGatesWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public QualityGatesWsSupport(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  QualityGateConditionDto getCondition(DbSession dbSession, long id) {
    return checkFound(dbClient.gateConditionDao().selectById(id, dbSession), "No quality gate condition with id '%d'", id);
  }

  OrganizationDto getOrganization(DbSession dbSession) {
    String organizationKey = defaultOrganizationProvider.get().getKey();
    return dbClient.organizationDao().selectByKey(dbSession, organizationKey)
      .orElseThrow(() -> new NotFoundException(format("No organization with key '%s'", organizationKey)));
  }

  boolean isQualityGateAdmin() {
    return userSession.hasPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
  }

  Qualitygates.Actions getActions(QualityGateDto qualityGate, @Nullable QualityGateDto defaultQualityGate) {
    Long defaultId = defaultQualityGate == null ? null : defaultQualityGate.getId();
    boolean isDefault = qualityGate.getId().equals(defaultId);
    boolean isBuiltIn = qualityGate.isBuiltIn();
    boolean isQualityGateAdmin = isQualityGateAdmin();
    return Qualitygates.Actions.newBuilder()
      .setCopy(isQualityGateAdmin)
      .setEdit(!isBuiltIn && isQualityGateAdmin)
      .setSetAsDefault(!isDefault && isQualityGateAdmin)
      .setAssociateProjects(!isDefault && isQualityGateAdmin)
      .build();
  }

  @CheckForNull
  QualityGateDto getDefault(DbSession dbSession) {
    Long defaultId = getDefaultId(dbSession);
    if (defaultId == null) {
      return null;
    }
    return dbClient.qualityGateDao().selectById(dbSession, defaultId);
  }

  @CheckForNull
  private Long getDefaultId(DbSession dbSession) {
    PropertyDto defaultQgate = dbClient.propertiesDao().selectGlobalProperty(dbSession, SONAR_QUALITYGATE_PROPERTY);
    if (defaultQgate == null || StringUtils.isBlank(defaultQgate.getValue())) {
      return null;
    }
    return Long.valueOf(defaultQgate.getValue());
  }

  void checkCanEdit(QualityGateDto qualityGate) {
    checkNotBuiltInt(qualityGate);
    userSession.checkPermission(ADMINISTER_QUALITY_GATES, defaultOrganizationProvider.get().getUuid());
  }

  private static void checkNotBuiltInt(QualityGateDto qualityGate) {
    checkArgument(!qualityGate.isBuiltIn(), "Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName());
  }
}
