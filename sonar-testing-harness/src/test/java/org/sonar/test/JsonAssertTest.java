package org.sonar.test;///*
// * SonarQube
// * Copyright (C) 2009-2024 SonarSource SA
// * mailto:info AT sonarsource DOT com
// *
// * This program is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 3 of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
// */
//package org.sonar.test;
//
//import java.io.File;
//import java.net.URL;
//import org.junit.ComparisonFailure;
//import org.junit.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.junit.Assert.fail;
//import static org.sonar.test.JsonAssert.assertJson;
//
//public class JsonAssertTest {
//
//  @Test
//  public void isSimilarAs_strings() {
//    assertJson("{}").isSimilarTo("{}");
//
//    try {
//      assertJson("{}").isSimilarTo("[]");
//      fail();
//    } catch (ComparisonFailure error) {
//      assertThat(error.getMessage()).isEqualTo("Not a super-set of expected JSON - expected:<[[]]> but was:<[{}]>");
//      assertThat(error.getActual()).isEqualTo("{}");
//      assertThat(error.getExpected()).isEqualTo("[]");
//    }
//  }
//
//  @Test
//  public void isSimilarAs_urls() {
//    URL url1 = getClass().getResource("JsonAssertTest/sample1.json");
//    URL url2 = getClass().getResource("JsonAssertTest/sample2.json");
//    assertJson(url1).isSimilarTo(url1);
//
//    assertThatThrownBy(() -> assertJson(url1).isSimilarTo(url2))
//      .isInstanceOf(AssertionError.class);
//  }
//
//  @Test
//  public void actual_can_be_superset_of_expected() {
//    assertJson("{\"foo\": \"bar\"}").isSimilarTo("{}");
//
//    assertThatThrownBy(() -> assertJson("{}").isSimilarTo("{\"foo\": \"bar\"}"))
//      .isInstanceOf(AssertionError.class);
//  }
//
//  @Test
//  public void fail_to_load_url() {
//    assertThatThrownBy(() -> assertJson(new File("target/missing").toURI().toURL()))
//      .isInstanceOf(IllegalStateException.class);
//  }
//
//  @Test
//  public void enable_strict_order_of_arrays() {
//    assertThatThrownBy(() -> assertJson("[1,2]").withStrictArrayOrder().isSimilarTo("[2, 1]"))
//      .isInstanceOf(AssertionError.class);
//  }
//
//  @Test
//  public void enable_strict_timezone() {
//    assertThatThrownBy(() -> assertJson("[\"2010-05-18T15:50:45+0100\"]").withStrictTimezone().isSimilarTo("[\"2010-05-18T16:50:45+0200\"]"))
//      .isInstanceOf(AssertionError.class);
//  }
//
//  @Test
//  public void ignore_fields() {
//    assertJson("{\"foo\": \"bar\"}")
//      .ignoreFields("ignore-me")
//      .isSimilarTo("{\"foo\": \"bar\", \"ignore-me\": \"value\"}");
//  }
//}
//
//

import org.junit.ComparisonFailure;
import org.junit.Test;
import org.sonar.test.JsonAssert;

import java.net.URL;

public class JsonAssertTest {

  @Test
  public void testIsSimilarTo_withIdenticalJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"John\", \"age\":30}";
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedJson);
  }

  @Test(expected = ComparisonFailure.class)
  public void testIsSimilarTo_withDifferentJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"Jane\", \"age\":25}";
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedJson);
  }

  @Test
  public void testIsSimilarTo_withExtraFieldsInActual() {
    String actualJson = "{\"name\":\"John\", \"age\":30, \"city\":\"New York\"}";
    String expectedJson = "{\"name\":\"John\", \"age\":30}";
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedJson);
  }

  @Test
  public void testIsNotSimilarTo_withDifferentJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"Jane\", \"age\":25}";
    JsonAssert.assertJson(actualJson).isNotSimilarTo(expectedJson);
  }

  @Test(expected = ComparisonFailure.class)
  public void testIsNotSimilarTo_withIdenticalJson() {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    String expectedJson = "{\"name\":\"John\", \"age\":30}";
    JsonAssert.assertJson(actualJson).isNotSimilarTo(expectedJson);
  }

  @Test
  public void testWithStrictArrayOrder() {
    String actualJson = "[\"apple\", \"banana\"]";
    String expectedJson = "[\"banana\", \"apple\"]";
    JsonAssert.assertJson(actualJson)
            .withStrictArrayOrder()
            .isNotSimilarTo(expectedJson); // Should not be similar because the order matters
  }

  @Test
  public void testWithStrictTimezone() {
    String actualJson = "{\"time\": \"2020-01-01T10:00:00+0100\"}";
    String expectedJson = "{\"time\": \"2020-01-01T09:00:00Z\"}";
    JsonAssert.assertJson(actualJson)
            .withStrictTimezone()
            .isNotSimilarTo(expectedJson); // Should not be similar because the timezone matters
  }

  @Test
  public void testIgnoreFields() {
    String actualJson = "{\"name\":\"John\", \"age\":30, \"city\":\"New York\"}";
    String expectedJson = "{\"name\":\"John\", \"city\":\"Los Angeles\"}";
    JsonAssert.assertJson(actualJson)
            .ignoreFields("age")
            .isSimilarTo(expectedJson); // Should be similar because age is ignored
  }

  // Test loading expected JSON from URL
  @Test
  public void testIsSimilarTo_withUrlExpected() throws Exception {
    String actualJson = "{\"name\":\"John\", \"age\":30}";
    URL expectedUrl = new URL("http://example.com/expected.json"); // This should point to a real URL for your test
    JsonAssert.assertJson(actualJson).isSimilarTo(expectedUrl);
  }

}

