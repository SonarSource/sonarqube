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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.util.Validation;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class QualityGateUpdater {

  private final DbClient dbClient;

  public QualityGateUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateDto create(DbSession dbSession, String name) {
    validateQualityGate(dbSession, null, name);
    QualityGateDto newQualityGate = new QualityGateDto().setName(name);
    dbClient.qualityGateDao().insert(dbSession, newQualityGate);
    return newQualityGate;
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
