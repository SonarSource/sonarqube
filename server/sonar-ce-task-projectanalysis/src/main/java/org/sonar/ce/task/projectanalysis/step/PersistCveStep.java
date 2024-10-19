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

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dependency.CveCweDto;
import org.sonar.db.dependency.CveDto;
import org.sonar.scanner.protocol.output.ScannerReport;

/**
 * Step that persists CVEs and their CWEs in the database.
 * CVEs are inserted or updated in the database based on the information from the scanner report.
 * If CWEs need to be updated, we simply remove all CWEs from the CVE and insert what is sent by the scanner.
 */
public class PersistCveStep implements ComputationStep {

  private static final Logger LOG = LoggerFactory.getLogger(PersistCveStep.class);

  private final BatchReportReader batchReportReader;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system2;

  public PersistCveStep(BatchReportReader batchReportReader, DbClient dbClient, UuidFactory uuidFactory, System2 system2) {
    this.batchReportReader = batchReportReader;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  public String getDescription() {
    return "Persist CVEs";
  }

  @Override
  public void execute(Context context) {
    int count = 0;
    try (DbSession dbSession = dbClient.openSession(false);
         CloseableIterator<ScannerReport.Cve> batchCves = batchReportReader.readCves()) {
      while (batchCves.hasNext()) {
        updateOrInsertCve(dbSession, batchCves.next());
        count++;
      }
      LOG.debug("{} CVEs were imported/updated", count);
      dbSession.commit();
    } catch (Exception exception) {
      throw new IllegalStateException(String.format("CVEs import failed after processing %d CVEs successfully", count), exception);
    }
  }

  private void updateOrInsertCve(DbSession dbSession, ScannerReport.Cve scannerCve) {
    dbClient.cveDao().selectById(dbSession, scannerCve.getCveId())
      .ifPresentOrElse(
        cveDto -> updateCve(dbSession, cveDto, scannerCve),
        () -> insertCve(dbSession, scannerCve));
  }

  private void updateCve(DbSession dbSession, CveDto cveInDb, ScannerReport.Cve scannerCve) {
    CveDto dtoForUpdate = toDtoForUpdate(scannerCve, cveInDb);
    dbClient.cveDao().update(dbSession, dtoForUpdate);
    String cveUuid = cveInDb.uuid();
    deleteThenInsertCwesIfUpdated(dbSession, scannerCve, cveUuid);
  }

  private CveDto toDtoForUpdate(ScannerReport.Cve cve, CveDto cveInDb) {
    return new CveDto(
      cveInDb.uuid(),
      cve.getCveId(),
      cve.getDescription(),
      cve.getCvssScore(),
      cve.getEpssScore(),
      cve.getEpssPercentile(),
      cve.getPublishedDate(),
      cve.getLastModifiedDate(),
      cveInDb.createdAt(),
      system2.now()
    );
  }

  private void deleteThenInsertCwesIfUpdated(DbSession dbSession, ScannerReport.Cve scannerCve, String cveUuid) {
    Set<String> cweInDb = dbClient.cveCweDao().selectByCveUuid(dbSession, cveUuid);
    Set<String> cweFromReport = new HashSet<>(scannerCve.getCweList());
    if (!cweInDb.equals(cweFromReport)) {
      dbClient.cveCweDao().deleteByCveUuid(dbSession, cveUuid);
      cweFromReport.forEach(cwe -> dbClient.cveCweDao().insert(dbSession, new CveCweDto(cveUuid, cwe)));
    }
  }

  private void insertCve(DbSession dbSession, ScannerReport.Cve scannerCve) {
    CveDto dtoForInsert = toDtoForInsert(scannerCve);
    dbClient.cveDao().insert(dbSession, dtoForInsert);
    scannerCve.getCweList().forEach(cwe -> dbClient.cveCweDao().insert(dbSession, new CveCweDto(dtoForInsert.uuid(), cwe)));
  }

  private CveDto toDtoForInsert(ScannerReport.Cve cve) {
    long now = system2.now();
    return new CveDto(
      uuidFactory.create(),
      cve.getCveId(),
      cve.getDescription(),
      cve.getCvssScore(),
      cve.getEpssScore(),
      cve.getEpssPercentile(),
      cve.getPublishedDate(),
      cve.getLastModifiedDate(),
      now,
      now
    );
  }

}
