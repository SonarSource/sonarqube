/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import org.sonarqube.ws.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class BaseRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  FakeRequest underTest = new FakeRequest("api/foo");

  @Test
  public void test_defaults() {
    assertThat(underTest.getMethod()).isEqualTo(WsRequest.Method.GET);
    assertThat(underTest.getParams()).isEmpty();
    assertThat(underTest.getMediaType()).isEqualTo(MediaTypes.JSON);
    assertThat(underTest.getPath()).isEqualTo("api/foo");
  }

  @Test
  public void setMediaType() {
    underTest.setMediaType(MediaTypes.PROTOBUF);
    assertThat(underTest.getMediaType()).isEqualTo(MediaTypes.PROTOBUF);
  }

  @Test
  public void keep_order_of_params() {
    assertThat(underTest.getParams()).isEmpty();

    underTest.setParam("keyB", "b");
    assertThat(underTest.getParams()).containsExactly(entry("keyB", "b"));

    underTest.setParam("keyA", "a");
    assertThat(underTest.getParams()).containsExactly(entry("keyB", "b"), entry("keyA", "a"));
  }

  @Test
  public void null_param_value() {
    underTest.setParam("key", null);
    assertThat(underTest.getParams()).isEmpty();
  }

  @Test
  public void fail_if_null_param_key() {
    expectedException.expect(IllegalArgumentException.class);
    underTest.setParam(null, "val");
  }

  private static class FakeRequest extends BaseRequest<FakeRequest> {
    FakeRequest(String path) {
      super(path);
    }

    @Override
    public Method getMethod() {
      return Method.GET;
    }
  }
}
