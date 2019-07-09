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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.scanner.scan.filesystem.ScannerComponentIdGenerator;

public class InputModuleHierarchyProvider extends ProviderAdapter {

  private static final Logger LOG = Loggers.get(InputModuleHierarchyProvider.class);

  private DefaultInputModuleHierarchy hierarchy = null;

  public DefaultInputModuleHierarchy provide(ScannerComponentIdGenerator scannerComponentIdGenerator, DefaultInputProject project) {
    if (hierarchy == null) {
      LOG.debug("Creating module hierarchy");
      DefaultInputModule root = createModule(project.definition(), project.scannerId());
      Map<DefaultInputModule, DefaultInputModule> parents = createChildren(root, scannerComponentIdGenerator, new HashMap<>());
      if (parents.isEmpty()) {
        hierarchy = new DefaultInputModuleHierarchy(root);
      } else {
        hierarchy = new DefaultInputModuleHierarchy(root, parents);
      }
    }
    return hierarchy;
  }

  private static Map<DefaultInputModule, DefaultInputModule> createChildren(DefaultInputModule parent, ScannerComponentIdGenerator scannerComponentIdGenerator,
    Map<DefaultInputModule, DefaultInputModule> parents) {
    for (ProjectDefinition def : parent.definition().getSubProjects()) {
      DefaultInputModule child = createModule(def, scannerComponentIdGenerator.getAsInt());
      parents.put(child, parent);
      createChildren(child, scannerComponentIdGenerator, parents);
    }
    return parents;
  }

  private static DefaultInputModule createModule(ProjectDefinition def, int scannerComponentId) {
    LOG.debug("  Init module '{}'", def.getName());
    DefaultInputModule module = new DefaultInputModule(def, scannerComponentId);
    LOG.debug("    Base dir: {}", module.getBaseDir().toAbsolutePath().toString());
    LOG.debug("    Working dir: {}", module.getWorkDir().toAbsolutePath().toString());
    LOG.debug("    Module global encoding: {}, default locale: {}", module.getEncoding().displayName(), Locale.getDefault());
    return module;
  }

}
