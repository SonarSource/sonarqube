/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.svn;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ChangedLinesComputer {

  private final Tracker tracker;

  private final OutputStream receiver = new OutputStream() {
    StringBuilder sb = new StringBuilder();

    @Override
    public void write(int b) {
      sb.append((char) b);
      if (b == '\n') {
        tracker.parseLine(sb.toString());
        sb.setLength(0);
      }
    }
  };

  ChangedLinesComputer(Path rootBaseDir, Set<Path> included) {
    this.tracker = new Tracker(rootBaseDir, included);
  }

  /**
   * The OutputStream to pass to svnkit's diff command.
   */
  OutputStream receiver() {
    return receiver;
  }

  /**
   * From a stream of svn-style unified diff lines,
   * compute the line numbers that should be considered changed.
   *
   * Example input:
   * <pre>
   * Index: path/to/file
   * ===================================================================
   * --- lao 2002-02-21 23:30:39.942229878 -0800
   * +++ tzu 2002-02-21 23:30:50.442260588 -0800
   * @@ -1,7 +1,6 @@
   * -The Way that can be told of is not the eternal Way;
   * -The name that can be named is not the eternal name.
   *  The Nameless is the origin of Heaven and Earth;
   * -The Named is the mother of all things.
   * +The named is the mother of all things.
   * +
   *  Therefore let there always be non-being,
   *    so we may see their subtlety,
   *  And let there always be being,
   * @@ -9,3 +8,6 @@
   *  The two are the same,
   *  But after they are produced,
   *    they have different names.
   * +They both may be called deep and profound.
   * +Deeper and more profound,
   * +The door of all subtleties!
   * </pre>
   *
   * See also: http://www.gnu.org/software/diffutils/manual/html_node/Example-Unified.html#Example-Unified
   */
  Map<Path, Set<Integer>> changedLines() {
    return tracker.changedLines();
  }

  private static class Tracker {

    private static final Pattern START_LINE_IN_TARGET = Pattern.compile(" \\+(\\d+)");
    private static final String ENTRY_START_PREFIX = "Index: ";

    private final Map<Path, Set<Integer>> changedLines = new HashMap<>();
    private final Set<Path> included;
    private final Path rootBaseDir;

    private int lineNumInTarget;
    private Path currentPath = null;
    private int skipCount = 0;

    Tracker(Path rootBaseDir, Set<Path> included) {
      this.rootBaseDir = rootBaseDir;
      this.included = included;
    }

    private void parseLine(String line) {
      if (line.startsWith(ENTRY_START_PREFIX)) {
        currentPath = Paths.get(line.substring(ENTRY_START_PREFIX.length()).trim());
        if (!currentPath.isAbsolute()) {
          currentPath = rootBaseDir.resolve(currentPath);
        }
        if (!included.contains(currentPath)) {
          return;
        }
        skipCount = 3;
        return;
      }

      if (!included.contains(currentPath)) {
        return;
      }

      if (skipCount > 0) {
        skipCount--;
        return;
      }

      if (line.startsWith("@@ ")) {
        Matcher matcher = START_LINE_IN_TARGET.matcher(line);
        if (!matcher.find()) {
          throw new IllegalStateException("Invalid block header: " + line);
        }
        lineNumInTarget = Integer.parseInt(matcher.group(1));
        return;
      }

      parseContent(line);
    }

    private void parseContent(String line) {
      char firstChar = line.charAt(0);
      if (firstChar == ' ') {
        lineNumInTarget++;
      } else if (firstChar == '+') {
        changedLines
          .computeIfAbsent(currentPath, path -> new HashSet<>())
          .add(lineNumInTarget);
        lineNumInTarget++;
      }
    }

    Map<Path, Set<Integer>> changedLines() {
      return changedLines;
    }
  }
}
