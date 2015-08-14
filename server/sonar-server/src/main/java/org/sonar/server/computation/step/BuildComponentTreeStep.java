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
package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.Date;
import javax.annotation.Nonnull;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentImpl;
import org.sonar.server.computation.component.MutableTreeRootHolder;

/**
 * Populates the {@link MutableTreeRootHolder} and {@link MutableAnalysisMetadataHolder} from the {@link BatchReportReader}
 */
public class BuildComponentTreeStep implements ComputationStep {
  private final BatchReportReader reportReader;
  private final MutableTreeRootHolder treeRootHolder;
  private final MutableAnalysisMetadataHolder analysisMetadataHolder;

  public BuildComponentTreeStep(BatchReportReader reportReader, MutableTreeRootHolder treeRootHolder, MutableAnalysisMetadataHolder analysisMetadataHolder) {
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute() {
    BatchReport.Metadata metadata = reportReader.readMetadata();
    treeRootHolder.setRoot(buildComponentRoot(metadata));
    analysisMetadataHolder.setAnalysisDate(new Date(metadata.getAnalysisDate()));
  }

  private Component buildComponentRoot(BatchReport.Metadata metadata) {
    int rootComponentRef = metadata.getRootComponentRef();
    BatchReport.Component component = reportReader.readComponent(rootComponentRef);
    return new ComponentImpl(component, buildChildren(component));
  }

  private Iterable<Component> buildChildren(BatchReport.Component component) {
    return Iterables.transform(
        component.getChildRefList(),
        new Function<Integer, Component>() {
          @Override
          public Component apply(@Nonnull Integer componentRef) {
            BatchReport.Component component = reportReader.readComponent(componentRef);
            return new ComponentImpl(component, buildChildren(component));
          }
        }
    );
  }

  @Override
  public String getDescription() {
    return "Builds the Component tree";
  }
}
