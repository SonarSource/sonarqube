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
package org.sonar.server.platform.platformlevel;

import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.server.platform.WebServer;

import static org.assertj.core.api.Assertions.assertThat;

public class PlatformLevelTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private PlatformLevel underTest = new PlatformLevel("name") {

    @Override
    protected void configureLevel() {

    }
  };

  @Test
  public void addIfStartupLeader_throws_ISE_if_container_does_not_have_WebServer_object() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("WebServer not available in Pico yet");

    underTest.addIfStartupLeader();
  }

  @Test
  public void addIfStartupLeader_always_returns_the_same_instance() {
    underTest.add(Mockito.mock(WebServer.class));

    PlatformLevel.AddIfStartupLeader addIfStartupLeader = underTest.addIfStartupLeader();
    IntStream.range(0, 1 + new Random().nextInt(4)).forEach(i -> assertThat(underTest.addIfStartupLeader()).isSameAs(addIfStartupLeader));
  }

  @Test
  public void addIfCluster_throws_ISE_if_container_does_not_have_WebServer_object() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("WebServer not available in Pico yet");

    underTest.addIfCluster();
  }

  @Test
  public void addIfCluster_always_returns_the_same_instance() {
    underTest.add(Mockito.mock(WebServer.class));

    PlatformLevel.AddIfCluster addIfCluster = underTest.addIfCluster();
    IntStream.range(0, 1 + new Random().nextInt(4)).forEach(i -> assertThat(underTest.addIfCluster()).isSameAs(addIfCluster));
  }

  @Test
  public void addIfStandalone_throws_ISE_if_container_does_not_have_WebServer_object() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("WebServer not available in Pico yet");

    underTest.addIfCluster();
  }

  @Test
  public void addIfStandalone_always_returns_the_same_instance() {
    underTest.add(Mockito.mock(WebServer.class));

    PlatformLevel.AddIfCluster addIfCluster = underTest.addIfCluster();
    IntStream.range(0, 1 + new Random().nextInt(4)).forEach(i -> assertThat(underTest.addIfCluster()).isSameAs(addIfCluster));
  }
}
