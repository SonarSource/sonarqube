/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.rule;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

/**
 * Aggregate which Quality profiles have been used on the current module.
 */
public class QProfileDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return project.getModules().size() > 0;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (!ResourceUtils.isProject(resource)) {
      return;
    }
    UsedQProfiles profiles = UsedQProfiles.empty();
    for (Measure childProfilesMeasure : context.getChildrenMeasures(CoreMetrics.PROFILES)) {
      UsedQProfiles childProfiles = UsedQProfiles.fromJSON(childProfilesMeasure.getData());
      profiles = profiles.merge(childProfiles);
    }

    Measure detailsMeasure = new Measure(CoreMetrics.PROFILES, profiles.toJSON());
    context.saveMeasure(detailsMeasure);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
