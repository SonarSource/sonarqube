/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.squid.bridges;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SquidUtils;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.indexer.QueryByType;

import java.util.Collection;
import java.util.HashMap;

public final class ResourceIndex extends HashMap<SourceCode, Resource> {

  public ResourceIndex    loadSquidResources(Squid squid, SensorContext context, Project project) {
    loadSquidProject(squid, project);
    loadSquidPackages(squid, context);
    loadSquidFiles(squid, context);
    return this;
  }

  private void loadSquidProject(Squid squid, Project project) {
    put(squid.getProject(), project);
  }

  private void loadSquidPackages(Squid squid, SensorContext context) {
    Collection<SourceCode> packages = squid.search(new QueryByType(SourcePackage.class));
    for (SourceCode squidPackage : packages) {
      JavaPackage sonarPackage = SquidUtils.convertJavaPackageKeyFromSquidFormat(squidPackage.getKey());
      context.saveResource(sonarPackage);
      put(squidPackage, context.getResource(sonarPackage)); // resource is reloaded to get the id
    }
  }

  private void loadSquidFiles(Squid squid, SensorContext context) {
    Collection<SourceCode> files = squid.search(new QueryByType(SourceFile.class));
    for (SourceCode squidFile : files) {
      JavaFile sonarFile = SquidUtils.convertJavaFileKeyFromSquidFormat(squidFile.getKey());
      context.saveResource(sonarFile);
      put(squidFile, context.getResource(sonarFile)); // resource is reloaded to get the id
    }
  }

}
