/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.api.utils.System2;
import org.sonar.server.util.Paths2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ContainerSupportImplTest {
  private final Paths2 paths2 = mock(Paths2.class);
  private final System2 system2 = mock(System2.class);
  private ContainerSupportImpl underTest = new ContainerSupportImpl(paths2, system2);

  private final ContainerEnvConfig containerContext;

  public ContainerSupportImplTest(ContainerEnvConfig containerContext) {
    this.containerContext = containerContext;
  }

  @Before
  public void setUp() {
    underTest = spy(underTest);
    when(underTest.readContainerenvFile()).thenReturn(
      containerContext.hasBuildahContainerenv ?
        "engine=\"buildah-" : "");
    when(paths2.exists("/run/.containerenv")).thenReturn(containerContext.hasContainerenvFile);
    when(paths2.exists("/.dockerenv")).thenReturn(containerContext.hasDockerEnvFile);
    when(system2.envVariable("container")).thenReturn(
      containerContext.hasPodmanEnvVariable ?
        "podman" : "");
    when(underTest.getMountOverlays()).thenReturn(containerContext.mountOverlays);
    when(system2.envVariable("IS_HELM_OPENSHIFT_ENABLED")).thenReturn(containerContext.isRunningOnOpenShiftEnvVariable);
    when(system2.envVariable("IS_HELM_AUTOSCALING_ENABLED")).thenReturn(containerContext.isHelmAutoscalingEnabledEnvVariable);
    underTest.populateCache();
  }

  @Parameterized.Parameters
  public static Collection<ContainerEnvConfig> data() {
    return List.of(ContainerEnvConfig.values());
  }

  @Test
  public void testGetContainerContext() {
    assertThat(underTest.getContainerContext())
      .isEqualTo(containerContext.expectedContainerContext);
  }

  @Test
  public void testIsRunningInContainer() {
    assertThat(underTest.isRunningInContainer())
      .isEqualTo(containerContext.expectedContainerContext != null);
  }

  @Test
  public void testIsRunningOnHelmOpenshift() {
    assertThat(underTest.isRunningOnHelmOpenshift())
      .isEqualTo(containerContext.expectedRunningOnOpenShift);
  }

  @Test
  public void testIsHelmAutoscalingEnabled() {
    assertThat(underTest.isHelmAutoscalingEnabled())
      .isEqualTo(containerContext.expectedHelmAutoscaling);
  }

  public enum ContainerEnvConfig {
    DOCKER("docker", false, true, false, false, "/docker", "false", false, "false", false),
    PODMAN("podman", true, false, true, false, "", "false", false, "false", false),
    BUILDAH("buildah", true, false, false, true, "", "false", false, "false", false),
    CONTAINER_D("containerd", false, false, false, false, "/containerd", "false", false, "false", false),
    GENERAL_CONTAINER("general_container", true, false, false, false, "", "false", false, "false", false),
    NONE(null, false, false, false, false, "", null, false, "false", false),
    OPENSHIFT(null, false, false, false, false, "", "true", true, "false", false),
    OPENSHIFT_SET_TO_FALSE(null, false, false, false, false, "", "false", false, "false", false),
    HELM_AUTOSCALING_ENABLED(null, false, false, false, false, "", "true", true, "true", true);

    final String expectedContainerContext;
    final boolean hasContainerenvFile;
    final boolean hasDockerEnvFile;
    final boolean hasPodmanEnvVariable;
    final boolean hasBuildahContainerenv;
    final String mountOverlays;
    final String isRunningOnOpenShiftEnvVariable;
    final boolean expectedRunningOnOpenShift;
    final String isHelmAutoscalingEnabledEnvVariable;
    final boolean expectedHelmAutoscaling;


    ContainerEnvConfig(@Nullable String expectedContainerContext, boolean hasContainerenvFile, boolean hasDockerEnvFile, boolean hasPodmanEnvVariable,
      boolean hasBuildahContainerenv, String mountOverlays, @Nullable String isRunningOnOpenShiftEnvVariable, boolean expectedRunningOnOpenShift,
      String isHelmAutoscalingEnabledEnvVariable, boolean expectedHelmAutoscaling) {
      this.expectedContainerContext = expectedContainerContext;
      this.hasContainerenvFile = hasContainerenvFile;
      this.hasDockerEnvFile = hasDockerEnvFile;
      this.hasPodmanEnvVariable = hasPodmanEnvVariable;
      this.hasBuildahContainerenv = hasBuildahContainerenv;
      this.mountOverlays = mountOverlays;
      this.isRunningOnOpenShiftEnvVariable = isRunningOnOpenShiftEnvVariable;
      this.expectedRunningOnOpenShift = expectedRunningOnOpenShift;
      this.isHelmAutoscalingEnabledEnvVariable = isHelmAutoscalingEnabledEnvVariable;
      this.expectedHelmAutoscaling = expectedHelmAutoscaling;
    }
  }

}
