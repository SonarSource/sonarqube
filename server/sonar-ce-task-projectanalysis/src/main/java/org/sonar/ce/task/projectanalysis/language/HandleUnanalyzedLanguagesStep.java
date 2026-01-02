/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.language;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.sonar.api.utils.System2;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.dismissmessage.MessageType;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static org.sonar.core.language.UnanalyzedLanguages.C;
import static org.sonar.core.language.UnanalyzedLanguages.CPP;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP_KEY;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C_KEY;

/**
 * Check if there are files that could be analyzed with a higher SQ edition.
 */
public class HandleUnanalyzedLanguagesStep implements ComputationStep {

  static final String DESCRIPTION = "Check upgrade possibility for not analyzed code files.";

  private static final String LANGUAGE_UPGRADE_MESSAGE = "%s detected in this project during the last analysis. %s cannot be analyzed with your" +
    " current SonarQube edition. Please consider <a target=\"_blank\" href=\"https://www.sonarsource.com/plans-and-pricing/developer/?referrer=sonarqube-cpp\">upgrading to" +
    " Developer Edition</a> to find Bugs, Code Smells, Vulnerabilities and Security Hotspots in %s.";

  private static final Set<String> C_EXTENSIONS = Set.of("c");
  private static final Set<String> CPP_EXTENSIONS = Set.of("cpp", "cc", "cxx", "c++");

  private final ScannerReportReader reportReader;
  private final CeTaskMessages ceTaskMessages;
  private final PlatformEditionProvider editionProvider;
  private final System2 system;
  private final TreeRootHolder treeRootHolder;
  private final MeasureRepository measureRepository;
  private final Metric unanalyzedCMetric;
  private final Metric unanalyzedCppMetric;

  public HandleUnanalyzedLanguagesStep(ScannerReportReader reportReader, CeTaskMessages ceTaskMessages, PlatformEditionProvider editionProvider,
    System2 system, TreeRootHolder treeRootHolder, MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.reportReader = reportReader;
    this.ceTaskMessages = ceTaskMessages;
    this.editionProvider = editionProvider;
    this.system = system;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.unanalyzedCMetric = metricRepository.getByKey(UNANALYZED_C_KEY);
    this.unanalyzedCppMetric = metricRepository.getByKey(UNANALYZED_CPP_KEY);
  }

  @Override
  public void execute(Context context) {
    editionProvider.get().ifPresent(edition -> {
      if (edition != EditionProvider.Edition.COMMUNITY) {
        return;
      }

      Map<String, Integer> filesPerLanguage = extractUnanalyzedFilesFromIndexedFileCount();

      if (filesPerLanguage.isEmpty()) {
        return;
      }

      ceTaskMessages.add(constructMessage(filesPerLanguage));
      computeMeasures(filesPerLanguage);
    });
  }

  private Map<String, Integer> extractUnanalyzedFilesFromIndexedFileCount() {
    Map<String, Integer> notAnalyzedindexedFileCountPerType = reportReader.readMetadata().getNotAnalyzedIndexedFileCountPerTypeMap();
    Map<String, Integer> filesPerLanguage = new HashMap<>();

    addFileCountForLanguage(filesPerLanguage, notAnalyzedindexedFileCountPerType, C_EXTENSIONS, C.toString());
    addFileCountForLanguage(filesPerLanguage, notAnalyzedindexedFileCountPerType, CPP_EXTENSIONS, CPP.toString());

    return filesPerLanguage;
  }

  private static void addFileCountForLanguage(Map<String, Integer> filesPerLanguage, Map<String, Integer> fileCountPerType, Collection<String> extensions, String languageKey) {
    long count = fileCountPerType.entrySet().stream()
      .filter(entry -> extensions.contains(entry.getKey().toLowerCase(ENGLISH)))
      .mapToLong(Map.Entry::getValue)
      .sum();
    if (count > 0) {
      filesPerLanguage.put(languageKey, (int) count);
    }
  }

  private CeTaskMessages.Message constructMessage(Map<String, Integer> filesPerLanguage) {
    SortedMap<String, Integer> sortedLanguageMap = new TreeMap<>(filesPerLanguage);
    Iterator<Map.Entry<String, Integer>> iterator = sortedLanguageMap.entrySet().iterator();
    Map.Entry<String, Integer> firstLanguage = iterator.next();
    StringBuilder languageLabel = new StringBuilder(firstLanguage.getKey());
    StringBuilder fileCountLabel = new StringBuilder(format("%s unanalyzed %s", firstLanguage.getValue(), firstLanguage.getKey()));
    while (iterator.hasNext()) {
      Map.Entry<String, Integer> nextLanguage = iterator.next();
      if (iterator.hasNext()) {
        languageLabel.append(", ");
        fileCountLabel.append(", ");
      } else {
        languageLabel.append(" and ");
        fileCountLabel.append(" and ");
      }
      languageLabel.append(nextLanguage.getKey());
      fileCountLabel.append(format("%s unanalyzed %s", nextLanguage.getValue(), nextLanguage.getKey()));
    }

    if (sortedLanguageMap.size() == 1 && sortedLanguageMap.entrySet().iterator().next().getValue() == 1) {
      fileCountLabel.append(" file was");
    } else {
      fileCountLabel.append(" files were");
    }

    String message = format(LANGUAGE_UPGRADE_MESSAGE, fileCountLabel, languageLabel, sortedLanguageMap.size() == 1 ? "this file" : "these files");
    return new CeTaskMessages.Message(message, system.now(), MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
  }

  private void computeMeasures(Map<String, Integer> filesPerLanguage) {
    Component project = treeRootHolder.getRoot();
    Integer unanalyzedCFiles = filesPerLanguage.getOrDefault(C.toString(), 0);
    if (unanalyzedCFiles > 0) {
      measureRepository.add(project, unanalyzedCMetric, Measure.newMeasureBuilder().create(unanalyzedCFiles));
    }
    Integer unanalyzedCppFiles = filesPerLanguage.getOrDefault(CPP.toString(), 0);
    if (unanalyzedCppFiles > 0) {
      measureRepository.add(project, unanalyzedCppMetric, Measure.newMeasureBuilder().create(unanalyzedCppFiles));
    }
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}
