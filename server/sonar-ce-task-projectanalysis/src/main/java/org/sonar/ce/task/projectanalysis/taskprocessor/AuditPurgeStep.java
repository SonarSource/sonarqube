/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditDto;
import org.sonar.db.property.PropertyDto;

public final class AuditPurgeStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(AuditPurgeStep.class);

  private final AuditHousekeepingFrequencyHelper auditHousekeepingFrequencyHelper;
  private final DbClient dbClient;

  public AuditPurgeStep(AuditHousekeepingFrequencyHelper auditHousekeepingFrequencyHelper, DbClient dbClient) {
    this.auditHousekeepingFrequencyHelper = auditHousekeepingFrequencyHelper;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PropertyDto property = auditHousekeepingFrequencyHelper.getHouseKeepingFrequency(dbClient, dbSession);
      long deleteBefore = auditHousekeepingFrequencyHelper.getThresholdDate(property.getValue());
      Set<String> auditUuids = dbClient.auditDao()
        .selectOlderThan(dbSession, deleteBefore)
        .stream()
        .map(AuditDto::getUuid)
        .collect(Collectors.toSet());
      LOG.info(String.format("%s audit logs to be deleted...", auditUuids.size()));
      dbClient.auditDao().deleteByUuids(dbSession, auditUuids);
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Purge Audit Logs";
  }
}
