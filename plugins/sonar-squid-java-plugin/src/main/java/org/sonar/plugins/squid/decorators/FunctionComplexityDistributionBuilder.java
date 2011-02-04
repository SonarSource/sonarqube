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
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;
import org.sonar.java.api.JavaMethod;

/**
 * @since 2.6
 */
public final class FunctionComplexityDistributionBuilder implements Decorator {

  public static final Number[] LIMITS = {1, 2, 4, 6, 8, 10, 12};

  @DependsUpon
  public Metric dependOnComplexity() {
    return CoreMetrics.COMPLEXITY;
  }

  @DependedUpon
  public Metric generatesFunctionComplexityDistribution() {
    return CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldExecuteOn(resource, context)) {
      RangeDistributionBuilder builder = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, LIMITS);
      for (DecoratorContext childContext : context.getChildren()) {
        if (childContext.getResource() instanceof JavaMethod) {
          JavaMethod javaMethod = (JavaMethod)childContext.getResource();
          Measure complexity = childContext.getMeasure(CoreMetrics.COMPLEXITY);
          if (!javaMethod.isAccessor() && complexity != null) {
            builder.add(complexity.getValue());
          }
        }
      }
      Measure measure = builder.build(true);
      measure.setPersistenceMode(PersistenceMode.MEMORY);
      context.saveMeasure(measure);
    }
  }

  boolean shouldExecuteOn(Resource resource, DecoratorContext context) {
    return Scopes.isType(resource) && context.getMeasure(CoreMetrics.COMPLEXITY) != null;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }
}

