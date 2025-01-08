/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.taskprocessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.config.Frequency;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static org.sonar.core.config.PurgeConstants.AUDIT_HOUSEKEEPING_FREQUENCY;
import static org.sonar.core.config.PurgeProperties.DEFAULT_FREQUENCY;

public class AuditHousekeepingFrequencyHelper {
  private final System2 system2;

  public AuditHousekeepingFrequencyHelper(System2 system2) {
    this.system2 = system2;
  }

  public PropertyDto getHouseKeepingFrequency(DbClient dbClient, DbSession dbSession) {
    return Optional.ofNullable(dbClient.propertiesDao()
      .selectGlobalProperty(dbSession, AUDIT_HOUSEKEEPING_FREQUENCY))
      .orElse(defaultAuditHouseKeepingProperty());
  }

  public long getThresholdDate(String frequency) {
    Optional<Frequency> housekeepingFrequency = Arrays.stream(Frequency.values())
      .filter(f -> f.name().equalsIgnoreCase(frequency)).findFirst();
    if (housekeepingFrequency.isEmpty()) {
      throw new IllegalArgumentException("Unsupported frequency: " + frequency);
    }

    return Instant.ofEpochMilli(system2.now())
      .minus(housekeepingFrequency.get().getDays(), ChronoUnit.DAYS)
      .toEpochMilli();
  }

  private static PropertyDto defaultAuditHouseKeepingProperty() {
    PropertyDto property = new PropertyDto();
    property.setKey(AUDIT_HOUSEKEEPING_FREQUENCY);
    property.setValue(DEFAULT_FREQUENCY);
    return property;
  }
}
