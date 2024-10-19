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
package org.sonar.ce.task.projectanalysis.step;

import org.slf4j.MDC;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport.ContextProperty;

/**
 * Add MDC variable to the logger framework. This MDC variable will be cleared out in the ComputationStepExecutor.
 *
 * @see org.sonar.ce.task.log.CeTaskLogging
 * @see org.sonar.ce.task.step.ComputationStepExecutor
 */
public class CodescanLoggingStep implements ComputationStep {

    private static final String CODESCAN_JOB_ID_SONAR_PARAM = "sonar.analysis.buildId";
    private static final String MDC_CODESCAN_JOB_ID = "jobId";

    private final BatchReportReader reportReader;

    public CodescanLoggingStep(BatchReportReader reportReader) {
        this.reportReader = reportReader;
    }

    @Override
    public void execute(Context context) {
        try (CloseableIterator<ContextProperty> it = reportReader.readContextProperties()) {
            it.forEachRemaining(
                    contextProperty -> {
                        String propertyKey = contextProperty.getKey();
                        if (propertyKey.equals(CODESCAN_JOB_ID_SONAR_PARAM)) {
                            MDC.put(MDC_CODESCAN_JOB_ID, contextProperty.getValue());
                        }
                    });
        }
    }

    @Override
    public String getDescription() {
        return "Add Codescan jobId MDC variable.";
    }
}
