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

import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.Errors;
import org.sonar.server.exceptions.Message;
import org.sonar.server.util.Validation;

import static com.google.common.base.Strings.isNullOrEmpty;

public class QualityGateUpdater {

  private final DbClient dbClient;

  public QualityGateUpdater(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QualityGateDto create(DbSession dbSession, String name) {
    validateQualityGate(null, name);
    QualityGateDto newQualityGate = new QualityGateDto().setName(name);
    dbClient.qualityGateDao().insert(dbSession, newQualityGate);
    return newQualityGate;
  }

  private void validateQualityGate(@Nullable Long qGateId, @Nullable String name) {
    Errors errors = new Errors();
    if (isNullOrEmpty(name)) {
      errors.add(Message.of(Validation.CANT_BE_EMPTY_MESSAGE, "Name"));
    } else {
      checkQualityGateDoesNotAlreadyExist(qGateId, name, errors);
    }
    if (!errors.isEmpty()) {
      throw new BadRequestException(errors);
    }
  }

  private void checkQualityGateDoesNotAlreadyExist(@Nullable Long qGateId, String name, Errors errors) {
    QualityGateDto existingQgate = dbClient.qualityGateDao().selectByName(name);
    boolean isModifyingCurrentQgate = qGateId != null && existingQgate != null && existingQgate.getId().equals(qGateId);
    errors.check(isModifyingCurrentQgate || existingQgate == null, Validation.IS_ALREADY_USED_MESSAGE, "Name");
  }
}
