/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmChangedFilesTest {
  private ScmChangedFiles scmChangedFiles;

  @Test
  public void testGetter() {
    Collection<Path> files = Collections.singletonList(Paths.get("files"));
    scmChangedFiles = new ScmChangedFiles(files);
    assertThat(scmChangedFiles.get()).containsOnly(Paths.get("files"));
  }

  @Test
  public void testNullable() {
    scmChangedFiles = new ScmChangedFiles(null);
    assertThat(scmChangedFiles.get()).isNull();
    assertThat(scmChangedFiles.verifyChanged(Paths.get("files2"))).isTrue();
  }

  @Test
  public void testConfirm() {
    Collection<Path> files = Collections.singletonList(Paths.get("files"));
    scmChangedFiles = new ScmChangedFiles(files);
    assertThat(scmChangedFiles.verifyChanged(Paths.get("files"))).isTrue();
    assertThat(scmChangedFiles.verifyChanged(Paths.get("files2"))).isFalse();
  }
}
