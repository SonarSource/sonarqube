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
package org.sonar.scanner.mediumtest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component;
import org.sonar.scanner.protocol.output.ScannerReport.Symbol;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.report.ScannerReportUtils;
import org.sonar.scanner.scan.SpringProjectScanContainer;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

public class AnalysisResult implements AnalysisObserver {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisResult.class);

  private final Map<String, InputFile> inputFilesByKeys = new HashMap<>();
  private InputProject project;
  private ScannerReportReader reader;

  @Override
  public void analysisCompleted(SpringProjectScanContainer container) {
    LOG.info("Store analysis results in memory for later assertions in medium test");
    FileStructure fileStructure = container.getComponentByType(FileStructure.class);
    reader = new ScannerReportReader(fileStructure);
    project = container.getComponentByType(InputProject.class);

    storeFs(container);

  }

  public ScannerReportReader getReportReader() {
    return reader;
  }

  private void storeFs(SpringProjectScanContainer container) {
    InputComponentStore inputFileCache = container.getComponentByType(InputComponentStore.class);
    for (InputFile inputPath : inputFileCache.inputFiles()) {
      inputFilesByKeys.put(((DefaultInputFile) inputPath).getProjectRelativePath(), inputPath);
    }
  }

  public Component getReportComponent(InputComponent inputComponent) {
    return getReportReader().readComponent(((DefaultInputComponent) inputComponent).scannerId());
  }

  public Component getReportComponent(int scannerId) {
    return getReportReader().readComponent(scannerId);
  }

  public List<ScannerReport.Issue> issuesFor(InputComponent inputComponent) {
    return readFromReport(inputComponent, ScannerReportReader::readComponentIssues);
  }

  public List<ScannerReport.ExternalIssue> externalIssuesFor(InputComponent inputComponent) {
    return readFromReport(inputComponent, ScannerReportReader::readComponentExternalIssues);
  }

  public List<ScannerReport.Issue> issuesFor(Component reportComponent) {
    return readFromReport(reportComponent, ScannerReportReader::readComponentIssues);
  }

  public InputProject project() {
    return project;
  }

  public Collection<InputFile> inputFiles() {
    return inputFilesByKeys.values();
  }

  @CheckForNull
  public InputFile inputFile(String relativePath) {
    return inputFilesByKeys.get(relativePath);
  }

  public Map<String, List<ScannerReport.Measure>> allMeasures() {
    Map<String, List<ScannerReport.Measure>> result = new HashMap<>();
    result.put(project.key(), readFromReport(project, ScannerReportReader::readComponentMeasures));
    for (InputFile inputFile : inputFilesByKeys.values()) {
      result.put(inputFile.key(), readFromReport(inputFile, ScannerReportReader::readComponentMeasures));
    }
    return result;
  }

  /**
   * Get highlighting types at a given position in an inputfile
   *
   * @param lineOffset 0-based offset in file
   */
  public List<TypeOfText> highlightingTypeFor(InputFile file, int line, int lineOffset) {
    int ref = ((DefaultInputComponent) file).scannerId();
    if (!reader.hasSyntaxHighlighting(ref)) {
      return Collections.emptyList();
    }
    TextPointer pointer = file.newPointer(line, lineOffset);
    List<TypeOfText> result = new ArrayList<>();
    try (CloseableIterator<ScannerReport.SyntaxHighlightingRule> it = reader.readComponentSyntaxHighlighting(ref)) {
      while (it.hasNext()) {
        ScannerReport.SyntaxHighlightingRule rule = it.next();
        TextRange ruleRange = toRange(file, rule.getRange());
        if (ruleRange.start().compareTo(pointer) <= 0 && ruleRange.end().compareTo(pointer) > 0) {
          result.add(ScannerReportUtils.toBatchType(rule.getType()));
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read syntax highlighting for " + file, e);
    }
    return result;
  }

  private static TextRange toRange(InputFile file, ScannerReport.TextRange reportRange) {
    return file.newRange(file.newPointer(reportRange.getStartLine(), reportRange.getStartOffset()), file.newPointer(reportRange.getEndLine(), reportRange.getEndOffset()));
  }

  /**
   * Get list of all start positions of a symbol in an inputfile
   *
   * @param symbolStartLine       0-based start offset for the symbol in file
   * @param symbolStartLineOffset 0-based end offset for the symbol in file
   */
  @CheckForNull
  public List<ScannerReport.TextRange> symbolReferencesFor(InputFile file, int symbolStartLine, int symbolStartLineOffset) {
    int ref = ((DefaultInputComponent) file).scannerId();
    try (CloseableIterator<Symbol> symbols = getReportReader().readComponentSymbols(ref)) {
      while (symbols.hasNext()) {
        Symbol symbol = symbols.next();
        if (symbol.getDeclaration().getStartLine() == symbolStartLine && symbol.getDeclaration().getStartOffset() == symbolStartLineOffset) {
          return symbol.getReferenceList();
        }
      }
    }
    return Collections.emptyList();
  }

  public List<ScannerReport.Duplication> duplicationsFor(InputFile file) {
    return readFromReport(file, ScannerReportReader::readComponentDuplications);
  }

  public List<ScannerReport.CpdTextBlock> duplicationBlocksFor(InputFile file) {
    return readFromReport(file, ScannerReportReader::readCpdTextBlocks);
  }

  @CheckForNull
  public ScannerReport.LineCoverage coverageFor(InputFile file, int line) {
    int ref = ((DefaultInputComponent) file).scannerId();
    try (CloseableIterator<ScannerReport.LineCoverage> it = getReportReader().readComponentCoverage(ref)) {
      while (it.hasNext()) {
        ScannerReport.LineCoverage coverage = it.next();
        if (coverage.getLine() == line) {
          return coverage;
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }

  public List<ScannerReport.AdHocRule> adHocRules() {
    return readFromReport(ScannerReportReader::readAdHocRules);
  }

  public List<ScannerReport.Dependency> dependencies() {
    return readFromReport(ScannerReportReader::readDependencies);
  }

  @NotNull
  private <G> List<G> readFromReport(InputComponent component, BiFunction<ScannerReportReader, Integer, CloseableIterator<G>> readerMethod) {
    int ref = ((DefaultInputComponent) component).scannerId();
    return readFromReport(r -> readerMethod.apply(r, ref));
  }

  @NotNull
  private <G> List<G> readFromReport(Component component, BiFunction<ScannerReportReader, Integer, CloseableIterator<G>> readerMethod) {
    int ref = component.getRef();
    return readFromReport(r -> readerMethod.apply(r, ref));
  }

  @NotNull
  private <G> List<G> readFromReport(Function<ScannerReportReader, CloseableIterator<G>> readerMethod) {
    List<G> result = new ArrayList<>();
    try (CloseableIterator<G> it = readerMethod.apply(getReportReader())) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }
}
