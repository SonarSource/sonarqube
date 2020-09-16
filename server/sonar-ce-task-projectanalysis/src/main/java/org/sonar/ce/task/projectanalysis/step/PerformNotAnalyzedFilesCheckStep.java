/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Check if there are files that could be analyzed with a higher SQ edition.
 */
public class PerformNotAnalyzedFilesCheckStep implements ComputationStep {
  static final String DESCRIPTION = "Check upgrade possibility for not analyzed code files.";

  private static final String LANGUAGE_UPGRADE_MESSAGE = "%s file(s) detected during the last analysis. %s code cannot be analyzed with SonarQube " +
    "community edition. Please consider <a href=\"https://www.sonarqube.org/trial-request/developer-edition/?referrer=sonarqube-cpp\">upgrading to " +
    "the Developer Edition</a> to analyze this language.";

  private final BatchReportReader reportReader;
  private final CeTaskMessages ceTaskMessages;
  private final PlatformEditionProvider editionProvider;
  private final System2 system;

  public PerformNotAnalyzedFilesCheckStep(BatchReportReader reportReader, CeTaskMessages ceTaskMessages, PlatformEditionProvider editionProvider,
    System2 system) {
    this.reportReader = reportReader;
    this.ceTaskMessages = ceTaskMessages;
    this.editionProvider = editionProvider;
    this.system = system;
  }

  @Override
  public void execute(Context context) {
    editionProvider.get().ifPresent(edition -> {
      if (!edition.equals(EditionProvider.Edition.COMMUNITY)) {
        return;
      }

      Map<String, Integer> filesPerLanguage = reportReader.readMetadata().getNotAnalyzedFilesByLanguageMap()
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 0)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      if (filesPerLanguage.isEmpty()) {
        return;
      }

      ceTaskMessages.add(constructMessage(filesPerLanguage));
    });
  }

  private CeTaskMessages.Message constructMessage(Map<String, Integer> filesPerLanguage) {
    checkNotNull(filesPerLanguage);
    checkArgument(filesPerLanguage.size() > 0);

    SortedMap<String, Integer> sortedLanguageMap = new TreeMap<>(filesPerLanguage);
    Iterator<Map.Entry<String, Integer>> iterator = sortedLanguageMap.entrySet().iterator();
    Map.Entry<String, Integer> firstLanguage = iterator.next();
    StringBuilder languageLabel = new StringBuilder(firstLanguage.getKey());
    StringBuilder fileCountLabel = new StringBuilder(format("%s %s", firstLanguage.getValue(), firstLanguage.getKey()));
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
      fileCountLabel.append(format("%s %s", nextLanguage.getValue(), nextLanguage.getKey()));
    }

    return new CeTaskMessages.Message(format(LANGUAGE_UPGRADE_MESSAGE, fileCountLabel, languageLabel), system.now(), true);
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }
}
