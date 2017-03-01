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
package org.sonar.db.qualityprofile;

import java.util.Date;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static java.util.Arrays.stream;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextLong;

public class QualityProfileTesting {

  public static QualityProfileDto newQualityProfileDto() {
    String uuid = Uuids.createFast();
    QualityProfileDto dto = QualityProfileDto.createFor(uuid)
      .setOrganizationUuid(randomAlphanumeric(40))
      .setName(uuid)
      .setLanguage(randomAlphanumeric(20))
      .setLastUsed(nextLong());
    dto.setCreatedAt(new Date())
      .setUpdatedAt(new Date());
    return dto;
  }

  public static QProfileChangeDto newQProfileChangeDto() {
    return new QProfileChangeDto()
      .setKey(randomAlphanumeric(40))
      .setProfileKey(randomAlphanumeric(40))
      .setCreatedAt(nextLong())
      .setChangeType("ACTIVATED")
      .setLogin(randomAlphanumeric(10));
  }

  public static void insert(DbTester dbTester, QProfileChangeDto... dtos) {
    // do not use QProfileChangeDao so that generated fields key and creation date
    // can be defined by tests
    DbSession dbSession = dbTester.getSession();
    QProfileChangeMapper mapper = dbSession.getMapper(QProfileChangeMapper.class);
    stream(dtos).forEach(dto -> mapper.insert(dto));
    dbSession.commit();
  }
}
