/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.platform;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.Is;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.text.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class ServerKeyGeneratorTest {

  private static InetAddress localhost;

  @BeforeClass
  public static void init() throws UnknownHostException {
    localhost = InetAddress.getLocalHost();
  }

  @Test
  public void keyShouldHaveTenCharacters() {
    String key = new ServerKeyGenerator().toKey("SonarSource", localhost);
    assertThat(key.length(), Is.is(10)); // first character is version + 9 characters for checksum
    assertThat(StringUtils.isBlank(key), Is.is(false));
  }

  @Test
  public void keyShouldStartWithVersion() {
    String key = new ServerKeyGenerator().toKey("SonarSource", localhost);
    assertThat(key, startsWith(ServerKeyGenerator.VERSION));
  }

  @Test
  public void loopbackAddressesShouldNotBeAccepted() throws UnknownHostException {
    assertThat(new ServerKeyGenerator().isFixed(InetAddress.getByName("127.0.0.1")), Is.is(false));
  }

  @Test
  public void publicAddressesNotBeAccepted() throws UnknownHostException {
    assertThat(new ServerKeyGenerator().isFixed(InetAddress.getByName("sonarsource.com")), Is.is(true));
  }

  @Test
  public void keyShouldBeUniquePerOrganization() {
    ServerKeyGenerator generator = new ServerKeyGenerator(true);

    String k1 = generator.generate("Corp One", "127.0.0.1");
    String k2 = generator.generate("Corp Two", "127.0.0.1");
    assertThat(StringUtils.equals(k1, k2), Is.is(false));
  }

  @Test
  public void keyShouldBeReproducible() {
    ServerKeyGenerator generator = new ServerKeyGenerator(true);
    String k1 = generator.generate("SonarSource", "127.0.0.1");
    String k2 = generator.generate("SonarSource", "127.0.0.1");
    assertThat(StringUtils.equals(k1, k2), Is.is(true));
  }

}
