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

import com.google.common.io.InputSupplier;
import org.junit.Test;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.protocol.input.BatchInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRepositoryTest {

  @Test
  public void testLoad() throws IOException {
    ServerClient serverClient = mock(ServerClient.class);
    UserRepository userRepo = new UserRepository(serverClient);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BatchInput.User.Builder builder = BatchInput.User.newBuilder();
    builder.setLogin("fmallet").setName("Freddy Mallet").build().writeDelimitedTo(out);
    builder.setLogin("sbrandhof").setName("Simon").build().writeDelimitedTo(out);

    InputSupplier<InputStream> is = mock(InputSupplier.class);
    when(serverClient.doRequest("/batch/users?logins=fmallet,sbrandhof", "GET", null))
      .thenReturn(is);
    when(is.getInput()).thenReturn(new ByteArrayInputStream(out.toByteArray()));

    assertThat(userRepo.loadFromWs(Arrays.asList("fmallet", "sbrandhof"))).extracting("login", "name").containsOnly(tuple("fmallet", "Freddy Mallet"), tuple("sbrandhof", "Simon"));
  }
}
