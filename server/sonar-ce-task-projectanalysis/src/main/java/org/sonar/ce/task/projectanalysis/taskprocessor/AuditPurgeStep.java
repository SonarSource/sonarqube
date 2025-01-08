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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.logs.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static java.lang.String.format;

public final class AuditPurgeStep implements ComputationStep {
  private static final Logger LOG = LoggerFactory.getLogger(AuditPurgeStep.class);

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
      long threshold = auditHousekeepingFrequencyHelper.getThresholdDate(property.getValue());
      Profiler profiler = Profiler.create(LOG).logTimeLast(true);
      profiler.startInfo("Purge audit logs");
      long deleted = dbClient.auditDao().deleteBefore(dbSession, threshold);
      dbSession.commit();
      profiler.stopInfo(format("Purged %d audit logs", deleted));
    }
  }

  @Override
  public String getDescription() {
    return "Purge Audit Logs";
  }
}
