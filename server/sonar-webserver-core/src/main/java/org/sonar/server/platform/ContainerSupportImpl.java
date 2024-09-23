/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.server.util.Paths2;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ContainerSupportImpl implements ContainerSupport {

  private static final Logger LOG = LoggerFactory.getLogger(ContainerSupportImpl.class);
  private static final String CONTAINER_FILE_PATH = "/run/.containerenv";
  private static final String DOCKER = "docker";
  private static final String PODMAN = "podman";
  private static final String BUILDAH = "buildah";
  private static final String CONTAINER_D = "containerd";
  private static final String GENERAL_CONTAINER = "general_container";
  private static final String IS_HELM_OPENSHIFT_ENABLED = "IS_HELM_OPENSHIFT_ENABLED";
  private static final String IS_HELM_AUTOSCALING_ENABLED = "IS_HELM_AUTOSCALING_ENABLED";

  private static final String[] MOUNT_GREP_COMMAND = {"bash", "-c", "mount | grep 'overlay on /'"};
  private static final String[] CAT_COMMAND = {"bash", "-c", "cat /run/.containerenv"};

  private final System2 system2;
  private final Paths2 paths2;
  private String containerContextCache;

  public ContainerSupportImpl(Paths2 paths2, System2 system2) {
    this.paths2 = paths2;
    this.system2 = system2;

    populateCache();
  }

  @VisibleForTesting
  void populateCache() {
    if (isDocker()) {
      containerContextCache = DOCKER;
    } else if (isPodman()) {
      containerContextCache = PODMAN;
    } else if (isBuildah()) {
      containerContextCache = BUILDAH;
    } else if (isContainerd()) {
      containerContextCache = CONTAINER_D;
    } else if (isGeneralContainer()) {
      containerContextCache = GENERAL_CONTAINER;
    } else {
      containerContextCache = null;
    }
  }

  @Override
  public boolean isRunningInContainer() {
    return containerContextCache != null;
  }

  @Override
  public String getContainerContext() {
    return containerContextCache;
  }

  @Override
  public boolean isRunningOnHelmOpenshift() {
    return "true".equals(system2.envVariable(IS_HELM_OPENSHIFT_ENABLED));
  }

  @Override
  public boolean isHelmAutoscalingEnabled() {
    return "true".equals(system2.envVariable(IS_HELM_AUTOSCALING_ENABLED));
  }

  private boolean isDocker() {
    return getMountOverlays().contains("/docker") && paths2.exists("/.dockerenv");
  }

  private boolean isPodman() {
    return Objects.equals(system2.envVariable("container"), PODMAN) && paths2.exists(CONTAINER_FILE_PATH);
  }

  private boolean isBuildah() {
    return paths2.exists(CONTAINER_FILE_PATH) && readContainerenvFile().contains("engine=\"buildah-");
  }

  private boolean isContainerd() {
    return getMountOverlays().contains("/containerd");
  }

  private boolean isGeneralContainer() {
    return paths2.exists(CONTAINER_FILE_PATH);
  }

  @VisibleForTesting
  String getMountOverlays() {
    return executeCommand(MOUNT_GREP_COMMAND);
  }

  @VisibleForTesting
  String readContainerenvFile() {
    return executeCommand(CAT_COMMAND);
  }

  private static String executeCommand(String[] command) {
    try {
      Process process = new ProcessBuilder().command(command).start();
      try (Scanner scanner = new Scanner(process.getInputStream(), UTF_8)) {
        scanner.useDelimiter("\n");
        return scanner.next();
      } finally {
        process.destroy();
      }
    } catch (Exception e) {
      LOG.debug("Failed to execute command", e);
      return "";
    }
  }
}
