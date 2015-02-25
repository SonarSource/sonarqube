/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.issue.tracking;

import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.File;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceHashHolderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  SourceHashHolder sourceHashHolder;

  ServerLineHashesLoader lastSnapshots;
  DefaultInputFile file;

  private File ioFile;

  @Before
  public void setUp() throws Exception {
    lastSnapshots = mock(ServerLineHashesLoader.class);
    file = mock(DefaultInputFile.class);
    ioFile = temp.newFile();
    when(file.file()).thenReturn(ioFile);
    when(file.path()).thenReturn(ioFile.toPath());
    when(file.lines()).thenReturn(1);
    when(file.charset()).thenReturn(Charsets.UTF_8);

    sourceHashHolder = new SourceHashHolder(file, lastSnapshots);
  }

  @Test
  public void should_lazy_load_line_hashes() throws Exception {
    final String source = "source";
    FileUtils.write(ioFile, source + "\n", Charsets.UTF_8);
    when(file.lines()).thenReturn(2);

    assertThat(sourceHashHolder.getHashedSource().getHash(1)).isEqualTo(md5Hex(source));
    assertThat(sourceHashHolder.getHashedSource().getHash(2)).isEqualTo("");
    verify(file).key();
    verify(file).status();

    assertThat(sourceHashHolder.getHashedSource().getHash(1)).isEqualTo(md5Hex(source));
  }

  @Test
  public void should_lazy_load_reference_hashes_when_status_changed() throws Exception {
    final String source = "source";
    String key = "foo:src/Foo.java";
    FileUtils.write(ioFile, source, Charsets.UTF_8);
    when(file.key()).thenReturn(key);
    when(file.status()).thenReturn(InputFile.Status.CHANGED);
    when(lastSnapshots.getLineHashes(key)).thenReturn(new String[] {md5Hex(source)});

    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    verify(lastSnapshots).getLineHashes(key);

    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

  @Test
  public void should_not_load_reference_hashes_when_status_same() throws Exception {
    final String source = "source";
    String key = "foo:src/Foo.java";
    FileUtils.write(ioFile, source, Charsets.UTF_8);
    when(file.key()).thenReturn(key);
    when(file.status()).thenReturn(InputFile.Status.SAME);

    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

  @Test
  public void no_reference_hashes_when_status_added() throws Exception {
    final String source = "source";
    String key = "foo:src/Foo.java";
    FileUtils.write(ioFile, source, Charsets.UTF_8);
    when(file.key()).thenReturn(key);
    when(file.status()).thenReturn(InputFile.Status.ADDED);

    assertThat(sourceHashHolder.getHashedReference()).isNull();
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

}
