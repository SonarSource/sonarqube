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

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.QPMeasureData;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.QualityProfile;
import org.sonar.server.computation.task.step.ComputationStep;

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
  public void execute() {
    DbSession dbSession = dbClient.openSession(true);
    try {
      Component root = treeRootHolder.getRoot();
      Metric metric = metricRepository.getByKey(QUALITY_PROFILES_KEY);
      Set<QualityProfile> qualityProfiles = parseQualityProfiles(measureRepository.getRawMeasure(root, metric));
      if (qualityProfiles.isEmpty()) {
        return;
      }

      List<QualityProfileDto> dtos = dbClient.qualityProfileDao().selectByKeys(dbSession, qualityProfiles.stream().map(QualityProfile::getQpKey).collect(Collectors.toList()));
      dtos.addAll(getAncestors(dbSession, dtos));
      long analysisDate = analysisMetadataHolder.getAnalysisDate();
      dtos.forEach(dto -> {
        dto.setLastUsed(analysisDate);
        dbClient.qualityProfileDao().update(dbSession, dto);
      });

      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private List<QualityProfileDto> getAncestors(DbSession dbSession, List<QualityProfileDto> dtos) {
    List<QualityProfileDto> ancestors = new ArrayList<>();
    dtos.forEach(dto -> incrementAncestors(dbSession, dto, ancestors));
    return ancestors;
  }

  private void incrementAncestors(DbSession session, QualityProfileDto profile, List<QualityProfileDto> ancestors) {
    String parentKey = profile.getParentKee();
    if (parentKey != null) {
      QualityProfileDto parentDto = dbClient.qualityProfileDao().selectOrFailByKey(session, parentKey);
      ancestors.add(parentDto);
      incrementAncestors(session, parentDto, ancestors);
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
