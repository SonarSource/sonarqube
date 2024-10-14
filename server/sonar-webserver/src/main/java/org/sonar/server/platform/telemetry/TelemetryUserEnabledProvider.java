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
package org.sonar.server.platform.telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;
import org.sonar.server.util.DigestUtil;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

public class TelemetryUserEnabledProvider extends AbstractTelemetryDataProvider<Boolean> {

  private final DbClient dbClient;

  public TelemetryUserEnabledProvider(DbClient dbClient) {
    super("user_enabled", Dimension.USER, Granularity.DAILY, TelemetryDataType.BOOLEAN);
    this.dbClient = dbClient;
  }

  @Override
  public Map<String, Boolean> getValues() {
    Map<String, Boolean> result = new HashMap<>();
    int pageSize = 1000;
    int page = 1;
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserDto> userDtos;
      do {
        userDtos = dbClient.userDao().selectUsers(dbSession, UserQuery.builder().build(), page, pageSize);
        for (UserDto userDto : userDtos) {
          String anonymizedUuid = DigestUtil.sha3_224Hex(userDto.getUuid());
          result.put(anonymizedUuid, userDto.isActive());
        }
        page++;
      } while (!userDtos.isEmpty());
    }
    return result;
  }
}
