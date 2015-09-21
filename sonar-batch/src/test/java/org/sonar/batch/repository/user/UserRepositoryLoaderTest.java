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
package org.sonar.batch.repository.user;

import org.assertj.core.util.Lists;

import org.sonar.batch.cache.WSLoaderResult;
import org.sonar.batch.cache.WSLoader;
import org.junit.Before;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.mutable.MutableBoolean;
import com.google.common.collect.ImmutableMap;
import org.junit.rules.ExpectedException;
import org.junit.Rule;
import org.mockito.Mockito;
import com.google.common.io.ByteSource;
import org.junit.Test;
import org.sonar.batch.protocol.input.BatchInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRepositoryLoaderTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private WSLoader wsLoader;
  private UserRepositoryLoader userRepo;

  @Before
  public void setUp() {
    wsLoader = mock(WSLoader.class);
    userRepo = new UserRepositoryLoader(wsLoader);
  }

  @Test
  public void testLoadEmptyList() {
    assertThat(userRepo.load(Lists.<String>emptyList())).isEmpty();
  }
  
  @Test
  public void testLoad() throws IOException {
    Map<String, String> userMap = ImmutableMap.of("fmallet", "Freddy Mallet", "sbrandhof", "Simon");
    WSLoaderResult<ByteSource> res = new WSLoaderResult<>(createUsersMock(userMap), true);
    when(wsLoader.loadSource("/scanner/users?logins=fmallet,sbrandhof")).thenReturn(res);

    assertThat(userRepo.load(Arrays.asList("fmallet", "sbrandhof"))).extracting("login", "name").containsOnly(tuple("fmallet", "Freddy Mallet"), tuple("sbrandhof", "Simon"));
  }

  @Test
  public void testFromCache() throws IOException {
    WSLoaderResult<ByteSource> res = new WSLoaderResult<>(createUsersMock(ImmutableMap.of("fmallet", "Freddy Mallet")), true);
    when(wsLoader.loadSource(anyString())).thenReturn(res);
    MutableBoolean fromCache = new MutableBoolean();
    userRepo.load("", fromCache);
    assertThat(fromCache.booleanValue()).isTrue();

    fromCache.setValue(false);
    userRepo.load(ImmutableList.of("user"), fromCache);
    assertThat(fromCache.booleanValue()).isTrue();
  }

  @Test
  public void testLoadSingleUser() throws IOException {
    WSLoaderResult<ByteSource> res = new WSLoaderResult<>(createUsersMock(ImmutableMap.of("fmallet", "Freddy Mallet")), true);
    when(wsLoader.loadSource("/scanner/users?logins=fmallet")).thenReturn(res);

    assertThat(userRepo.load("fmallet").getName()).isEqualTo("Freddy Mallet");
  }

  private ByteSource createUsersMock(Map<String, String> users) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    for (Map.Entry<String, String> user : users.entrySet()) {
      BatchInput.User.Builder builder = BatchInput.User.newBuilder();
      builder.setLogin(user.getKey()).setName(user.getValue()).build().writeDelimitedTo(out);
    }
    ByteSource source = mock(ByteSource.class);
    when(source.openStream()).thenReturn(new ByteArrayInputStream(out.toByteArray()));
    return source;
  }

  @Test
  public void testInputStreamError() throws IOException {
    ByteSource source = mock(ByteSource.class);

    WSLoaderResult<ByteSource> res = new WSLoaderResult<>(source, true);

    when(wsLoader.loadSource("/scanner/users?logins=fmallet,sbrandhof")).thenReturn(res);

    InputStream errorInputStream = mock(InputStream.class);
    Mockito.doThrow(IOException.class).when(errorInputStream).read();
    when(source.openStream()).thenReturn(errorInputStream);

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unable to get user details from server");

    userRepo.load(Arrays.asList("fmallet", "sbrandhof"));
  }
}
