/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.api.batch.fs.internal.predicates;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.FileSystem.Index;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.slf4j.event.Level.DEBUG;

class AbsolutePathPredicateTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(DEBUG);

  @Test
  void get_shouldReturnEmptyAndLogError_whenPathIsInvalid(@TempDir Path baseDir) {
    String path = "../../some/path/file.txt";
    AbsolutePathPredicate predicate = new AbsolutePathPredicate(path, baseDir);

    Iterable<InputFile> inputFiles = predicate.get(mock(Index.class));

    assertThat(inputFiles).isEmpty();
    assertThat(logTester.logs(DEBUG)).contains(format("Cannot resolve absolute path '%s' as it is not a valid path", path));
  }

}
