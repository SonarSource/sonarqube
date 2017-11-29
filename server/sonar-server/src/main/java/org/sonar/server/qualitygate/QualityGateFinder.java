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
package org.sonar.server.qualitygate;

import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;
import static org.sonar.server.ws.WsUtils.checkFound;

public class QualityGateFinder {

  private final DbClient dbClient;

  public QualityGateFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Return effective quality gate of a project.
   *
   * It will first try to get the quality gate explicitly defined on a project, if none it will try to return default quality gate.
   * As it's possible to have no default quality gate, this method can return {@link Optional#empty()}
   */
  public Optional<QualityGateData> getQualityGate(DbSession dbSession, long componentId) {
    Optional<Long> qualityGateId = dbClient.projectQgateAssociationDao().selectQGateIdByComponentId(dbSession, componentId);
    if (qualityGateId.isPresent()) {
      return Optional.of(new QualityGateData(getById(dbSession, qualityGateId.get()), false));
    } else {
      Optional<QualityGateDto> defaultQualityGate = getDefault(dbSession);
      if (!defaultQualityGate.isPresent()) {
        return Optional.empty();
      }
      return Optional.of(new QualityGateData(defaultQualityGate.get(), true));
    }
  }

  public QualityGateDto getById(DbSession dbSession, long qualityGateId) {
    return checkFound(dbClient.qualityGateDao().selectById(dbSession, qualityGateId), "No quality gate has been found for id %s", qualityGateId);
  }

  public QualityGateDto getByNameOrId(DbSession dbSession, @Nullable String name, @Nullable Long id) {
    if (name != null) {
      return checkFound(dbClient.qualityGateDao().selectByName(dbSession, name), "No quality gate has been found for name %s", name);
    }
    if (id != null) {
      return getById(dbSession, id);
    }
    throw new IllegalArgumentException("No parameter has been set to identify a quality gate");
  }

  public Optional<QualityGateDto> getDefault(DbSession dbSession) {
    Optional<Long> defaultQualityGateId = getDefaultId(dbSession);

    if (!defaultQualityGateId.isPresent()) {
      // For the moment, it's possible to have no default quality gate, but it will change with SONAR-8507
      return Optional.empty();
    } else {
      return Optional.ofNullable(
        dbClient.qualityGateDao().selectById(dbSession, defaultQualityGateId.get()));
    }
  }

  public QualityGateDto getBuiltInQualityGate(DbSession dbSession) {
    QualityGateDto builtIn = dbClient.qualityGateDao().selectBuiltIn(dbSession);
    checkState(builtIn != null, "Builtin quality gate is missing.");
    return builtIn;
  }

  private Optional<Long> getDefaultId(DbSession dbSession) {
    PropertyDto defaultQualityGateId = dbClient.propertiesDao().selectGlobalProperty(dbSession, SONAR_QUALITYGATE_PROPERTY);

    if (defaultQualityGateId == null || StringUtils.isBlank(defaultQualityGateId.getValue())) {
      // For the moment, it's possible to have no default quality gate, but it will change with SONAR-8507
      return Optional.empty();
    }

    return Optional.of(Long.valueOf(defaultQualityGateId.getValue()));
  }

  public static class QualityGateData {
    private final QualityGateDto qualityGate;
    private final boolean isDefault;

    private QualityGateData(QualityGateDto qualityGate, boolean isDefault) {
      this.qualityGate = qualityGate;
      this.isDefault = isDefault;
    }

    public QualityGateDto getQualityGate() {
      return qualityGate;
    }

    public boolean isDefault() {
      return isDefault;
    }
  }

}
