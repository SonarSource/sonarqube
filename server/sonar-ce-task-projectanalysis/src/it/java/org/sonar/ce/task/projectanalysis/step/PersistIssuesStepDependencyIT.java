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
package org.sonar.ce.task.projectanalysis.step;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.issue.AdHocRuleCreator;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.ce.task.projectanalysis.issue.UpdateConflictResolver;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.ce.task.step.ComputationStep.Context;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.dependency.CveDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.issue.IssueStorage;

import static java.nio.file.Files.createTempFile;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.IssueStatus.OPEN;
import static org.sonar.api.rules.RuleType.VULNERABILITY;

class PersistIssuesStepDependencyIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final  BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @RegisterExtension
  private final  PeriodHolderRule periodHolder = new PeriodHolderRule();
  @TempDir
  private Path tempDir;

  private final DbSession dbSession = db.getSession();
  private final DbClient dbClient = db.getDbClient();

  private ProtoIssueCache protoIssueCache;

  private PersistIssuesStep persistIssuesStep;

  private ComponentDto project;
  RuleDto scaRule;
  private CveDto cve;

  @BeforeEach
  public void setUp() throws IOException {
    periodHolder.setPeriod(new Period(NewCodePeriodType.NUMBER_OF_DAYS.name(), "10", 1000L));
    protoIssueCache = new ProtoIssueCache(createTempFile(tempDir, null, null).toFile(), System2.INSTANCE);
    reportReader.setMetadata(ScannerReport.Metadata.getDefaultInstance());

    persistIssuesStep = new PersistIssuesStep(dbClient, System2.INSTANCE, new UpdateConflictResolver(), new RuleRepositoryImpl(mock(AdHocRuleCreator.class), dbClient),
      periodHolder, protoIssueCache, new IssueStorage(), UuidFactoryImpl.INSTANCE);

    project = db.components().insertPrivateProject().getMainBranchComponent();
    scaRule = db.rules().insert(RuleTesting.newRule(RuleKey.of("external_sca", "use-of-vulnerable-dependency")));
    cve = new CveDto("cve_uuid", "CVE-123", "Some CVE description", 1.0, 2.0, 3.0, 4L, 5L, 6L, 7L);
    dbClient.cveDao().insert(dbSession, cve);
    db.commit();
  }

  @Test
  void execute_shouldInsertCveId_whenNewCveIssue() {
    String issueKey = "ISSUE_KEY";
    DefaultIssue defaultIssue = new DefaultIssue()
      .setCveId(cve.id())
      .setKey(issueKey)
      .setType(VULNERABILITY)
      .setComponentKey(project.getKey())
      .setComponentUuid(project.uuid())
      .setProjectUuid(project.uuid())
      .setProjectKey(project.getKey())
      .setRuleKey(scaRule.getKey())
      .setStatus(OPEN.toString())
      .setCreationDate(new Date());
    protoIssueCache.newAppender().append(defaultIssue).close();

    Context context = new TestComputationStepContext();
    persistIssuesStep.execute(context);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from issues_dependency");
    Assertions.assertThat(select).hasSize(1);
    Assertions.assertThat(select.get(0)).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "issue_uuid", issueKey,
        "cve_uuid", cve.uuid())
    );
  }
}
