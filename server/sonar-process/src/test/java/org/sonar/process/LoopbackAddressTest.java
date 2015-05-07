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
package org.sonar.process;

import com.google.common.collect.Iterators;
import org.junit.Test;
import org.sonar.test.TestUtils;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LoopbackAddressTest {

  @Test
  public void get() {
    assertThat(LoopbackAddress.get()).isNotNull();
    assertThat(LoopbackAddress.get().isLoopbackAddress()).isTrue();
    assertThat(LoopbackAddress.get().getHostAddress()).isNotNull();
  }

  @Test
  public void fail_to_get_loopback_address() {
    Enumeration<NetworkInterface> ifaces = Iterators.asEnumeration(Collections.<NetworkInterface>emptyList().iterator());
    try {
      LoopbackAddress.doGet(ifaces);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Impossible to get a IPv4 loopback address");
    }
  }

  @Test
  public void private_constructor() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(LoopbackAddress.class)).isTrue();
  }
}
