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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.util.Validation;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class QualityGateUpdater {

  public static final String SONAR_QUALITYGATE_PROPERTY = "sonar.qualitygate";

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;

  public QualityGateUpdater(DbClient dbClient, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
  }

  public QualityGateDto create(DbSession dbSession, String name) {
    validateQualityGate(dbSession, null, name);
    QualityGateDto newQualityGate = new QualityGateDto()
      .setName(name)
      .setBuiltIn(false)
      .setUuid(uuidFactory.create());
    dbClient.qualityGateDao().insert(dbSession, newQualityGate);
    return newQualityGate;
  }

  public void setDefault(DbSession dbSession, @Nullable QualityGateDto qualityGateDto) {
    if (qualityGateDto == null) {
      dbClient.propertiesDao().deleteGlobalProperty(SONAR_QUALITYGATE_PROPERTY, dbSession);
    } else {
      checkQualityGateExistence(dbSession, qualityGateDto.getId());
      dbClient.propertiesDao().saveProperty(dbSession,
        new PropertyDto().setKey(SONAR_QUALITYGATE_PROPERTY).setValue(qualityGateDto.getId().toString()));
    }
  }

  private void checkQualityGateExistence(DbSession dbSession, @Nullable Long qualityGateId) {
    if (qualityGateId == null ||
      dbClient.qualityGateDao().selectById(dbSession, qualityGateId) == null) {
      throw new NotFoundException("There is no quality gate with id=" + qualityGateId);
    }
  }

  private void validateQualityGate(DbSession dbSession, @Nullable Long qGateId, @Nullable String name) {
    List<String> errors = new ArrayList<>();
    if (isNullOrEmpty(name)) {
      errors.add(format(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      checkQualityGateDoesNotAlreadyExist(dbSession, qGateId, name, errors);
    }
    checkRequest(errors.isEmpty(), errors);
  }

  private void checkQualityGateDoesNotAlreadyExist(DbSession dbSession, @Nullable Long qGateId, String name, List<String> errors) {
    QualityGateDto existingQgate = dbClient.qualityGateDao().selectByName(dbSession, name);
    boolean isModifyingCurrentQgate = qGateId != null && existingQgate != null && existingQgate.getId().equals(qGateId);
    if (!isModifyingCurrentQgate && existingQgate != null) {
      errors.add(format(Validation.IS_ALREADY_USED_MESSAGE, "Name"));
    }
  }
}
