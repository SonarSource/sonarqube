/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Collection;

/**
 * @since 2.2
 */
public final class FilesDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public Metric generateDirectoriesMetric() {
    return CoreMetrics.FILES;
  }

  @SuppressWarnings("rawtypes")
  public void decorate(Resource resource, DecoratorContext context) {
    if (MeasureUtils.hasValue(context.getMeasure(CoreMetrics.FILES))) {
      checkRootProjectHasFiles(resource, context.getMeasure(CoreMetrics.FILES).getValue());
      return;
    }

    if (Resource.QUALIFIER_CLASS.equals(resource.getQualifier()) || Resource.QUALIFIER_FILE.equals(resource.getQualifier())) {
      context.saveMeasure(CoreMetrics.FILES, 1.0);

    } else {
      Collection<Measure> childrenMeasures = context.getChildrenMeasures(CoreMetrics.FILES);
      Double sum = MeasureUtils.sum(false, childrenMeasures);
      checkRootProjectHasFiles(resource, sum);
      if (sum != null) {
        context.saveMeasure(CoreMetrics.FILES, sum);
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private void checkRootProjectHasFiles(Resource resource, Double sum) {
    if (ResourceUtils.isRootProject(resource) && (sum == null || sum.doubleValue() == 0)) {
      String sourceFoldersList = printSourceFoldersList((Project) resource);
      throw new SonarException("Project \"" + resource.getName() + "\" does not contain any file in its source folders:\n" +
        sourceFoldersList + "\nPlease check your project configuration.");
    }
  }

  private String printSourceFoldersList(Project project) {
    StringBuilder result = new StringBuilder();
    for (File sourceDir : project.getFileSystem().getSourceDirs()) {
      result.append("   - ");
      result.append(sourceDir.getAbsolutePath());
      result.append("\n");
    }
    return result.toString();
  }
}
