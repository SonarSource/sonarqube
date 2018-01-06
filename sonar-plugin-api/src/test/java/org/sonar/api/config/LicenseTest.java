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
package org.sonar.api.config;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import java.util.Calendar;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseTest {

  private static final String V2_FORMAT = "Foo: bar\n" +
    "Organisation: ABC \n" +
    "Server: 12345   \n" +
    "Product: SQALE\n" +
    "  Expiration: 2012-05-18  \n" +
    "Type:  EVALUATION   \n" +
    "Other: field\n";

  private static final String V1_FORMAT = "Foo: bar\n" +
    "Name: ABC \n" +
    "Plugin: SQALE\n" +
    "  Expires: 2012-05-18  \n" +
    "Other: field\n" +
    "Digest: abcdef\n" +
    "Obeo: obeo\n";

  @Test
  public void readPlainTest() {
    License license = License.readPlainText(V2_FORMAT);

    assertThat(license.getOrganization()).isEqualTo("ABC");
    assertThat(license.getServer()).isEqualTo("12345");
    assertThat(license.getProduct()).isEqualTo("SQALE");
    assertThat(license.getExpirationDateAsString()).isEqualTo("2012-05-18");
    assertThat(license.getType()).isEqualTo("EVALUATION");
  }

  @Test
  public void readPlainText_empty_fields() {
    License license = License.readPlainText("");

    assertThat(license.getOrganization()).isNull();
    assertThat(license.getServer()).isNull();
    assertThat(license.getProduct()).isNull();
    assertThat(license.getExpirationDateAsString()).isNull();
    assertThat(license.getExpirationDate()).isNull();
    assertThat(license.getType()).isNull();
  }

  @Test
  public void readPlainText_not_valid_input() {
    License license = License.readPlainText("old pond ... a frog leaps in water’s sound");

    assertThat(license.getOrganization()).isNull();
    assertThat(license.getServer()).isNull();
    assertThat(license.getProduct()).isNull();
    assertThat(license.getExpirationDateAsString()).isNull();
    assertThat(license.getExpirationDate()).isNull();
    assertThat(license.getType()).isNull();
  }

  @Test
  public void readPlainTest_version_1() {
    License license = License.readPlainText(V1_FORMAT);

    assertThat(license.getOrganization()).isEqualTo("ABC");
    assertThat(license.getServer()).isNull();
    assertThat(license.getProduct()).isEqualTo("SQALE");
    assertThat(license.getExpirationDateAsString()).isEqualTo("2012-05-18");
    assertThat(license.getType()).isNull();
  }

  @Test
  public void readBase64() {
    License license = License.readBase64(new String(Base64.encodeBase64(V2_FORMAT.getBytes())));

    assertThat(license.getOrganization()).isEqualTo("ABC");
    assertThat(license.getServer()).isEqualTo("12345");
    assertThat(license.getProduct()).isEqualTo("SQALE");
    assertThat(license.getExpirationDateAsString()).isEqualTo("2012-05-18");
    assertThat(license.getType()).isEqualTo("EVALUATION");
  }

  @Test
  public void trimBeforeReadingBase64() {
    String encodedKeyWithTrailingWhiteSpaces = "Rm9vOiBiYXIKT3JnYW5pc2F0aW9uOiBBQkMgClNlcnZlcjogMTIzND  \n" +
      "UgICAKUHJvZHVjdDogU1FBTEUKICBFeHBpcmF0aW9uOiAyMDEyLTA1    \n" +
      "LTE4ICAKVHlwZTogIEVWQUxVQVRJT04gICAKT3RoZXI6IGZpZWxkCg==\n";

    License license = License.readBase64(new String(encodedKeyWithTrailingWhiteSpaces.getBytes()));

    assertThat(license.getOrganization()).isEqualTo("ABC");
    assertThat(license.getServer()).isEqualTo("12345");
    assertThat(license.getProduct()).isEqualTo("SQALE");
    assertThat(license.getExpirationDateAsString()).isEqualTo("2012-05-18");
    assertThat(license.getType()).isEqualTo("EVALUATION");
  }

  @Test
  public void readBase64_not_base64() {
    License license = License.readBase64("çé '123$@");

    assertThat(license.getOrganization()).isNull();
    assertThat(license.getServer()).isNull();
    assertThat(license.getProduct()).isNull();
    assertThat(license.getExpirationDateAsString()).isNull();
    assertThat(license.getExpirationDate()).isNull();
    assertThat(license.getType()).isNull();
  }

  @Test
  public void isExpired() {
    License license = License.readPlainText(V2_FORMAT);

    assertThat(license.isExpired(DateUtils.parseDate("2011-01-01"))).isFalse();
    Calendar sameDay = Calendar.getInstance(TimeZone.getDefault());
    sameDay.setTime(DateUtils.parseDate("2012-05-18"));
    assertThat(license.isExpired(sameDay.getTime())).isFalse();
    sameDay.set(Calendar.HOUR_OF_DAY, 15);
    assertThat(license.isExpired(sameDay.getTime())).isFalse();
    sameDay.set(Calendar.HOUR_OF_DAY, 23);
    sameDay.set(Calendar.MINUTE, 59);
    sameDay.set(Calendar.SECOND, 59);
    assertThat(license.isExpired(sameDay.getTime())).isFalse();
    // The day after
    sameDay.add(Calendar.SECOND, 1);
    assertThat(license.isExpired(sameDay.getTime())).isTrue();
    assertThat(license.isExpired(DateUtils.parseDate("2013-06-23"))).isTrue();
  }

  @Test
  public void otherProperties() {
    License license = License.readPlainText(V2_FORMAT);

    assertThat(license.additionalProperties().get("Other")).isEqualTo("field");
    assertThat(license.additionalProperties().containsKey("Digest")).isFalse();
    assertThat(license.additionalProperties().containsKey("Obeo")).isFalse();
  }
}
