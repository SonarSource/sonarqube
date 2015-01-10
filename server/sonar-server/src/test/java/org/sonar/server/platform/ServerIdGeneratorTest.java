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
package org.sonar.server.platform;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.Is;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class ServerIdGeneratorTest {

  private static InetAddress localhost;

  @BeforeClass
  public static void init() throws UnknownHostException {
    localhost = InetAddress.getLocalHost();
  }

  @Test
  public void shouldNotGenerateIdIfBlankParams() {
    ServerIdGenerator generator = new ServerIdGenerator(true);
    assertThat(generator.generate("  ", "127.0.0.1")).isNull();
    assertThat(generator.generate("SonarSource", "   ")).isNull();
  }

  @Test
  public void organizationShouldRespectPattern() {
    ServerIdGenerator generator = new ServerIdGenerator(true);
    assertThat(generator.generate("SonarSource", "127.0.0.1")).isNotNull();
    assertThat(generator.generate("SonarSource$", "127.0.0.1")).isNull();
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
    assertThat(id.length(), Is.is(15)); // first character is version + 14 characters for checksum
    assertThat(StringUtils.isBlank(id), Is.is(false));
  }

  @Test
  public void idShouldStartWithVersion() {
    String id = new ServerIdGenerator().toId("SonarSource", localhost);
    assertThat(id, startsWith(ServerIdGenerator.VERSION));
  }

  @Test
  public void loopbackAddressesShouldNotBeAccepted() throws UnknownHostException {
    assertThat(new ServerIdGenerator().isFixed(InetAddress.getByName("127.0.0.1")), Is.is(false));
  }

  @Test
  public void publicAddressesNotBeAccepted() throws UnknownHostException {
    assertThat(new ServerIdGenerator().isFixed(InetAddress.getByName("sonarsource.com")), Is.is(true));
  }

  @Test
  public void idShouldBeUniquePerOrganisation() {
    ServerIdGenerator generator = new ServerIdGenerator(true);

    String k1 = generator.generate("Corp One", "127.0.0.1");
    String k2 = generator.generate("Corp Two", "127.0.0.1");
    assertThat(StringUtils.equals(k1, k2), Is.is(false));
  }

  @Test
  public void idShouldBeReproducible() {
    ServerIdGenerator generator = new ServerIdGenerator(true);
    String i1 = generator.generate("SonarSource", "127.0.0.1");
    String i2 = generator.generate("SonarSource", "127.0.0.1");
    assertThat(StringUtils.equals(i1, i2), Is.is(true));
  }

}
