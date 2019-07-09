/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.scan;

import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.deprecated.perspectives.ScannerPerspectives;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.ModuleInputComponentStore;
import org.sonar.scanner.sensor.ModuleSensorContext;
import org.sonar.scanner.sensor.ModuleSensorExtensionDictionnary;
import org.sonar.scanner.sensor.ModuleSensorOptimizer;
import org.sonar.scanner.sensor.ModuleSensorsExecutor;

import static org.sonar.api.batch.InstantiationStrategy.PER_PROJECT;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isDeprecatedScannerSide;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isInstantiationStrategy;

public class ModuleScanContainer extends ComponentContainer {
  private final DefaultInputModule module;

  public ModuleScanContainer(ProjectScanContainer parent, DefaultInputModule module) {
    super(parent);
    this.module = module;
  }

  @Override
  protected void doBeforeStart() {
    addCoreComponents();
    addExtensions();
  }

  private void addCoreComponents() {
    add(
      module.definition(),
      module,
      MutableModuleSettings.class,
      new ModuleConfigurationProvider(),

      ModuleSensorsExecutor.class,

      // file system
      ModuleInputComponentStore.class,
      FileExclusions.class,
      DefaultModuleFileSystem.class,

      ModuleSensorOptimizer.class,

      ModuleSensorContext.class,
      ModuleSensorExtensionDictionnary.class,

      // Perspectives
      ScannerPerspectives.class);
  }

  private void addExtensions() {
    ExtensionInstaller pluginInstaller = getComponentByType(ExtensionInstaller.class);
    pluginInstaller.install(this, e -> isDeprecatedScannerSide(e) && isInstantiationStrategy(e, PER_PROJECT));
  }

  @Override
  protected void doAfterStart() {
    getComponentByType(ModuleSensorsExecutor.class).execute();
  }

}
