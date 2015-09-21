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

import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Before;
import org.junit.Test;
import org.sonar.batch.cache.WSLoader;
import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.protocol.input.GlobalRepositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultGlobalRepositoriesLoaderTest {
  private static final String BATCH_GLOBAL_URL = "/scanner/global";
  private WSLoader wsLoader;
  private WSLoaderResult<String> result;
  private DefaultGlobalRepositoriesLoader globalRepositoryLoader;

  @Before
  public void setUp() {
    wsLoader = mock(WSLoader.class);
    result = new WSLoaderResult<>(new GlobalRepositories().toJson(), true);
    when(wsLoader.loadString(BATCH_GLOBAL_URL)).thenReturn(result);

    globalRepositoryLoader = new DefaultGlobalRepositoriesLoader(wsLoader);
  }

  @Test
  public void test() {
    MutableBoolean fromCache = new MutableBoolean();
    globalRepositoryLoader.load(fromCache);

    assertThat(fromCache.booleanValue()).isTrue();
    verify(wsLoader).loadString(BATCH_GLOBAL_URL);
    verifyNoMoreInteractions(wsLoader);
  }
  
  @Test
  public void testFromServer() {
    result = new WSLoaderResult<>(new GlobalRepositories().toJson(), false);
    when(wsLoader.loadString(BATCH_GLOBAL_URL)).thenReturn(result);
    MutableBoolean fromCache = new MutableBoolean();
    globalRepositoryLoader.load(fromCache);

    assertThat(fromCache.booleanValue()).isFalse();
    verify(wsLoader).loadString(BATCH_GLOBAL_URL);
    verifyNoMoreInteractions(wsLoader);
  }

  public void testWithoutArg() {
    globalRepositoryLoader.load(null);

    verify(wsLoader).loadString(BATCH_GLOBAL_URL);
    verifyNoMoreInteractions(wsLoader);
  }
}
