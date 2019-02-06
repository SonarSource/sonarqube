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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;

public class NewLinesRepository {
  private final BatchReportReader reportReader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final ScmInfoRepository scmInfoRepository;
  private final PeriodHolder periodHolder;
  private final Map<Component, Optional<Set<Integer>>> reportChangedLinesCache = new HashMap<>();

  public NewLinesRepository(BatchReportReader reportReader, AnalysisMetadataHolder analysisMetadataHolder, PeriodHolder periodHolder, ScmInfoRepository scmInfoRepository) {
    this.reportReader = reportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.scmInfoRepository = scmInfoRepository;
    this.periodHolder = periodHolder;
  }

  public boolean newLinesAvailable() {
    return analysisMetadataHolder.isSLBorPR() || periodHolder.hasPeriod();
  }

  public Optional<Set<Integer>> getNewLines(Component file) {
    Preconditions.checkArgument(file.getType() == Component.Type.FILE, "Changed lines are only available on files, but was: " + file.getType().name());
    Optional<Set<Integer>> reportChangedLines = getChangedLinesFromReport(file);
    if (reportChangedLines.isPresent()) {
      return reportChangedLines;
    }
    return computeNewLinesFromScm(file);
  }

  /**
   * If the changed lines are not in the report or if we are not analyzing a short lived branch (or P/R) we fall back to this method.
   * If there is a period and SCM information, we compare the change dates of each line with the start of the period to figure out
   * if a line is new or not.
   */
  private Optional<Set<Integer>> computeNewLinesFromScm(Component component) {
    if (!periodHolder.hasPeriod()) {
      return Optional.empty();
    }

    Optional<ScmInfo> scmInfoOpt = scmInfoRepository.getScmInfo(component);
    if (!scmInfoOpt.isPresent()) {
      return Optional.empty();
    }

    ScmInfo scmInfo = scmInfoOpt.get();
    Map<Integer, Changeset> allChangesets = scmInfo.getAllChangesets();
    Set<Integer> lines = new HashSet<>();

    for (Map.Entry<Integer, Changeset> e : allChangesets.entrySet()) {
      if (isLineInPeriod(e.getValue().getDate(), periodHolder.getPeriod())) {
        lines.add(e.getKey());
      }
    }
    return Optional.of(lines);
  }

  /**
   * A line belongs to a Period if its date is older than the SNAPSHOT's date of the period.
   */
  private static boolean isLineInPeriod(long lineDate, Period period) {
    return lineDate > period.getSnapshotDate();
  }

  private Optional<Set<Integer>> getChangedLinesFromReport(Component file) {
    if (analysisMetadataHolder.isSLBorPR()) {
      return reportChangedLinesCache.computeIfAbsent(file, this::readFromReport);
    }

    return Optional.empty();
  }

  private Optional<Set<Integer>> readFromReport(Component file) {
    return reportReader.readComponentChangedLines(file.getReportAttributes().getRef())
      .map(c -> new HashSet<>(c.getLineList()));
  }
}
