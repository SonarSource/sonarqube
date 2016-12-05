/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import javax.annotation.CheckForNull;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;

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
      return Optional.of(new QualityGateData(selectOrFailById(dbSession, qualityGateId.get()), false));
    } else {
      QualityGateDto defaultQualityGate = getDefault(dbSession);
      if (defaultQualityGate == null) {
        return Optional.empty();
      }
      return Optional.of(new QualityGateData(defaultQualityGate, true));
    }
  }

  @CheckForNull
  private QualityGateDto getDefault(DbSession dbSession) {
    Long defaultId = getDefaultId(dbSession);
    if (defaultId == null) {
      return null;
    }
    return selectOrFailById(dbSession, defaultId);
  }

  private QualityGateDto selectOrFailById(DbSession dbSession, long qualityGateId) {
    QualityGateDto qualityGateDto = dbClient.qualityGateDao().selectById(dbSession, qualityGateId);
    if (qualityGateDto == null) {
      throw new NotFoundException(String.format("No quality gate has been found for id %s", qualityGateId));
    }
    return qualityGateDto;
  }

  @CheckForNull
  private Long getDefaultId(DbSession dbSession) {
    PropertyDto defaultQgate = dbClient.propertiesDao().selectGlobalProperty(dbSession, SONAR_QUALITYGATE_PROPERTY);
    if (defaultQgate == null || isBlank(defaultQgate.getValue())) {
      // For the moment, it's possible to have no default quality gate, but it will change with SONAR-8507
      return null;
    }
    return Long.valueOf(defaultQgate.getValue());
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
