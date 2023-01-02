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
package org.sonar.scm.git;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ChangedLinesComputer {
  private final Tracker tracker = new Tracker();

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

  /**
   * The OutputStream to pass to JGit's diff command.
   */
  OutputStream receiver() {
    return receiver;
  }

  /**
   * From a stream of unified diff lines emitted by Git <strong>for a single file</strong>,
   * compute the line numbers that should be considered changed.
   * Example input:
   * <pre>
   * diff --git a/lao.txt b/lao.txt
   * index 635ef2c..7f050f2 100644
   * --- a/lao.txt
   * +++ b/lao.txt
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
   * @@ -9,3 +8,6 @@ And let there always be being,
   *  The two are the same,
   *  But after they are produced,
   *    they have different names.
   * +They both may be called deep and profound.
   * +Deeper and more profound,
   * +The door of all subtleties!names.
   * </pre>
   * See also: http://www.gnu.org/software/diffutils/manual/html_node/Example-Unified.html#Example-Unified
   */
  Set<Integer> changedLines() {
    return tracker.changedLines();
  }

  private static class Tracker {

    private static final Pattern START_LINE_IN_TARGET = Pattern.compile(" \\+(\\d+)");

    private final Set<Integer> changedLines = new HashSet<>();

    private boolean foundStart = false;
    private int lineNumInTarget;

    private void parseLine(String line) {
      if (line.startsWith("@@ ")) {
        Matcher matcher = START_LINE_IN_TARGET.matcher(line);
        if (!matcher.find()) {
          throw new IllegalStateException("Invalid block header on line " + line);
        }
        foundStart = true;
        lineNumInTarget = Integer.parseInt(matcher.group(1));
      } else if (foundStart) {
        char firstChar = line.charAt(0);
        if (firstChar == ' ') {
          lineNumInTarget++;
        } else if (firstChar == '+') {
          changedLines.add(lineNumInTarget);
          lineNumInTarget++;
        }
      }
    }

    Set<Integer> changedLines() {
      return changedLines;
    }
  }
}
