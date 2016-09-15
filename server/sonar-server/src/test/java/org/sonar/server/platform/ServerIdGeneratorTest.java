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
package org.sonar.server.platform;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;

public class ServerIdGeneratorTest {
  static InetAddress localhost;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ServerIdGenerator underTest = new ServerIdGenerator(true);

  @BeforeClass
  public static void init() throws UnknownHostException {
    localhost = InetAddress.getLocalHost();
  }

  @Test
  public void shouldNotGenerateIdIfBlankParams() {
    ServerIdGenerator generator = new ServerIdGenerator(true);
    assertThat(generator.validate("  ", "127.0.0.1", "191e806623bb0c2")).isFalse();
    assertThat(generator.validate("SonarSource", "   ", "191e806623bb0c2")).isFalse();
  }

  @Test
  public void organizationShouldRespectPattern() {
    ServerIdGenerator generator = new ServerIdGenerator(true);
    assertThat(generator.generate("SonarSource", "127.0.0.1")).isEqualTo("191e806623bb0c2");
    assertThat(generator.validate("SonarSource", "127.0.0.1", "191e806623bb0c2")).isTrue();
    assertThat(generator.validate("SonarSource$", "127.0.0.1", "191e806623bb0c2")).isFalse();
  }

  @Test
  public void fail_if_organization_does_not_respect_pattern() {
    assertThat(underTest.generate("SonarSource", "127.0.0.1")).isNotEmpty();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Organization name is invalid. Alpha numeric characters and space only are allowed. 'SonarSource$' was provided.");

    underTest.generate("SonarSource$", "127.0.0.1");
  }

  @Test
  public void fail_if_organization_is_blank() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Organization name must not be null or empty");

    underTest.generate("   ", "127.0.0.1");
  }

  @Test
  public void fail_if_ip_blank() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("IP must not be null or empty");

    underTest.generate("SonarSource", "     ");
  }

  @Test
  public void fail_if_ip_is_unknown() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Invalid IP '50.154.42.42'");

    underTest.generate("SonarSource", "50.154.42.42");
  }

  @Test
  public void checkValidOrganizationName() {
    ServerIdGenerator generator = new ServerIdGenerator();
    assertThat(generator.isValidOrganizationName("Sonar Source")).isTrue();
    assertThat(generator.isValidOrganizationName("Sonar Source 5")).isTrue();
    assertThat(generator.isValidOrganizationName("Sonar Source $")).isFalse();
    assertThat(generator.isValidOrganizationName("Sonar Source Héhé")).isFalse();
    assertThat(generator.isValidOrganizationName("Sonar Source \n")).isFalse();
    assertThat(generator.isValidOrganizationName("  ")).isFalse();
    assertThat(generator.isValidOrganizationName("\tBar ")).isFalse();
  }

  @Test
  public void idShouldHaveTenCharacters() {
    String id = new ServerIdGenerator().toId("SonarSource", localhost);
    assertThat(id).hasSize(15); // first character is version + 14 characters for checksum
    assertThat(isBlank(id)).isFalse();
  }

  @Test
  public void idShouldStartWithVersion() {
    String id = new ServerIdGenerator().toId("SonarSource", localhost);
    assertThat(id).startsWith(ServerIdGenerator.VERSION);
  }

  @Test
  public void loopbackAddressesShouldNotBeAccepted() throws UnknownHostException {
    assertThat(new ServerIdGenerator().isFixed(InetAddress.getLoopbackAddress())).isFalse();
  }

  @Test
  public void idShouldBeUniquePerOrganization() {
    ServerIdGenerator generator = new ServerIdGenerator(true);

    String k1 = generator.generate("Corp One", "127.0.0.1");
    String k2 = generator.generate("Corp Two", "127.0.0.1");
    assertThat(StringUtils.equals(k1, k2)).isFalse();
  }

  @Test
  public void idShouldBeReproducible() {
    ServerIdGenerator generator = new ServerIdGenerator(true);
    String i1 = generator.generate("SonarSource", "127.0.0.1");
    String i2 = generator.generate("SonarSource", "127.0.0.1");
    assertThat(StringUtils.equals(i1, i2)).isTrue();
  }

}
