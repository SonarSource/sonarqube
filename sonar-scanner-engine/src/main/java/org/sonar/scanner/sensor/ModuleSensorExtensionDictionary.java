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
package org.sonar.scanner.sensor;

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.scanner.bootstrap.AbstractExtensionDictionary;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.MutableFileSystem;

public class ModuleSensorExtensionDictionary extends AbstractExtensionDictionary {

  private final ModuleSensorContext sensorContext;
  private final ModuleSensorOptimizer sensorOptimizer;
  private final MutableFileSystem fileSystem;
  private final BranchConfiguration branchConfiguration;

  public ModuleSensorExtensionDictionary(ExtensionContainer componentContainer, ModuleSensorContext sensorContext, ModuleSensorOptimizer sensorOptimizer,
    MutableFileSystem fileSystem, BranchConfiguration branchConfiguration) {
    super(componentContainer);
    this.sensorContext = sensorContext;
    this.sensorOptimizer = sensorOptimizer;
    this.fileSystem = fileSystem;
    this.branchConfiguration = branchConfiguration;
  }

  public Collection<ModuleSensorWrapper> selectSensors(boolean global) {
    Collection<Sensor> result = sort(getFilteredExtensions(Sensor.class, null));
    return result.stream()
      .map(s -> new ModuleSensorWrapper(s, sensorContext, sensorOptimizer, fileSystem, branchConfiguration))
      .filter(s -> global == s.isGlobal() && s.shouldExecute())
      .collect(Collectors.toList());
  }
}
