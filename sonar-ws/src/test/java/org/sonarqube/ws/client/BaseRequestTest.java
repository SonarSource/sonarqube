/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.MediaTypes;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class BaseRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private FakeRequest underTest = new FakeRequest("api/foo");

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
    assertThat(underTest.getParameters().getKeys()).isEmpty();

    underTest.setParam("keyB", "b");
    assertThat(underTest.getParams()).containsExactly(entry("keyB", "b"));
    assertParameters(entry("keyB", "b"));
    assertMultiValueParameters(entry("keyB", singletonList("b")));

    underTest.setParam("keyA", "a");
    assertThat(underTest.getParams()).containsExactly(entry("keyB", "b"), entry("keyA", "a"));
    assertParameters(entry("keyB", "b"), entry("keyA", "a"));
    assertMultiValueParameters(entry("keyB", singletonList("b")), entry("keyA", singletonList("a")));

    underTest.setParam("keyC", ImmutableList.of("c1", "c2", "c3"));
    assertParameters(entry("keyB", "b"), entry("keyA", "a"), entry("keyC", "c1"));
    assertMultiValueParameters(
      entry("keyB", singletonList("b")),
      entry("keyA", singletonList("a")),
      entry("keyC", ImmutableList.of("c1", "c2", "c3")));
  }

  @Test
  public void skip_null_value_in_multi_param() {
    underTest.setParam("key", newArrayList("v1", null, "v3"));

  }

  @Test
  public void null_param_value() {
    Boolean nullBool = null;
    underTest.setParam("key", nullBool);
    assertThat(underTest.getParams()).isEmpty();
  }

  @Test
  public void fail_if_null_param_key() {
    expectedException.expect(IllegalArgumentException.class);
    underTest.setParam(null, "val");
  }

  @Test
  public void headers_are_empty_by_default() {
    assertThat(underTest.getHeaders().getNames()).isEmpty();
  }

  @Test
  public void set_and_get_headers() {
    underTest.setHeader("foo", "fooz");
    underTest.setHeader("bar", "barz");

    assertThat(underTest.getHeaders().getNames()).containsExactlyInAnyOrder("foo", "bar");
    assertThat(underTest.getHeaders().getValue("foo")).hasValue("fooz");
    assertThat(underTest.getHeaders().getValue("bar")).hasValue("barz");
    assertThat(underTest.getHeaders().getValue("xxx")).isEmpty();
  }

  private void assertParameters(MapEntry<String, String>... values) {
    Parameters parameters = underTest.getParameters();
    assertThat(parameters.getKeys()).extracting(key -> MapEntry.entry(key, parameters.getValue(key))).containsExactly(values);
  }

  private void assertMultiValueParameters(MapEntry<String, List<String>>... expectedParameters) {
    Parameters parameters = underTest.getParameters();
    String[] expectedKeys = Arrays.stream(expectedParameters).map(MapEntry::getKey).toArray(String[]::new);
    assertThat(parameters.getKeys()).containsExactly(expectedKeys);
    Arrays.stream(expectedParameters).forEach(expectedParameter -> {
      assertThat(parameters.getValues(expectedParameter.getKey())).containsExactly(expectedParameter.getValue().toArray(new String[0]));
    });
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
