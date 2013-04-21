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
package org.sonar.api.config;

import org.apache.commons.codec.binary.Base64;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

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
    "Other: field\n";

  @Test
  public void readPlainTest() {
    License license = License.readPlainText(V2_FORMAT);

    assertThat(license.getOrganization(), Is.is("ABC"));
    assertThat(license.getServer(), Is.is("12345"));
    assertThat(license.getProduct(), Is.is("SQALE"));
    assertThat(license.getExpirationDateAsString(), Is.is("2012-05-18"));
    assertThat(license.getType(), Is.is("EVALUATION"));
  }

  @Test
  public void readPlainText_empty_fields() {
    License license = License.readPlainText("");

    assertThat(license.getOrganization(), nullValue());
    assertThat(license.getServer(), nullValue());
    assertThat(license.getProduct(), nullValue());
    assertThat(license.getExpirationDateAsString(), nullValue());
    assertThat(license.getExpirationDate(), nullValue());
    assertThat(license.getType(), nullValue());
  }

  @Test
  public void readPlainText_not_valid_input() {
    License license = License.readPlainText("old pond ... a frog leaps in water’s sound");

    assertThat(license.getOrganization(), nullValue());
    assertThat(license.getServer(), nullValue());
    assertThat(license.getProduct(), nullValue());
    assertThat(license.getExpirationDateAsString(), nullValue());
    assertThat(license.getExpirationDate(), nullValue());
    assertThat(license.getType(), nullValue());
  }

  @Test
  public void readPlainTest_version_1() {
    License license = License.readPlainText(V1_FORMAT);

    assertThat(license.getOrganization(), Is.is("ABC"));
    assertThat(license.getServer(), nullValue());
    assertThat(license.getProduct(), Is.is("SQALE"));
    assertThat(license.getExpirationDateAsString(), Is.is("2012-05-18"));
    assertThat(license.getType(), nullValue());
  }

  @Test
  public void readBase64() {
    License license = License.readBase64(new String(Base64.encodeBase64(V2_FORMAT.getBytes())));

    assertThat(license.getOrganization(), Is.is("ABC"));
    assertThat(license.getServer(), Is.is("12345"));
    assertThat(license.getProduct(), Is.is("SQALE"));
    assertThat(license.getExpirationDateAsString(), Is.is("2012-05-18"));
    assertThat(license.getType(), Is.is("EVALUATION"));
  }

  @Test
  public void trimBeforeReadingBase64() {
    String encodedKeyWithTrailingWhiteSpaces = "Rm9vOiBiYXIKT3JnYW5pc2F0aW9uOiBBQkMgClNlcnZlcjogMTIzND  \n" +
      "UgICAKUHJvZHVjdDogU1FBTEUKICBFeHBpcmF0aW9uOiAyMDEyLTA1    \n" +
      "LTE4ICAKVHlwZTogIEVWQUxVQVRJT04gICAKT3RoZXI6IGZpZWxkCg==\n";

    License license = License.readBase64(new String(encodedKeyWithTrailingWhiteSpaces.getBytes()));

    assertThat(license.getOrganization(), Is.is("ABC"));
    assertThat(license.getServer(), Is.is("12345"));
    assertThat(license.getProduct(), Is.is("SQALE"));
    assertThat(license.getExpirationDateAsString(), Is.is("2012-05-18"));
    assertThat(license.getType(), Is.is("EVALUATION"));
  }

  @Test
  public void readBase64_not_base64() {
    License license = License.readBase64("çé '123$@");

    assertThat(license.getOrganization(), nullValue());
    assertThat(license.getServer(), nullValue());
    assertThat(license.getProduct(), nullValue());
    assertThat(license.getExpirationDateAsString(), nullValue());
    assertThat(license.getExpirationDate(), nullValue());
    assertThat(license.getType(), nullValue());
  }

  @Test
  public void isExpired() {
    License license = License.readPlainText(V2_FORMAT);

    assertThat(license.isExpired(DateUtils.parseDate("2013-06-23")), is(true));
    assertThat(license.isExpired(DateUtils.parseDate("2012-05-18")), is(true));
    assertThat(license.isExpired(DateUtils.parseDateTime("2012-05-18T15:50:45+0100")), is(true));
    assertThat(license.isExpired(DateUtils.parseDate("2011-01-01")), is(false));
  }
}
