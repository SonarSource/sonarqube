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
package org.sonar.wsclient.internal;

import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

public class EncodingUtilsTest {
  @Test
  public void test_date_param() {

    assertThat(EncodingUtils.toQueryParam(new Date(), false)).isNotEmpty().matches("\\d{4}-\\d{2}-\\d{2}");
    assertThat(EncodingUtils.toQueryParam(new Date(), true)).isNotEmpty();
  }

  @Test
  public void test_string_array_param() {
    assertThat(EncodingUtils.toQueryParam(new String[] {"foo", "bar"})).isEqualTo("foo,bar");
    assertThat(EncodingUtils.toQueryParam(new String[] {"foo"})).isEqualTo("foo");
    assertThat(EncodingUtils.toQueryParam(new String[] {""})).isEqualTo("");
  }

  @Test
  public void test_toMap() {
    assertThat(EncodingUtils.toMap()).isEmpty();
    assertThat(EncodingUtils.toMap("foo", "bar")).hasSize(1).containsEntry("foo", "bar");
    assertThat(EncodingUtils.toMap("1", "one", "2", "two")).hasSize(2).contains(entry("1", "one"), entry("2", "two"));
    assertThat(EncodingUtils.toMap("foo", null)).isEmpty();
  }

  @Test
  public void toMap_should_fail_if_odd_arguments() {
    try {
      EncodingUtils.toMap("foo");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not an even number of arguments");
    }
  }
}
