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
package org.sonar.ce.task.projectanalysis.source;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.source.linereader.LineReader;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.sonar.ce.task.projectanalysis.source.linereader.LineReader.Data.HIGHLIGHTING;
import static org.sonar.ce.task.projectanalysis.source.linereader.LineReader.Data.SYMBOLS;

public class FileSourceDataWarnings {
  private static final Comparator<Component> COMPONENT_COMPARATOR = Comparator.comparingInt(t -> t.getReportAttributes().getRef());

  private final CeTaskMessages taskMessages;
  private final System2 system2;
  private final EnumMap<LineReader.Data, Set<Component>> fileErrorsPerData = new EnumMap<>(LineReader.Data.class);
  private boolean closed = false;

  public FileSourceDataWarnings(CeTaskMessages taskMessages, System2 system2) {
    this.taskMessages = taskMessages;
    this.system2 = system2;
  }

  public void addWarning(Component file, LineReader.ReadError readError) {
    checkNotCommitted();
    requireNonNull(file, "file can't be null");
    requireNonNull(readError, "readError can't be null");

    fileErrorsPerData.compute(readError.getData(), (data, existingList) -> {
      Set<Component> res = existingList == null ? new HashSet<>() : existingList;
      res.add(file);
      return res;
    });
  }

  public void commitWarnings() {
    checkNotCommitted();
    this.closed = true;
    createWarning(HIGHLIGHTING, "highlighting");
    createWarning(SYMBOLS, "symbol");
  }

  private void createWarning(LineReader.Data data, String dataWording) {
    Set<Component> filesWithErrors = fileErrorsPerData.get(data);
    if (filesWithErrors == null) {
      return;
    }

    taskMessages.add(new CeTaskMessages.Message(computeMessage(dataWording, filesWithErrors), system2.now()));
  }

  private static String computeMessage(String dataWording, Set<Component> filesWithErrors) {
    if (filesWithErrors.size() == 1) {
      Component file = filesWithErrors.iterator().next();
      return format("Inconsistent %s data detected on file '%s'. " +
        "File source may have been modified while analysis was running.",
        dataWording,
        file.getName());
    }
    String lineHeader = "\n   ° ";
    return format("Inconsistent %s data detected on some files (%s in total). " +
      "File source may have been modified while analysis was running.", dataWording, filesWithErrors.size())
      + filesWithErrors.stream()
        .sorted(COMPONENT_COMPARATOR)
        .limit(5)
        .map(Component::getName)
        .collect(joining(lineHeader, lineHeader, ""));
  }

  private void checkNotCommitted() {
    checkState(!closed, "warnings already commit");
  }

}
