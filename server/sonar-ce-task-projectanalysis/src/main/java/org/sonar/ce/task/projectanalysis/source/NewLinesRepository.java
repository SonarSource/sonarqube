/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
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
  @Nullable
  private final Period period;
  private final Map<Component, Optional<Set<Integer>>> changedLinesCache = new HashMap<>();

  public NewLinesRepository(BatchReportReader reportReader, AnalysisMetadataHolder analysisMetadataHolder, PeriodHolder periodHolder, ScmInfoRepository scmInfoRepository) {
    this.reportReader = reportReader;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.scmInfoRepository = scmInfoRepository;
    this.period = periodHolder.getPeriod();
  }

  public boolean newLinesAvailable(Component component) {
    return getNewLines(component).isPresent();
  }

  public boolean isLineNew(Component component, int line) {
    return getNewLines(component).map(s -> s.contains(line))
      .orElseThrow(() -> new IllegalStateException("No data about new lines available"));
  }

  public Optional<Set<Integer>> getNewLines(Component component) {
    return changedLinesCache.computeIfAbsent(component, this::computeNewLines);
  }

  private Optional<Set<Integer>> computeNewLines(Component component) {
    Optional<Set<Integer>> reportChangedLines = getChangedLinesFromReport(component);
    if (reportChangedLines.isPresent()) {
      return reportChangedLines;
    }
    return generateNewLinesFromScm(component);
  }

  private Optional<Set<Integer>> generateNewLinesFromScm(Component component) {
    if (period == null) {
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
      if (isLineInPeriod(e.getValue().getDate(), period)) {
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

  private Optional<Set<Integer>> getChangedLinesFromReport(Component component) {
    if (analysisMetadataHolder.isPullRequest() || analysisMetadataHolder.isShortLivingBranch()) {
      return reportReader.readComponentChangedLines(component.getReportAttributes().getRef())
        .map(c -> new HashSet<>(c.getLineList()));
    }

    return Optional.empty();
  }
}
