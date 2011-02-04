/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.squid.decorators;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.java.api.JavaMethod;

public final class FunctionsDecorator implements Decorator {

  public void decorate(Resource resource, DecoratorContext context) {
    if (Scopes.isType(resource)) {
      int methods=0, accessors=0;
      for (DecoratorContext child : context.getChildren()) {
        if (child.getResource() instanceof JavaMethod) {
          if (((JavaMethod)child.getResource()).isAccessor()) {
            accessors++;
          } else {
            methods++;
          }
        }
      }
      context.saveMeasure(new Measure(CoreMetrics.FUNCTIONS, (double)methods));
      context.saveMeasure(new Measure(CoreMetrics.ACCESSORS, (double)accessors));
    }
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }
}
