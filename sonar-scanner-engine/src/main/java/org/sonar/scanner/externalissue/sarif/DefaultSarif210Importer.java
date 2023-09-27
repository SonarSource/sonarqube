/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.externalissue.sarif;

import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Sarif210;
import org.sonar.scanner.externalissue.sarif.RunMapper.RunMapperResult;

import static java.util.Objects.requireNonNull;

@ScannerSide
public class DefaultSarif210Importer implements Sarif210Importer {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultSarif210Importer.class);

  private final RunMapper runMapper;

  DefaultSarif210Importer(RunMapper runMapper) {
    this.runMapper = runMapper;
  }

  @Override
  public SarifImportResults importSarif(Sarif210 sarif210) {
    int successFullyImportedIssues = 0;
    int successFullyImportedRuns = 0;
    int failedRuns = 0;

    Set<Run> runs = requireNonNull(sarif210.getRuns(), "The runs section of the Sarif report is null");
    for (Run run : runs) {
      RunMapperResult runMapperResult = tryMapRun(run);
      if (runMapperResult.isSuccess()) {
        List<NewAdHocRule> newAdHocRules = runMapperResult.getNewAdHocRules();
        newAdHocRules.forEach(NewAdHocRule::save);

        List<NewExternalIssue> newExternalIssues = runMapperResult.getNewExternalIssues();
        successFullyImportedRuns += 1;
        successFullyImportedIssues += newExternalIssues.size();
        newExternalIssues.forEach(NewExternalIssue::save);
      } else {
        failedRuns += 1;
      }
    }
    return SarifImportResults.builder()
      .successFullyImportedIssues(successFullyImportedIssues)
      .successFullyImportedRuns(successFullyImportedRuns)
      .failedRuns(failedRuns)
      .build();
  }

  private RunMapperResult tryMapRun(Run run) {
    try {
      return runMapper.mapRun(run);
    } catch (Exception exception) {
      LOG.warn("Failed to import a sarif run, error: {}", exception.getMessage());
      return new RunMapperResult().success(false);

    }
  }

}
