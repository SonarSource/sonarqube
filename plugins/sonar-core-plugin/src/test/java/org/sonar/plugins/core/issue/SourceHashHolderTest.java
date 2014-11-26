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
package org.sonar.plugins.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.batch.scan.LastLineHashes;

import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceHashHolderTest {

  SourceHashHolder sourceHashHolder;

  LastLineHashes lastSnapshots;
  DefaultInputFile file;

  @Before
  public void setUp() {
    lastSnapshots = mock(LastLineHashes.class);
    file = mock(DefaultInputFile.class);

    sourceHashHolder = new SourceHashHolder(file, lastSnapshots);
  }

  @Test
  public void should_lazy_load_line_hashes() {
    final String source = "source";
    when(file.lineHashes()).thenReturn(new byte[][] {md5(source), null});

    assertThat(sourceHashHolder.getHashedSource().getHash(1)).isEqualTo(md5Hex(source));
    assertThat(sourceHashHolder.getHashedSource().getHash(2)).isEqualTo("");
    verify(file).lineHashes();
    verify(file).key();
    verify(file).status();

    assertThat(sourceHashHolder.getHashedSource().getHash(1)).isEqualTo(md5Hex(source));
    Mockito.verifyNoMoreInteractions(file);
  }

  @Test
  public void should_lazy_load_reference_hashes_when_status_changed() {
    final String source = "source";
    String key = "foo:src/Foo.java";
    when(file.lineHashes()).thenReturn(new byte[][] {md5(source)});
    when(file.key()).thenReturn(key);
    when(file.status()).thenReturn(InputFile.Status.CHANGED);
    when(lastSnapshots.getLineHashes(key)).thenReturn(new String[] {md5Hex(source)});

    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    verify(lastSnapshots).getLineHashes(key);

    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

  @Test
  public void should_not_load_reference_hashes_when_status_same() {
    final String source = "source";
    String key = "foo:src/Foo.java";
    when(file.lineHashes()).thenReturn(new byte[][] {md5(source)});
    when(file.key()).thenReturn(key);
    when(file.status()).thenReturn(InputFile.Status.SAME);

    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    assertThat(sourceHashHolder.getHashedReference().getHash(1)).isEqualTo(md5Hex(source));
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

  @Test
  public void no_reference_hashes_when_status_added() {
    final String source = "source";
    String key = "foo:src/Foo.java";
    when(file.lineHashes()).thenReturn(new byte[][] {md5(source)});
    when(file.key()).thenReturn(key);
    when(file.status()).thenReturn(InputFile.Status.ADDED);

    assertThat(sourceHashHolder.getHashedReference()).isNull();
    Mockito.verifyNoMoreInteractions(lastSnapshots);
  }

}
