/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.scm;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.source.SourceHashRepository;

import static com.google.common.base.Preconditions.checkNotNull;

public class ScmInfoRepositoryImpl implements ScmInfoRepository {

  private static final Logger LOGGER = Loggers.get(ScmInfoRepositoryImpl.class);

  private final BatchReportReader batchReportReader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final SourceHashRepository sourceHashRepository;

  private final Map<Component, ScmInfo> scmInfoCache = new HashMap<>();

  public ScmInfoRepositoryImpl(BatchReportReader batchReportReader, AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, SourceHashRepository sourceHashRepository) {
    this.batchReportReader = batchReportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.sourceHashRepository = sourceHashRepository;
  }

  @Override
  public Optional<ScmInfo> getScmInfo(Component component) {
    checkNotNull(component, "Component cannot be bull");
    initializeScmInfoForComponent(component);
    return Optional.fromNullable(scmInfoCache.get(component));
  }

  private void initializeScmInfoForComponent(Component component) {
    if (scmInfoCache.containsKey(component)) {
      return;
    }
    Optional<ScmInfo> scmInfoOptional = getScmInfoForComponent(component);
    scmInfoCache.put(component, scmInfoOptional.orNull());
  }

  private Optional<ScmInfo> getScmInfoForComponent(Component component) {
    if (component.getType() != Component.Type.FILE) {
      return Optional.absent();
    }

    BatchReport.Changesets changesets = batchReportReader.readChangesets(component.getReportAttributes().getRef());
    if (changesets == null) {
      return getScmInfoFromDb(component);
    }
    return getScmInfoFromReport(component, changesets);
  }

  private Optional<ScmInfo> getScmInfoFromDb(Component file) {
    if (analysisMetadataHolder.isFirstAnalysis()) {
      return Optional.absent();
    }

    LOGGER.trace("Reading SCM info from db for file '{}'", file.getKey());
    DbSession dbSession = dbClient.openSession(false);
    try {
      FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, file.getUuid());
      if (dto == null || !sourceHashRepository.getRawSourceHash(file).equals(dto.getSrcHash())) {
        return Optional.absent();
      }
      return DbScmInfo.create(file, dto.getSourceData().getLinesList());
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static Optional<ScmInfo> getScmInfoFromReport(Component file, BatchReport.Changesets changesets) {
    LOGGER.trace("Reading SCM info from report for file '{}'", file.getKey());
    return Optional.<ScmInfo>of(new ReportScmInfo(changesets));
  }
}
