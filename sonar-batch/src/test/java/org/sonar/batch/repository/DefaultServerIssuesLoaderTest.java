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
package org.sonar.batch.repository;

import org.sonar.batch.cache.WSLoaderResult;

import org.sonar.batch.cache.WSLoader;
import com.google.common.io.ByteSource;
import com.google.common.base.Function;
import org.junit.Before;
import org.junit.Test;
import org.sonar.batch.protocol.input.BatchInput;
import org.sonar.batch.protocol.input.BatchInput.ServerIssue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultServerIssuesLoaderTest {
  private DefaultServerIssuesLoader loader;
  private WSLoader wsLoader;

  @Before
  public void prepare() {
    wsLoader = mock(WSLoader.class);
    loader = new DefaultServerIssuesLoader(wsLoader);
  }

  @Test
  public void loadFromWs() throws Exception {
    ByteSource bs = mock(ByteSource.class);
    when(wsLoader.loadSource("/scanner/issues?key=foo")).thenReturn(new WSLoaderResult<>(bs, true));

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    ServerIssue.newBuilder().setKey("ab1").build()
      .writeDelimitedTo(bos);
    ServerIssue.newBuilder().setKey("ab2").build()
      .writeDelimitedTo(bos);

    when(bs.openBufferedStream()).thenReturn(new ByteArrayInputStream(bos.toByteArray()));

    final List<ServerIssue> result = new ArrayList<>();
    loader.load("foo", new Function<BatchInput.ServerIssue, Void>() {

      @Override
      public Void apply(ServerIssue input) {
        result.add(input);
        return null;
      }
    });

    assertThat(result).extracting("key").containsExactly("ab1", "ab2");
  }

  @Test(expected = IllegalStateException.class)
  public void testError() throws IOException {
    ByteSource source = mock(ByteSource.class);
    when(source.openBufferedStream()).thenThrow(IOException.class);
    when(wsLoader.loadSource("/scanner/issues?key=foo")).thenReturn(new WSLoaderResult<ByteSource>(source, true));
    loader.load("foo", mock(Function.class));
  }
}
