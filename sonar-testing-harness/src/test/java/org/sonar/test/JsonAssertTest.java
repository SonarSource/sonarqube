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
package org.sonar.test;

import org.junit.ComparisonFailure;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.test.JsonAssert.assertJson;

public class JsonAssertTest {

  @Test
  public void isSimilarAs_strings() throws Exception {
    assertJson("{}").isSimilarTo("{}");

    try {
      assertJson("{}").isSimilarTo("[]");
      fail();
    } catch (ComparisonFailure error) {
      assertThat(error.getMessage()).isEqualTo("Not a super-set of expected JSON - expected:<[[]]> but was:<[{}]>");
      assertThat(error.getActual()).isEqualTo("{}");
      assertThat(error.getExpected()).isEqualTo("[]");
    }
  }

  @Test
  public void isSimilarAs_urls() throws Exception {
    URL url1 = getClass().getResource("JsonAssertTest/sample1.json");
    URL url2 = getClass().getResource("JsonAssertTest/sample2.json");
    assertJson(url1).isSimilarTo(url1);

    try {
      assertJson(url1).isSimilarTo(url2);
      fail();
    } catch (AssertionError error) {
      // ok
    }
  }

  @Test
  public void actual_can_be_superset_of_expected() throws Exception {
    assertJson("{\"foo\": \"bar\"}").isSimilarTo("{}");
    try {
      assertJson("{}").isSimilarTo("{\"foo\": \"bar\"}");
      fail();
    } catch (AssertionError error) {
      // ok
    }
  }

  @Test(expected = IllegalStateException.class)
  public void fail_to_load_url() throws Exception {
    assertJson(new File("target/missing").toURL());
  }

  @Test
  public void enable_strict_order_of_arrays() throws Exception {
    try {
      assertJson("[1,2]").setStrictArrayOrder(true).isSimilarTo("[2, 1]");
      fail();
    } catch (AssertionError error) {
      // ok
    }
  }

  @Test
  public void enable_strict_timezone() throws Exception {
    try {
      assertJson("[\"2010-05-18T15:50:45+0100\"]").setStrictTimezone(true).isSimilarTo("[\"2010-05-18T16:50:45+0200\"]");
      fail();
    } catch (AssertionError error) {
      // ok
    }
  }
}
