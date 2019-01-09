/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS;

/**
 * Persist analysis properties
 * Only properties starting with "sonar.analysis" or "sonar.pullrequest" will be persisted in database
 */
public class PersistAnalysisPropertiesStep implements ComputationStep {

  private static final String SONAR_PULL_REQUEST = "sonar.pullrequest.";
  private static final String SCM_REVISION_ID = "sonar.analysis.scm_revision_id";

  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final BatchReportReader reportReader;
  private final UuidFactory uuidFactory;

  public PersistAnalysisPropertiesStep(DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder,
    BatchReportReader reportReader, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.reportReader = reportReader;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    List<AnalysisPropertyDto> analysisPropertyDtos = new ArrayList<>();
    try (CloseableIterator<ScannerReport.ContextProperty> it = reportReader.readContextProperties()) {
      it.forEachRemaining(
        contextProperty -> {
          String propertyKey = contextProperty.getKey();
          if (propertyKey.startsWith(SONAR_ANALYSIS) || propertyKey.startsWith(SONAR_PULL_REQUEST)) {
            analysisPropertyDtos.add(new AnalysisPropertyDto()
              .setUuid(uuidFactory.create())
              .setKey(propertyKey)
              .setValue(contextProperty.getValue())
              .setSnapshotUuid(analysisMetadataHolder.getUuid()));
          }
        });
    }

    analysisMetadataHolder.getScmRevisionId().ifPresent(scmRevisionId -> analysisPropertyDtos.add(new AnalysisPropertyDto()
      .setUuid(uuidFactory.create())
      .setKey(SCM_REVISION_ID)
      .setValue(scmRevisionId)
      .setSnapshotUuid(analysisMetadataHolder.getUuid())));

    if (analysisPropertyDtos.isEmpty()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.analysisPropertiesDao().insert(dbSession, analysisPropertyDtos);
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Persist analysis properties";
  }
}
