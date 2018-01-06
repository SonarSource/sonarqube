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
package org.sonar.scanner.storage;

import java.io.File;
import org.junit.Test;
import org.sonar.scanner.index.AbstractCachesTest;

import static org.assertj.core.api.Assertions.assertThat;

public class StoragesManagerTest extends AbstractCachesTest {
  @Test
  public void should_stop_and_clean_temp_dir() {
    File tempDir = cachesManager.tempDir();
    assertThat(tempDir).isDirectory().exists();
    assertThat(cachesManager.persistit()).isNotNull();
    assertThat(cachesManager.persistit().isInitialized()).isTrue();

    cachesManager.stop();

    assertThat(tempDir).doesNotExist();
    assertThat(cachesManager.tempDir()).isNull();
    assertThat(cachesManager.persistit()).isNull();
  }
}
