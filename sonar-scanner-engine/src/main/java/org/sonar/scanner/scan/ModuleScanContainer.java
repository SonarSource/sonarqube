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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.scanner.bootstrap.ExtensionInstaller;
import org.sonar.scanner.bootstrap.SensorExtensionDictionnary;
import org.sonar.scanner.deprecated.perspectives.ScannerPerspectives;
import org.sonar.scanner.phases.SensorsExecutor;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.ModuleInputComponentStore;
import org.sonar.scanner.sensor.DefaultSensorContext;
import org.sonar.scanner.sensor.SensorOptimizer;

import static org.sonar.api.batch.InstantiationStrategy.PER_PROJECT;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isDeprecatedScannerSide;
import static org.sonar.scanner.bootstrap.ExtensionUtils.isInstantiationStrategy;

public class ModuleScanContainer extends ComponentContainer {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleScanContainer.class);
  private final DefaultInputModule module;

  public ModuleScanContainer(ProjectScanContainer parent, DefaultInputModule module) {
    super(parent);
    this.module = module;
  }

  @Override
  protected void doBeforeStart() {
    LOG.info("-------------  Scan {}", module.definition().getName());
    addCoreComponents();
    addExtensions();
  }

  private void addCoreComponents() {
    add(
      module.definition(),
      module,
      MutableModuleSettings.class,
      new ModuleConfigurationProvider(),

      SensorsExecutor.class,

      // file system
      ModuleInputComponentStore.class,
      FileExclusions.class,
      DefaultModuleFileSystem.class,

      SensorOptimizer.class,

      DefaultSensorContext.class,
      SensorExtensionDictionnary.class,

      // Perspectives
      ScannerPerspectives.class);
  }

  private void addExtensions() {
    ExtensionInstaller pluginInstaller = getComponentByType(ExtensionInstaller.class);
    pluginInstaller.install(this, e -> isDeprecatedScannerSide(e) && isInstantiationStrategy(e, PER_PROJECT));
  }

  @Override
  protected void doAfterStart() {
    getComponentByType(SensorsExecutor.class).execute();
  }

}
