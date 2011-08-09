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
import static org.junit.Assert.*;

public class ServerKeyGeneratorTest {

  private static InetAddress localhost;

  @BeforeClass
  public static void init() throws UnknownHostException {
    localhost = InetAddress.getLocalHost();
  }

  @Test
  public void keyShouldHaveTenCharacters() {
    ServerKeyGenerator.ServerKey key = new ServerKeyGenerator.ServerKey("SonarSource", localhost);
    assertThat(key.getKey().length(), Is.is(10)); // first character is version + 9 characters for checksum
    assertThat(StringUtils.isBlank(key.getKey()), Is.is(false));
  }

  @Test
  public void keyShouldStartWithVersion() {
    ServerKeyGenerator.ServerKey key = new ServerKeyGenerator.ServerKey("SonarSource", localhost);
    assertThat(key.getKey(), startsWith(ServerKeyGenerator.VERSION));
  }

  @Test
  public void loopbackAddressesShouldNotBeValid() throws UnknownHostException {
    assertThat(new ServerKeyGenerator.ServerKey("SonarSource", InetAddress.getByName("127.0.0.1")).isValid(), Is.is(false));
  }

  @Test
  public void testEqualsAndHashCode() {
    ServerKeyGenerator.ServerKey key1 = new ServerKeyGenerator.ServerKey("Corp One", localhost);
    ServerKeyGenerator.ServerKey key2 = new ServerKeyGenerator.ServerKey("Corp Two", localhost);
    assertEquals(key1, key1);
    assertEquals(key1.hashCode(), key1.hashCode());

    assertThat(key1.equals(key2), Is.is(false));
    assertThat(key2.equals(key1), Is.is(false));

    assertThat(key1.equals("string"), Is.is(false));
  }

  @Test
  public void shouldGenerateKey() {
    String key = new ServerKeyGenerator().generate("SonarSource");
    assertThat(StringUtils.isNotBlank(key), Is.is(true));
  }

  @Test
  public void organizationShouldBeMandatory() {
    assertNull(new ServerKeyGenerator().generate(null));
    assertNull(new ServerKeyGenerator().generate(""));
    assertNull(new ServerKeyGenerator().generate("    "));
  }

  @Test
  public void keyShouldBeUniquePerOrganization() {
    ServerKeyGenerator generator = new ServerKeyGenerator();
    String k1 = generator.generate("Corp One");
    String k2 = generator.generate("Corp Two");
    assertThat(StringUtils.equals(k1, k2), Is.is(false));
  }

  @Test
  public void keyShouldBeReproducible() {
    ServerKeyGenerator generator = new ServerKeyGenerator();
    String k1 = generator.generate("SonarSource");
    String k2 = generator.generate("SonarSource");
    assertThat(StringUtils.equals(k1, k2), Is.is(true));
  }

  @Test
  public void shouldNotKeepPreviousKeyIfNotValid() {
    ServerKeyGenerator generator = new ServerKeyGenerator();
    String key = generator.generate("SonarSource", "unvalid");
    assertNotNull(key);
    assertThat(StringUtils.equals(key, "unvalid"), Is.is(false));
  }


}
