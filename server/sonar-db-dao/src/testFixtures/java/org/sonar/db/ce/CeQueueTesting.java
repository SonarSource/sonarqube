/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.ce;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

public class CeQueueTesting {

  private static final Random RANDOM = new SecureRandom();

  private CeQueueTesting() {
    // static methods only
  }

  public static CeQueueDto newCeQueueDto(String uuid) {
    return new CeQueueDto()
      .setUuid(uuid)
      .setComponentUuid(secure().nextAlphanumeric(40))
      .setEntityUuid(secure().nextAlphanumeric(39))
      .setStatus(CeQueueDto.Status.PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setSubmitterUuid(secure().nextAlphanumeric(255))
      .setCreatedAt(RANDOM.nextLong(Long.MAX_VALUE))
      .setUpdatedAt(RANDOM.nextLong(Long.MAX_VALUE));
  }

  public static void makeInProgress(DbSession dbSession, String workerUuid, long now, CeQueueDto... ceQueueDtos) {
    Stream.of(ceQueueDtos).forEach(ceQueueDto -> {
      CeQueueMapper mapper = dbSession.getMapper(CeQueueMapper.class);
      int touchedRows = mapper.updateIf(ceQueueDto.getUuid(),
        new UpdateIf.NewProperties(IN_PROGRESS, workerUuid, now, now),
        new UpdateIf.OldProperties(PENDING));
      assertThat(touchedRows).isOne();
    });
  }

  public static void reset(DbSession dbSession, long now, CeQueueDto... ceQueueDtos) {
    Stream.of(ceQueueDtos).forEach(ceQueueDto -> {
      checkArgument(ceQueueDto.getStatus() == IN_PROGRESS);
      checkArgument(ceQueueDto.getWorkerUuid() != null);

      CeQueueMapper mapper = dbSession.getMapper(CeQueueMapper.class);
      int touchedRows = mapper.updateIf(ceQueueDto.getUuid(),
        new UpdateIf.NewProperties(PENDING, ceQueueDto.getUuid(), now, now),
        new UpdateIf.OldProperties(IN_PROGRESS));
      assertThat(touchedRows).isOne();
    });
  }
}
