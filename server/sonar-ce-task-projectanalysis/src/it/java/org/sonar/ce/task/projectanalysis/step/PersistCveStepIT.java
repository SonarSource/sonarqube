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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.dependency.CveCweDto;
import org.sonar.db.dependency.CveDto;
import org.sonar.scanner.protocol.output.ScannerReport.Cve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PersistCveStepIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  @RegisterExtension
  private final BatchReportReaderRule batchReportReader = new BatchReportReaderRule();

  private final DbSession dbSession = db.getSession();
  private final DbClient dbClient = db.getDbClient();
  private final UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;

  private PersistCveStep persistCveStep;

  @BeforeEach
  void setUp() {
    persistCveStep = new PersistCveStep(batchReportReader, dbClient, uuidFactory, System2.INSTANCE);
  }

  @Test
  void getDescription_shouldReturnStepDescription() {
    assertThat(persistCveStep.getDescription()).isEqualTo("Persist CVEs");
  }

  @Test
  void execute_shouldInsertNewCVEs() {
    Cve cve1 = buildCve("1").build();
    Cve cve2 = buildCve("2").build();
    Cve cveAllOptionalEmpty = Cve.newBuilder().setCveId("CVE-empty").setDescription("Empty CVE").build();
    batchReportReader.putCves(List.of(cve1, cve2, cveAllOptionalEmpty));

    persistCveStep.execute(new TestComputationStepContext());

    assertCvePersistedInDatabase(cve1);
    assertCvePersistedInDatabase(cve2);
    assertCvePersistedInDatabase(cveAllOptionalEmpty);
  }

  private void assertCvePersistedInDatabase(Cve cve) {
    CveDto cveDto = dbClient.cveDao().selectById(dbSession, cve.getCveId())
      .orElseGet(() -> fail(String.format("CVE with id %s not found", cve.getCveId())));
    assertThat(cveDto.id()).isEqualTo(cve.getCveId());
    assertThat(cveDto.description()).isEqualTo(cve.getDescription());
    if (cve.hasCvssScore()) {
      assertThat(cveDto.cvssScore()).isEqualTo(cve.getCvssScore());
    } else {
      assertThat(cveDto.cvssScore()).isNull();
    }
    if (cve.hasEpssScore()) {
      assertThat(cveDto.epssScore()).isEqualTo(cve.getEpssScore());
    } else {
      assertThat(cveDto.epssScore()).isNull();
    }
    if (cve.hasEpssPercentile()) {
      assertThat(cveDto.epssPercentile()).isEqualTo(cve.getEpssPercentile());
    } else {
      assertThat(cveDto.epssPercentile()).isNull();
    }
    if (cve.hasPublishedDate()) {
      assertThat(cveDto.publishedAt()).isEqualTo(cve.getPublishedDate());
    } else {
      assertThat(cveDto.publishedAt()).isNull();
    }
    if (cve.hasLastModifiedDate()) {
      assertThat(cveDto.lastModifiedAt()).isEqualTo(cve.getLastModifiedDate());
    } else {
      assertThat(cveDto.lastModifiedAt()).isNull();
    }
    assertThat(cveDto.uuid()).isNotBlank();
    assertThat(cveDto.createdAt()).isNotNull();
    assertThat(cveDto.updatedAt()).isNotNull();
  }

  @Test
  void execute_shoudUpdateExistingCves() {
    dbClient.cveDao().insert(dbSession, new CveDto("cve-uuid-1", "CVE-1", "Old description 1", 10.0, 20.0, 30.0, 0L, 0L, 0L, 0L));
    dbClient.cveDao().insert(dbSession, new CveDto("cve-uuid-2", "CVE-2", "Old description 2", null, null, null, null, null, 0L, 0L));
    db.commit();
    Cve cve1 = buildCve("1").build();
    Cve cve2 = buildCve("2").build();
    batchReportReader.putCves(List.of(cve1, cve2));

    persistCveStep.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable(dbSession, "cves")).isEqualTo(2);
    assertCvePersistedInDatabase(cve1);
    assertCvePersistedInDatabase(cve2);
  }

  @Test
  void execute_shouldInsertCwes_whenNewCVEs() {
    Cve cve1 = buildCve("1").addCwe("CWE-11").addCwe("CWE-12").build();
    Cve cve2 = buildCve("2").addCwe("CWE-11").build();
    batchReportReader.putCves(List.of(cve1, cve2));

    persistCveStep.execute(new TestComputationStepContext());

    assertCveHasExactlyCwes(cve1, "CWE-11", "CWE-12");
    assertCveHasExactlyCwes(cve2, "CWE-11");
  }

  @Test
  void execute_shouldUpdateExistingCwesAndInsertNewOnes_whenUpdatingCVEs() {
    dbClient.cveDao().insert(dbSession, new CveDto("cve-uuid-1", "CVE-1", "Old description 1", 0.0, 0.0, 0.0, 0L, 0L, 0L, 0L));
    dbClient.cveCweDao().insert(dbSession, new CveCweDto("cve-uuid-1", "CWE-1"));
    dbClient.cveCweDao().insert(dbSession, new CveCweDto("cve-uuid-1", "CWE-2"));
    db.commit();
    Cve cve = buildCve("1").addCwe("CWE-2").addCwe("CWE-3").build();
    batchReportReader.putCves(List.of(cve));

    persistCveStep.execute(new TestComputationStepContext());

    assertCveHasExactlyCwes(cve, "CWE-2", "CWE-3");
  }

  private void assertCveHasExactlyCwes(Cve cve, String... cwes) {
    Set<String> cweInDb = dbClient.cveCweDao().selectByCveUuid(dbSession, getCveUuid(cve.getCveId()));
    assertThat(cweInDb).containsExactlyInAnyOrder(cwes);
  }

  private String getCveUuid(String cveId) {
    return dbClient.cveDao().selectById(dbSession, cveId)
      .map(CveDto::uuid)
      .orElseGet(() -> fail("CVE not found"));
  }

  private static Cve.Builder buildCve(String suffix) {
    return Cve.newBuilder()
      .setCveId("CVE-"+suffix)
      .setCvssScore(7.5F)
      .setEpssScore(0.1F)
      .setEpssPercentile(0.4F)
      .setDescription("Some CVE description "+suffix)
      .setLastModifiedDate(5L)
      .setPublishedDate(4L);
  }
}
