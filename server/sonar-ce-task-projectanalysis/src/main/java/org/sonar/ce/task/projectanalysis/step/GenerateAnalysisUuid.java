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

import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.UuidFactory;

public class GenerateAnalysisUuid implements ComputationStep {

  private final UuidFactory uuidFactory;
  private final MutableAnalysisMetadataHolder analysisMetadataHolder;

  public GenerateAnalysisUuid(UuidFactory uuidFactory, MutableAnalysisMetadataHolder analysisMetadataHolder) {
    this.uuidFactory = uuidFactory;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    analysisMetadataHolder.setUuid(uuidFactory.create());
  }

  @Override
  public String getDescription() {
    return "Generate analysis UUID";
  }
}
