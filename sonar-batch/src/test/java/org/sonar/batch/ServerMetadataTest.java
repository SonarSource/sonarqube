/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerMetadataTest {
  @Test
  public void should_proxy_target() {
    org.sonar.batch.bootstrap.ServerMetadata client = mock(org.sonar.batch.bootstrap.ServerMetadata.class);
    when(client.getId()).thenReturn("id1");
    when(client.getPermanentServerId()).thenReturn("pid1");
    Date startedAt = DateUtils.parseDate("2012-05-18");
    when(client.getStartedAt()).thenReturn(startedAt);
    when(client.getURL()).thenReturn("http://foo");
    when(client.getVersion()).thenReturn("v1");

    ServerMetadata metadata = new ServerMetadata(client);

    assertThat(metadata.getId()).isEqualTo("id1");
    assertThat(metadata.getPermanentServerId()).isEqualTo("pid1");
    assertThat(metadata.getStartedAt()).isEqualTo(startedAt);
    assertThat(metadata.getURL()).isEqualTo("http://foo");
    assertThat(metadata.getVersion()).isEqualTo("v1");

  }
}
