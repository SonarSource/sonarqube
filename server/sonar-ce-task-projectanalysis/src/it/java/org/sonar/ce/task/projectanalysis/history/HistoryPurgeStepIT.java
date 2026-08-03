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
package org.sonar.ce.task.projectanalysis.history;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DBSessionsImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountHistoryRow;
import org.sonarsource.history.model.IssueTtrHistory;
import org.sonarsource.history.model.MeasureHistoryRow;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.HistoryMyBatisConfExtension;
import org.sonarsource.history.server.db.repository.IssueCountDimensionsRepository;
import org.sonarsource.history.server.db.repository.IssueCountHistoryRepository;
import org.sonarsource.history.server.db.repository.IssueTtrHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;
import org.sonarsource.history.server.service.HistoryPurgeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.config.PurgeConstants.DAYS_BEFORE_DELETING_HISTORY;

public class HistoryPurgeStepIT {
  private static final String ENTITY_ID = "entity_uuid";
  private static final EntityType ENTITY_TYPE = EntityType.PROJECT_BRANCH;
  private static final int DIMENSION_ID = 1;
  private static final int METRIC_ID = 1;
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate THRESHOLD_DATE = LocalDate.of(2026, Month.APRIL, 13);
  private static final Instant BEFORE_THRESHOLD = THRESHOLD_DATE.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
  private static final Instant AT_THRESHOLD = THRESHOLD_DATE.atStartOfDay().toInstant(ZoneOffset.UTC);
  private static final Instant AFTER_THRESHOLD = THRESHOLD_DATE.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

  @Rule
  public final DbTester db = DbTester.createWithConfExtension(System2.INSTANCE, new HistoryMyBatisConfExtension());

  private final DbClient dbClient = db.getDbClient();
  private final HistoryDbClient historyDbClient = new HistoryDbClient(
    dbClient.getMyBatis(),
    new DBSessionsImpl(dbClient.getMyBatis()),
    List.of(
      new IssueCountDimensionsRepository(),
      new IssueCountHistoryRepository(),
      new IssueTtrHistoryRepository(),
      new MeasureHistoryRepository(),
      new MeasureKeyMappingRepository()));
  private final HistoryPurgeService historyPurgeService = new HistoryPurgeService(
    historyDbClient,
    historyDbClient.issueCountHistoryRepository(),
    historyDbClient.measureHistoryRepository(),
    historyDbClient.issueTtrHistoryRepository());
  private final HistoryPurgeStep underTest = new HistoryPurgeStep(historyPurgeService, dbClient, CLOCK);

  @Test
  public void execute_deletes_history_on_or_before_configured_threshold_and_preserves_newer_history() {
    dbClient.propertiesDao().saveProperty(new PropertyDto()
      .setKey(DAYS_BEFORE_DELETING_HISTORY)
      .setValue("100"));
    insertHistoryRows();
    db.commit();

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countSql("select count(*) from issue_count_history")).isOne();
    assertThat(db.countSql("select count(*) from issue_count_history where recorded_at_epoch = " + AFTER_THRESHOLD.toEpochMilli())).isOne();
    assertThat(db.countSql("select count(*) from measure_history")).isOne();
    assertThat(db.countSql("select count(*) from measure_history where recorded_at_epoch = " + AFTER_THRESHOLD.toEpochMilli())).isOne();
    assertThat(db.countSql("select count(*) from issue_ttr_history")).isOne();
    assertThat(db.countSql("select count(*) from issue_ttr_history where recorded_at_epoch = " + AFTER_THRESHOLD.toEpochMilli())).isOne();
  }

  private void insertHistoryRows() {
    IssueCountHistoryRepository issueCountHistoryRepository = historyDbClient.issueCountHistoryRepository();
    issueCountHistoryRepository.upsert(db.getSession(), new IssueCountHistoryRow(ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, BEFORE_THRESHOLD, 1));
    issueCountHistoryRepository.upsert(db.getSession(), new IssueCountHistoryRow(ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, AT_THRESHOLD, 2));
    issueCountHistoryRepository.upsert(db.getSession(), new IssueCountHistoryRow(ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, AFTER_THRESHOLD, 3));

    MeasureHistoryRepository measureHistoryRepository = historyDbClient.measureHistoryRepository();
    measureHistoryRepository.upsert(db.getSession(), new MeasureHistoryRow(METRIC_ID, ENTITY_ID, ENTITY_TYPE, BEFORE_THRESHOLD, "1"));
    measureHistoryRepository.upsert(db.getSession(), new MeasureHistoryRow(METRIC_ID, ENTITY_ID, ENTITY_TYPE, AT_THRESHOLD, "2"));
    measureHistoryRepository.upsert(db.getSession(), new MeasureHistoryRow(METRIC_ID, ENTITY_ID, ENTITY_TYPE, AFTER_THRESHOLD, "3"));

    IssueTtrHistoryRepository issueTtrHistoryRepository = historyDbClient.issueTtrHistoryRepository();
    issueTtrHistoryRepository.upsert(db.getSession(), new IssueTtrHistory(ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, BEFORE_THRESHOLD, 10L, 1));
    issueTtrHistoryRepository.upsert(db.getSession(), new IssueTtrHistory(ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, AT_THRESHOLD, 20L, 2));
    issueTtrHistoryRepository.upsert(db.getSession(), new IssueTtrHistory(ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, AFTER_THRESHOLD, 30L, 3));
  }
}
