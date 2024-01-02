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
package org.sonar.scanner.externalissue.sarif;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.sarif.Run;
import org.sonar.core.sarif.Sarif210;

import static java.util.Objects.requireNonNull;

@ScannerSide
public class DefaultSarif210Importer implements Sarif210Importer {
  private static final Logger LOG = Loggers.get(DefaultSarif210Importer.class);

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
      List<NewExternalIssue> newExternalIssues = toNewExternalIssues(run);
      if (newExternalIssues == null) {
        failedRuns += 1;
      } else {
        successFullyImportedRuns += 1;
        successFullyImportedIssues += newExternalIssues.size();
        newExternalIssues.forEach(NewExternalIssue::save);
      }
    }
    return SarifImportResults.builder()
      .successFullyImportedIssues(successFullyImportedIssues)
      .successFullyImportedRuns(successFullyImportedRuns)
      .failedRuns(failedRuns)
      .build();
  }

  @CheckForNull
  private List<NewExternalIssue> toNewExternalIssues(Run run) {
    try {
      return runMapper.mapRun(run);
    } catch (Exception exception) {
      LOG.warn("Failed to import a sarif run, error: {}", exception.getMessage());
      return null;
    }
  }

}
