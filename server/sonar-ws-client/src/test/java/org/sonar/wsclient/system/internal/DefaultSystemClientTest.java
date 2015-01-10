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
package org.sonar.wsclient.system.internal;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.system.Migration;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSystemClientTest {

  static final String RUNNING_JSON = "{\"status\": \"KO\", \"state\": \"MIGRATION_RUNNING\", " +
    "\"operational\": false, \"startedAt\": \"2013-12-20T12:34:56+0100\"}";

  static final String DONE_JSON = "{\"status\": \"OK\", \"state\": \"MIGRATION_SUCCEEDED\", " +
    "\"operational\": true, \"message\": \"done\"}";

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void start_migration_asynchronously() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(RUNNING_JSON);

    DefaultSystemClient client = new DefaultSystemClient(requestFactory);
    Migration migration = client.migrate();

    assertThat(httpServer.requestedPath()).isEqualTo("/api/server/setup");
    assertThat(migration.status()).isEqualTo(Migration.Status.MIGRATION_RUNNING);
    assertThat(migration.operationalWebapp()).isFalse();
    assertThat(migration.startedAt().getYear()).isEqualTo(113);//2013 = nb of years since 1900
  }

  @Test
  public void fail_if_rate_is_greater_than_timeout() throws Exception {
    try {
      DefaultSystemClient client = new DefaultSystemClient(mock(HttpRequestFactory.class));
      client.migrate(5L, 50L);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Timeout must be greater than rate");
    }
  }

  @Test
  public void stop_synchronous_migration_on_timeout() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());
    httpServer.stubResponseBody(RUNNING_JSON);

    DefaultSystemClient client = new DefaultSystemClient(requestFactory);
    Migration migration = client.migrate(50L, 5L);

    assertThat(migration.status()).isEqualTo(Migration.Status.MIGRATION_RUNNING);
    assertThat(migration.operationalWebapp()).isFalse();
  }

  @Test
  public void return_result_before_timeout_of_synchronous_migration() {
    HttpRequestFactory requestFactory = mock(HttpRequestFactory.class);
    when(requestFactory.post(eq("/api/server/setup"), anyMap())).thenReturn(
      RUNNING_JSON, DONE_JSON
    );

    DefaultSystemClient client = new DefaultSystemClient(requestFactory);
    Migration migration = client.migrate(500L, 5L);

    assertThat(migration.status()).isEqualTo(Migration.Status.MIGRATION_SUCCEEDED);
    assertThat(migration.operationalWebapp()).isTrue();
    assertThat(migration.message()).isEqualTo("done");
    assertThat(migration.startedAt()).isNull();
  }

  @Test
  public void fail_if_missing_state() {
    // should never occur
    HttpRequestFactory requestFactory = mock(HttpRequestFactory.class);
    when(requestFactory.post(eq("/api/server/setup"), anyMap())).thenReturn(
      "{\"status\": \"ko\", \"message\": \"done\"}"
    );

    DefaultSystemClient client = new DefaultSystemClient(requestFactory);
    try {
      client.migrate(500L, 5L);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("State is not set");
    }
  }

  @Test
  public void restart() {
    HttpRequestFactory requestFactory = mock(HttpRequestFactory.class);
    DefaultSystemClient client = new DefaultSystemClient(requestFactory);
    client.restart();
    verify(requestFactory).post("/api/system/restart", Collections.<String, Object>emptyMap());
  }
}
