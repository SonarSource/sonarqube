/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.api.utils.System2;
import org.sonar.server.util.Paths2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ContainerSupportImplTest {
  private static final String CONTAINER_FILE_PATH = "/run/.containerenv";
  private static final String[] MOUNT_GREP_COMMAND = {"bash", "-c", "mount | grep 'overlay on /'"};
  private static final String[] CAT_COMMAND = {"bash", "-c", "cat /run/.containerenv"};
  private static final String DOCKER = "docker";
  private static final String PODMAN = "podman";
  private static final String BUILDAH = "buildah";
  private static final String CONTAINER_D = "containerd";
  private static final String GENERAL_CONTAINER = "general_container";

  private final Paths2 paths2 = mock(Paths2.class);
  private final System2 system2 = mock(System2.class);
  private ContainerSupportImpl underTest = new ContainerSupportImpl(paths2, system2);

  private String containerContext;

  public ContainerSupportImplTest(String containerContext) {
    this.containerContext = containerContext;
  }

  @Before
  public void setUp() {
    if (containerContext == null) {
      return;
    }

    switch (containerContext) {
      case DOCKER -> {
        underTest = spy(underTest);
        when(underTest.executeCommand(MOUNT_GREP_COMMAND)).thenReturn("/docker");
        when(paths2.exists("/.dockerenv")).thenReturn(true);
      }
      case PODMAN -> {
        when(system2.envVariable("container")).thenReturn("podman");
        when(paths2.exists(CONTAINER_FILE_PATH)).thenReturn(true);
      }
      case BUILDAH -> {
        underTest = spy(underTest);
        when(paths2.exists(CONTAINER_FILE_PATH)).thenReturn(true);
        when(underTest.executeCommand(CAT_COMMAND)).thenReturn("XXX engine=\"buildah- XXX");
      }
      case CONTAINER_D -> {
        underTest = spy(underTest);
        when(underTest.executeCommand(MOUNT_GREP_COMMAND)).thenReturn("/containerd");
      }
      case GENERAL_CONTAINER -> when(paths2.exists(CONTAINER_FILE_PATH)).thenReturn(true);
      default -> {
      }
    }
    underTest.populateCache();
  }

  @Parameterized.Parameters
  public static Collection<String> data() {
    return Arrays.asList(DOCKER, PODMAN, BUILDAH, CONTAINER_D, GENERAL_CONTAINER, null);
  }

  @Test
  public void testGetContainerContext() {
    Assert.assertEquals(containerContext, underTest.getContainerContext());
  }

  @Test
  public void testIsRunningInContainer() {
    boolean expected = containerContext != null;
    when(paths2.exists(CONTAINER_FILE_PATH)).thenReturn(expected);
    Assert.assertEquals(expected, underTest.isRunningInContainer());
  }

}
