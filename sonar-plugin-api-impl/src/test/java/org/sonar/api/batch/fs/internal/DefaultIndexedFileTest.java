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
package org.sonar.api.batch.fs.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DefaultIndexedFileTest {
  @Test
  public void fail_to_index_if_file_key_too_long() {
    String path = StringUtils.repeat("a", 395);
    String projectKey = "12345";
    Path baseDir = Paths.get("");
    Assertions.assertThatThrownBy(() -> new DefaultIndexedFile(projectKey, baseDir, path, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageEndingWith("length (401) is longer than the maximum authorized (400)");
  }

  @Test
  public void sanitize_shouldThrow_whenRelativePathIsInvalid() {
    String invalidPath = "./../foo/bar";
    Assertions.assertThatThrownBy(() -> DefaultIndexedFile.checkSanitize(invalidPath))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(invalidPath);
  }

  @Test
  public void uri_should_be_cached() {
    String projectKey = "12345";
    Path baseDir = Paths.get("");
    String path = "foo/bar";

    DefaultIndexedFile file = new DefaultIndexedFile(projectKey, baseDir, path, null);
    Assertions.assertThat(file.uri()).isSameAs(file.uri());
  }
}
