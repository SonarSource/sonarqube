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

package org.sonarqube.ws.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.client.WsRequest.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.WsRequest.newGetRequest;
import static org.sonarqube.ws.client.WsRequest.newPostRequest;

public class WsRequestTest {

  static final String ENDPOINT = "api/issues/search";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsRequest underTest;

  @Test
  public void get_request() {
    underTest = newGetRequest("api/issues/search");

    assertThat(underTest.getMethod()).isEqualTo(Method.GET);
  }

  @Test
  public void post_request() {
    underTest = newPostRequest("api/issues/search");

    assertThat(underTest.getMethod()).isEqualTo(Method.POST);
  }

  @Test
  public void set_non_null_param() {
    underTest = newGetRequest("api/issues/search")
      .setParam("key", "value");

    assertThat(underTest.getParams().get("key")).isEqualTo("value");
  }

  @Test
  public void set_null_param_remove_existing_param() {
    underTest = newGetRequest(ENDPOINT)
      .setParam("key", "value")
      .setParam("key", null);

    assertThat(underTest.getParams().get("key")).isNull();
  }

  @Test
  public void fail_if_key_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("a WS parameter key cannot be null");

    underTest = newGetRequest(ENDPOINT)
      .setParam(null, "value");
  }
}
