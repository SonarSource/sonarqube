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

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import org.sonar.server.util.Paths2;

public class DockerSupportImpl implements DockerSupport {
  private final Paths2 paths2;

  public DockerSupportImpl(Paths2 paths2) {
    this.paths2 = paths2;
  }

  @Override
  public boolean isRunningInDocker() {
    if (paths2.exists("/run/.containerenv")) {
      return true;
    }
    try (Stream<String> stream = Files.lines(paths2.get("/proc/1/cgroup"))) {
      return stream.anyMatch(line -> line.contains("/docker") || line.contains("/kubepods") || line.contains("containerd.service") );
    } catch (IOException e) {
      return false;
    }
  }
}
