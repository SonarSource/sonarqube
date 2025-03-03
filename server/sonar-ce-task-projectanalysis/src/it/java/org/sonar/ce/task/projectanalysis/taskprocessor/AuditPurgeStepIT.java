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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditDto;
import org.sonar.db.audit.AuditTesting;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.taskprocessor.ContextUtils.EMPTY_CONTEXT;
import static org.sonar.core.config.Frequency.MONTHLY;
import static org.sonar.core.config.PurgeConstants.AUDIT_HOUSEKEEPING_FREQUENCY;

public class AuditPurgeStepIT {
  private static final long NOW = 1_400_000_000_000L;
  private static final long BEFORE = 1_300_000_000_000L;
  private static final long LATER = 1_500_000_000_000L;
  private static final PropertyDto FREQUENCY_PROPERTY = new PropertyDto()
    .setKey(AUDIT_HOUSEKEEPING_FREQUENCY)
    .setValue(MONTHLY.name());

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();

  private final System2 system2 = new System2();

  @Rule
  public final DbTester db = DbTester.create(system2);

  private final AuditHousekeepingFrequencyHelper auditHousekeepingFrequencyHelper = mock(AuditHousekeepingFrequencyHelper.class);

  private final AuditPurgeStep underTest = new AuditPurgeStep(auditHousekeepingFrequencyHelper, dbClient);

  @Before
  public void setUp() {
    when(auditHousekeepingFrequencyHelper.getHouseKeepingFrequency(any(), any())).thenReturn(FREQUENCY_PROPERTY);
    when(auditHousekeepingFrequencyHelper.getThresholdDate(anyString())).thenReturn(NOW);
  }

  @Test
  public void executeDeletesOlderAudits() {
    prepareRowsWithDeterministicCreatedAt();
    assertThat(dbClient.auditDao().selectOlderThan(db.getSession(), LATER + 1)).hasSize(3);

    underTest.execute(EMPTY_CONTEXT);

    assertThat(dbClient.auditDao().selectOlderThan(db.getSession(), LATER + 1)).hasSize(2);
  }

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Purge Audit Logs");
  }

  private void prepareRowsWithDeterministicCreatedAt() {
    insertAudit(BEFORE);
    insertAudit(NOW);
    insertAudit(LATER);
    db.getSession().commit();
  }

  private void insertAudit(long timestamp) {
    AuditDto auditDto = AuditTesting.newAuditDto(timestamp);
    dbClient.auditDao().insert(db.getSession(), auditDto);
  }
}
