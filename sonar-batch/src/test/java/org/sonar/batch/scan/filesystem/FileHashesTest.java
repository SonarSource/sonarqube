/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class FileHashesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  RemoteFileHashes remoteFileHashes = mock(RemoteFileHashes.class);

  @Test
  public void hash() throws Exception {
    File file = temp.newFile();
    FileUtils.write(file, "fooo");

    FileHashes hashes = new FileHashes(remoteFileHashes);
    assertThat(hashes.hash(file, Charset.forName("UTF-8"))).isEqualTo("efc4470c96a94b1ff400175ef8368444");
    verifyZeroInteractions(remoteFileHashes);
  }

  @Test
  public void remote_hash() throws Exception {
    String path = "src/main/java/Foo.java";
    when(remoteFileHashes.remoteHash(path)).thenReturn("ABCDE");

    FileHashes hashes = new FileHashes(remoteFileHashes);
    assertThat(hashes.remoteHash(path)).isEqualTo("ABCDE");
  }
}
