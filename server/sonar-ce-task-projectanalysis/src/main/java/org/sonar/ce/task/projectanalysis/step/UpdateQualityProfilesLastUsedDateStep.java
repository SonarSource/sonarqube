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

import java.util.Optional;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QPMeasureData;
import org.sonar.server.qualityprofile.QualityProfile;

import static java.util.Collections.emptySet;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES_KEY;

public class UpdateQualityProfilesLastUsedDateStep implements ComputationStep {

  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  public UpdateQualityProfilesLastUsedDateStep(DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder, TreeRootHolder treeRootHolder, MetricRepository metricRepository,
    MeasureRepository measureRepository) {
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    Component root = treeRootHolder.getRoot();
    Metric metric = metricRepository.getByKey(QUALITY_PROFILES_KEY);
    Set<QualityProfile> qualityProfiles = parseQualityProfiles(measureRepository.getRawMeasure(root, metric));
    try (DbSession dbSession = dbClient.openSession(true)) {
      for (QualityProfile qualityProfile : qualityProfiles) {
        updateDate(dbSession, qualityProfile.getQpKey(), analysisMetadataHolder.getAnalysisDate());
      }
      dbSession.commit();
    }
  }

  private void updateDate(DbSession dbSession, String qProfileUuid, long lastUsedDate) {
    // Traverse profiles from bottom to up in order to avoid DB deadlocks between multiple transactions.
    // Example of hierarchy of profiles:
    // A
    // |- B
    //    |- C1
    //    |- C2
    // Transaction #1 updates C1 then B then A
    // Transaction #2 updates C2 then B then A
    // Transaction #3 updates B then A
    // No cross-dependencies are possible.
    QProfileDto dto = dbClient.qualityProfileDao().selectOrFailByUuid(dbSession, qProfileUuid);
    dbClient.qualityProfileDao().updateLastUsedDate(dbSession, dto, lastUsedDate);
    String parentUuid = dto.getParentKee();
    if (parentUuid != null) {
      updateDate(dbSession, parentUuid, lastUsedDate);
    }
  }

  @Override
  public String getDescription() {
    return "Update last usage date of quality profiles";
  }

  private static Set<QualityProfile> parseQualityProfiles(Optional<Measure> measure) {
    if (!measure.isPresent()) {
      return emptySet();
    }

    String data = measure.get().getStringValue();
    return data == null ? emptySet() : QPMeasureData.fromJson(data).getProfiles();
  }
}
