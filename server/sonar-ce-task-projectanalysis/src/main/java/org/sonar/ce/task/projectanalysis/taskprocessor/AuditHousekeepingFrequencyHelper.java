/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.core.config.Frequency;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static org.sonar.core.config.PurgeConstants.AUDIT_HOUSEKEEPING_FREQUENCY;
import static org.sonar.core.config.PurgeConstants.AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeConstants.DEFAULT_AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeConstants.MAX_AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeConstants.MIN_AUDIT_PURGE_BATCH_SIZE;
import static org.sonar.core.config.PurgeProperties.DEFAULT_FREQUENCY;

public class AuditHousekeepingFrequencyHelper {
  private static final Logger LOG = LoggerFactory.getLogger(AuditHousekeepingFrequencyHelper.class);

  private final System2 system2;

  public AuditHousekeepingFrequencyHelper(System2 system2) {
    this.system2 = system2;
  }

  public PropertyDto getHouseKeepingFrequency(DbClient dbClient, DbSession dbSession) {
    return Optional.ofNullable(dbClient.propertiesDao()
      .selectGlobalProperty(dbSession, AUDIT_HOUSEKEEPING_FREQUENCY))
      .orElse(defaultAuditHouseKeepingProperty());
  }

  public int getPurgeBatchSize(DbClient dbClient, DbSession dbSession) {
    PropertyDto property = dbClient.propertiesDao().selectGlobalProperty(dbSession, AUDIT_PURGE_BATCH_SIZE);
    if (property == null || property.getValue() == null) {
      return DEFAULT_AUDIT_PURGE_BATCH_SIZE;
    }
    int value;
    try {
      value = Integer.parseInt(property.getValue());
    } catch (NumberFormatException e) {
      LOG.warn("Invalid value '{}' for property {}, falling back to default {}", property.getValue(), AUDIT_PURGE_BATCH_SIZE, DEFAULT_AUDIT_PURGE_BATCH_SIZE);
      return DEFAULT_AUDIT_PURGE_BATCH_SIZE;
    }
    if (value < MIN_AUDIT_PURGE_BATCH_SIZE || value > MAX_AUDIT_PURGE_BATCH_SIZE) {
      LOG.warn("Invalid value '{}' for property {}, falling back to default {}", value, AUDIT_PURGE_BATCH_SIZE, DEFAULT_AUDIT_PURGE_BATCH_SIZE);
      return DEFAULT_AUDIT_PURGE_BATCH_SIZE;
    }
    return value;
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
