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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.server.computation.ComputationContext;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Nothing is persist for the moment. Only Syntax Highlighting are read and not persist for the moment
 */
public class PersistSyntaxHighLightingStep implements ComputationStep {

  private static final String OFFSET_SEPARATOR = ",";

  // Temporary variable in order to be able to test that syntax highlighting are well computed. Will only contains data from last processed
  // file
  private Map<Integer, StringBuilder> syntaxHighlightingByLineForLastProcessedFile;

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    recursivelyProcessComponent(context, rootComponentRef);
  }

  private void recursivelyProcessComponent(ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    List<BatchReport.SyntaxHighlighting.HighlightingRule> highlightingRules = reportReader.readComponentSyntaxHighlighting(componentRef);
    processSyntaxHightlighting(component, highlightingRules);

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, childRef);
    }
  }

  private void processSyntaxHightlighting(BatchReport.Component component, List<BatchReport.SyntaxHighlighting.HighlightingRule> highlightingRules) {
    syntaxHighlightingByLineForLastProcessedFile = newHashMap();
    if (!highlightingRules.isEmpty()) {
      for (BatchReport.SyntaxHighlighting.HighlightingRule highlightingRule : highlightingRules) {
        processHighlightingRule(highlightingRule);
      }
    }
  }

  private void processHighlightingRule(BatchReport.SyntaxHighlighting.HighlightingRule highlightingRule) {
    BatchReport.Range range = highlightingRule.getRange();
    int startLine = range.getStartLine();
    int endLine = range.getEndLine();
    if (startLine != endLine) {
      // TODO support syntax highlighting on multiple lines when source will be in compute, in order to be able to know the end line in this case
      throw new IllegalStateException("To be implemented : Syntax Highlighting on multiple lines are not supported for the moment");
    }
    StringBuilder symbolLine = syntaxHighlightingByLineForLastProcessedFile.get(startLine);
    if (symbolLine == null) {
      symbolLine = new StringBuilder();
      syntaxHighlightingByLineForLastProcessedFile.put(startLine, symbolLine);
    }
    symbolLine.append(range.getStartOffset()).append(OFFSET_SEPARATOR);
    symbolLine.append(range.getEndOffset()).append(OFFSET_SEPARATOR);
    symbolLine.append(highlightingRule.getType().toString());
  }

  @VisibleForTesting
  Map<Integer, StringBuilder> getSyntaxHighlightingByLine() {
    return syntaxHighlightingByLineForLastProcessedFile;
  }

  @Override
  public String getDescription() {
    return "Read Syntax Highlighting";
  }
}
