/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.NetworkUtils;
import org.sonar.process.NetworkUtilsImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class StartableHazelcastMemberTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();
  private String loopback = InetAddress.getLoopbackAddress().getHostAddress();

  @Test
  public void start_initializes_hazelcast() {
    completeValidSettings();
    StartableHazelcastMember underTest = new StartableHazelcastMember(settings.asConfig(), NetworkUtilsImpl.INSTANCE);
    verifyStopped(underTest);

    underTest.start();

    assertThat(underTest.getUuid()).isNotEmpty();
    assertThat(underTest.getCluster().getMembers()).hasSize(1);
    assertThat(underTest.getMemberUuids()).containsExactly(underTest.getUuid());
    assertThat(underTest.getSet("foo")).isNotNull();
    assertThat(underTest.getReplicatedMap("foo")).isNotNull();
    assertThat(underTest.getAtomicReference("foo")).isNotNull();
    assertThat(underTest.getList("foo")).isNotNull();
    assertThat(underTest.getMap("foo")).isNotNull();
    assertThat(underTest.getLock("foo")).isNotNull();
    assertThat(underTest.getClusterTime()).isGreaterThan(0);

    underTest.stop();

    verifyStopped(underTest);
  }

  @Test
  public void throw_ISE_if_host_for_random_port_cant_be_resolved() throws Exception{
    NetworkUtils network = mock(NetworkUtils.class);
    doThrow(new UnknownHostException("BOOM")).when(network).toInetAddress(anyString());
    completeValidSettings();
    StartableHazelcastMember underTest = new StartableHazelcastMember(settings.asConfig(), network);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not resolve address ");

    underTest.start();

    verifyStopped(underTest);
  }

  private void completeValidSettings() {
    settings.setProperty("sonar.cluster.name", "foo");
    settings.setProperty("sonar.cluster.node.host", loopback);
    settings.setProperty("sonar.cluster.node.name", "bar");
    settings.setProperty("sonar.cluster.node.type", "application");
    settings.setProperty("process.key", "ce");
  }

  private static void verifyStopped(StartableHazelcastMember member) {
    expectNpe(member::getMemberUuids);
    expectNpe(member::getCluster);
    expectNpe(member::getUuid);
  }

  private static void expectNpe(Supplier supplier) {
    try {
      supplier.get();
      fail();
    } catch (NullPointerException e) {
    }
  }

}
