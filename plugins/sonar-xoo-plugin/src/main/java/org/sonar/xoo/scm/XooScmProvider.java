/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.xoo.scm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.api.batch.scm.ScmProvider;

import java.io.File;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class XooScmProvider extends ScmProvider {
  private static final Logger LOG = Loggers.get(XooScmProvider.class);
  private static final String SCM_EXTENSION = ".scm";

  private final XooBlameCommand blame;
  private XooIgnoreCommand ignore;

  public XooScmProvider(XooBlameCommand blame,  XooIgnoreCommand ignore) {
    this.blame = blame;
    this.ignore = ignore;
  }

  @Override
  public boolean supports(File baseDir) {
    return new File(baseDir, ".xoo").exists();
  }

  @Override
  public String key() {
    return "xoo";
  }

  @Override
  public BlameCommand blameCommand() {
    return blame;
  }

  @Override
  public IgnoreCommand ignoreCommand() {
    return ignore;
  }

  @Override
  public String revisionId(Path path) {
    return "fakeSha1FromXoo";
  }

  @CheckForNull
  public Set<Path> branchChangedFiles(String targetBranchName, Path rootBaseDir) {
    Set<Path> changedFiles = new HashSet<>();
    Set<Path> scmFiles = new HashSet<>();

    try {
      Files.walkFileTree(rootBaseDir, new SimpleFileVisitor<Path>() {
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (file.getFileName().toString().endsWith(".scm")) {
            scmFiles.add(file);
          }
          return FileVisitResult.CONTINUE;
        }
      });

      if (scmFiles.isEmpty()) {
        return null;
      }

      for (Path scmFilePath : scmFiles) {
        String fileName = scmFilePath.getFileName().toString();
        Path filePath = scmFilePath.resolveSibling(fileName.substring(0, fileName.length() - 4));
        if (!Files.exists(filePath)) {
          continue;
        }
        Set<Integer> newLines = loadNewLines(scmFilePath);
        if (newLines == null) {
          return null;
        }
        if (!newLines.isEmpty()) {
          changedFiles.add(filePath);
        }
      }

    } catch (IOException e) {
      throw new IllegalStateException("Failed to find scm files", e);
    }

    return changedFiles;
  }

  @CheckForNull
  public Map<Path, Set<Integer>> branchChangedLines(String targetBranchName, Path rootBaseDir, Set<Path> files) {
    Map<Path, Set<Integer>> map = new HashMap<>();
    for (Path filePath : files) {
      Path scmFilePath = filePath.resolveSibling(filePath.getFileName() + SCM_EXTENSION);
      try {
        Set<Integer> newLines = loadNewLines(scmFilePath);
        if (newLines != null && !newLines.isEmpty()) {
          map.put(filePath, newLines);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to parse scm file", e);
      }

    }
    return map.isEmpty() ? null : map;
  }

  // return null when any of the scm files lack the is-new flag (column 4)
  @CheckForNull
  private Set<Integer> loadNewLines(Path filePath) throws IOException {
    if (!Files.isRegularFile(filePath)) {
      return Collections.emptySet();
    }
    LOG.debug("Processing " + filePath.toAbsolutePath());
    Set<Integer> newLines = new HashSet<>();
    List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
    int lineNum = 0;
    boolean foundNewLineFlag = false;
    for (String line : lines) {
      lineNum++;
      String[] fields = StringUtils.splitPreserveAllTokens(line, ',');
      if (fields.length < 4) {
        continue;
      }
      foundNewLineFlag = true;

      if (Boolean.parseBoolean(fields[3])) {
        newLines.add(lineNum);
      }
    }
    return foundNewLineFlag ? newLines : null;
  }
}
