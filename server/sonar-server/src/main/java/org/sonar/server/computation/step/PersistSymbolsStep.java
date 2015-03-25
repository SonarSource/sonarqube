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
 * Nothing is persist for the moment. Only Symbols are read and not persist for the moment
 */
public class PersistSymbolsStep implements ComputationStep {

  private static final String OFFSET_SEPARATOR = ",";
  private static final String SYMBOLS_SEPARATOR = ";";

  // Temporary variable in order to be able to test that symbols are well computed. Will only contains data from last processed file
  private Map<Integer, StringBuilder> symbolsByLineForLastProcessedFile;

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[]{Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    int rootComponentRef = context.getReportMetadata().getRootComponentRef();
    recursivelyProcessComponent(context, rootComponentRef);
  }

  private void recursivelyProcessComponent(ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    List<BatchReport.Symbols.Symbol> symbols = reportReader.readComponentSymbols(componentRef);
    processSymbols(component, symbols);

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(context, childRef);
    }
  }

  private void processSymbols(BatchReport.Component component, List<BatchReport.Symbols.Symbol> symbols) {
    symbolsByLineForLastProcessedFile = newHashMap();
    if (!symbols.isEmpty()) {
      int symbolId = 1;
      for (BatchReport.Symbols.Symbol symbol : symbols) {
        processSymbolRange(symbol.getDeclaration(), symbolId);
        for (BatchReport.Range reference : symbol.getReferenceList()) {
          processSymbolRange(reference, symbolId);
        }
        symbolId++;
      }
    }
  }

  private void processSymbolRange(BatchReport.Range range, int symboleId){
    int startLine = range.getStartLine();
    if (startLine != range.getEndLine()) {
      // TODO support symbols on multiple lines when source will be in compute, in order to be able to know the end line in this case
      throw new IllegalStateException("To be implemented : Symbols on multiple lines are not supported for the moment");
    }
    StringBuilder symbolLine = symbolsByLineForLastProcessedFile.get(startLine);
    if (symbolLine == null) {
      symbolLine = new StringBuilder();
      symbolsByLineForLastProcessedFile.put(startLine, symbolLine);
    } else {
      symbolLine.append(SYMBOLS_SEPARATOR);
    }
    symbolLine.append(range.getStartOffset()).append(OFFSET_SEPARATOR);
    symbolLine.append(range.getEndOffset()).append(OFFSET_SEPARATOR);
    symbolLine.append(symboleId);
  }

  @VisibleForTesting
  Map<Integer, StringBuilder> getSymbolsByLine(){
    return symbolsByLineForLastProcessedFile;
  }

  @Override
  public String getDescription() {
    return "Read Symbols";
  }
}
