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
package org.sonar.scm.git;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessWrapperFactoryTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private final ProcessWrapperFactory underTest = new ProcessWrapperFactory();

  @Test
  void should_log_error_output_in_debug_mode(@TempDir Path root) {
    logTester.setLevel(Level.DEBUG);
    var processWrapper = underTest.create(root, v -> {
    }, Map.of("LANG", "en_US"), "git", "blame");
    assertThatThrownBy(processWrapper::execute)
      .isInstanceOf(IllegalStateException.class);

    assertThat(logTester.logs(Level.DEBUG).get(0)).startsWith("fatal:");
  }

}
