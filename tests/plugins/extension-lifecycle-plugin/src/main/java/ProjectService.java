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
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;

/**
 * As many instances as projects (maven modules)
 */
public class ProjectService implements BatchExtension {

  private BatchService batchService;
  private Settings settings;

  public ProjectService(BatchService batchService, Settings settings) {
    this.batchService = batchService;
    this.settings = settings;
  }

  public void start() {
    if (!settings.getBoolean("extension.lifecycle")) {
      return;
    }
    System.out.println("Start ProjectService");

    if (!batchService.isStarted()) {
      throw new IllegalStateException("ProjectService must be started after BatchService");
    }
    batchService.incrementProjectService();
  }

  public void stop() {
    if (!settings.getBoolean("extension.lifecycle")) {
      return;
    }
    System.out.println("Stop ProjectService");
    if (!batchService.isStarted()) {
      System.out.println("ProjectService must be stopped before BatchService");
      System.exit(1);
    }
  }
}
