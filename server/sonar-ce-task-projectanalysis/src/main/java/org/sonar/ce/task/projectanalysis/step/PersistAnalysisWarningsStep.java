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
import java.util.Collection;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

/**
 * Propagate analysis warnings from scanner report.
 */
public class PersistAnalysisWarningsStep implements ComputationStep {

  static final String DESCRIPTION = "Propagate analysis warnings from scanner report";

  private final BatchReportReader reportReader;
  private final CeTaskMessages ceTaskMessages;

  public PersistAnalysisWarningsStep(BatchReportReader reportReader, CeTaskMessages ceTaskMessages) {
    this.reportReader = reportReader;
    this.ceTaskMessages = ceTaskMessages;
  }

  @Override
  public void execute(Context context) {
    Collection<CeTaskMessages.Message> warnings = new ArrayList<>();
    try (CloseableIterator<ScannerReport.AnalysisWarning> it = reportReader.readAnalysisWarnings()) {
      it.forEachRemaining(w -> warnings.add(new CeTaskMessages.Message(w.getText(), w.getTimestamp())));
    }
    if (!warnings.isEmpty()) {
      ceTaskMessages.addAll(warnings);
    }
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}
