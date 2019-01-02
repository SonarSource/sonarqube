/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.Version.parse;

public class VersionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_parse() {
    assertVersion(parse(""), 0, 0, 0, 0, "");
    assertVersion(parse("1"), 1, 0, 0, 0, "");
    assertVersion(parse("1.2"), 1, 2, 0, 0,"");
    assertVersion(parse("1.2.3"), 1, 2, 3, 0,"");
    assertVersion(parse("1.2-beta-1"), 1, 2, 0, 0,"beta-1");
    assertVersion(parse("1.2.3-beta1"), 1, 2, 3, 0,"beta1");
    assertVersion(parse("1.2.3-beta-1"), 1, 2, 3, 0,"beta-1");
    assertVersion(parse("1.2.3.4567"), 1, 2, 3, 4567,"");
    assertVersion(parse("1.2.3.4567-alpha"), 1, 2, 3, 4567,"alpha");
  }

  @Test
  public void parse_throws_IAE_if_more_than_4_fields() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Maximum 4 fields are accepted: 1.2.3.456.7");

    parse("1.2.3.456.7");
  }

  @Test
  public void test_equals() {
    Version one = parse("1");
    assertThat(one).isEqualTo(one);
    assertThat(one).isEqualTo(parse("1"));
    assertThat(one).isEqualTo(parse("1.0"));
    assertThat(one).isEqualTo(parse("1.0.0"));
    assertThat(one).isNotEqualTo(parse("1.2.3"));
    assertThat(one).isNotEqualTo("1");

    assertThat(parse("1.2.3")).isEqualTo(parse("1.2.3"));
    assertThat(parse("1.2.3")).isNotEqualTo(parse("1.2.4"));
    assertThat(parse("1.2.3")).isEqualTo(parse("1.2.3-b1"));
    assertThat(parse("1.2.3-b1")).isEqualTo(parse("1.2.3-b2"));
  }

  @Test
  public void test_hashCode() {
    assertThat(parse("1").hashCode()).isEqualTo(parse("1").hashCode());
    assertThat(parse("1").hashCode()).isEqualTo(parse("1.0.0").hashCode());
    assertThat(parse("1.2.3-beta1").hashCode()).isEqualTo(parse("1.2.3").hashCode());
  }

  @Test
  public void test_compareTo() {
    assertThat(parse("1.2").compareTo(parse("1.2.0"))).isEqualTo(0);
    assertThat(parse("1.2.3").compareTo(parse("1.2.3"))).isEqualTo(0);
    assertThat(parse("1.2.3").compareTo(parse("1.2.4"))).isLessThan(0);
    assertThat(parse("1.2.3").compareTo(parse("1.3"))).isLessThan(0);
    assertThat(parse("1.2.3").compareTo(parse("2.1"))).isLessThan(0);
    assertThat(parse("1.2.3").compareTo(parse("2.0.0"))).isLessThan(0);
    assertThat(parse("2.0").compareTo(parse("1.2"))).isGreaterThan(0);
  }

  @Test
  public void compareTo_handles_build_number() {
    assertThat(parse("1.2").compareTo(parse("1.2.0.0"))).isEqualTo(0);
    assertThat(parse("1.2.3.1234").compareTo(parse("1.2.3.4567"))).isLessThan(0);
    assertThat(parse("1.2.3.1234").compareTo(parse("1.2.3"))).isGreaterThan(0);
    assertThat(parse("1.2.3.1234").compareTo(parse("1.2.4"))).isLessThan(0);
    assertThat(parse("1.2.3.9999").compareTo(parse("1.2.4.1111"))).isLessThan(0);
  }

  @Test
  public void qualifier_is_ignored_from_comparison() {
    assertThat(parse("1.2.3")).isEqualTo(parse("1.2.3-build1"));
    assertThat(parse("1.2.3")).isEqualTo(parse("1.2.3-build1"));
    assertThat(parse("1.2.3").compareTo(parse("1.2.3-build1"))).isEqualTo(0);
  }

  @Test
  public void test_toString() {
    assertThat(parse("1").toString()).isEqualTo("1.0");
    assertThat(parse("1.2").toString()).isEqualTo("1.2");
    assertThat(parse("1.2.3").toString()).isEqualTo("1.2.3");
    assertThat(parse("1.2-b1").toString()).isEqualTo("1.2-b1");
    assertThat(parse("1.2.3-b1").toString()).isEqualTo("1.2.3-b1");
    assertThat(parse("1.2.3.4567").toString()).isEqualTo("1.2.3.4567");
    assertThat(parse("1.2.3.4567-beta1").toString()).isEqualTo("1.2.3.4567-beta1");

    // do not display zero numbers when possible
    assertThat(parse("1.2.0.0").toString()).isEqualTo("1.2");
    assertThat(parse("1.2.0.1").toString()).isEqualTo("1.2.0.1");
    assertThat(parse("1.2.1.0").toString()).isEqualTo("1.2.1");
    assertThat(parse("1.2.1.0-beta").toString()).isEqualTo("1.2.1-beta");
  }

  @Test
  public void test_create() {
    assertVersion(Version.create(1, 2), 1, 2, 0, 0, "");
    assertVersion(Version.create(1, 2, 3), 1, 2, 3, 0, "");
    assertVersion(Version.create(1, 2, 0, ""), 1, 2, 0, 0, "");
    assertVersion(Version.create(1, 2, 3, "build1"), 1, 2, 3, 0, "build1");
    assertThat(Version.create(1, 2, 3, "build1").toString()).isEqualTo("1.2.3-build1");

  }

  private static void assertVersion(Version version,
                                    int expectedMajor, int expectedMinor, int expectedPatch, long expectedBuildNumber, String expectedQualifier) {
    assertThat(version.major()).isEqualTo(expectedMajor);
    assertThat(version.minor()).isEqualTo(expectedMinor);
    assertThat(version.patch()).isEqualTo(expectedPatch);
    assertThat(version.buildNumber()).isEqualTo(expectedBuildNumber);
    assertThat(version.qualifier()).isEqualTo(expectedQualifier);
  }
}
