/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class PersistScannerContextStepTest {
  private static final String ANALYSIS_UUID = "UUID";

  @ClassRule
  public static final DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setUuid(ANALYSIS_UUID);

  private DbClient dbClient = dbTester.getDbClient();
  private PersistScannerContextStep underTest = new PersistScannerContextStep(reportReader, analysisMetadataHolder, dbClient);

  @Test
  public void getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Persist scanner context");
  }

  @Test
  public void log_scanner_logs() {
    reportReader.setScannerLogs(asList("log1", "log2"));

    underTest.execute();

    assertThat(dbClient.scannerContextDao().selectScannerContext(dbTester.getSession(), ANALYSIS_UUID))
      .contains("log1" + '\n' + "log2");
  }

}
