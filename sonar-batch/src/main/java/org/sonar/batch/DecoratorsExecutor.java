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
package org.sonar.batch;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.indexer.DefaultSonarIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DecoratorsExecutor implements CoreJob {

  private DecoratorsSelector decoratorsSelector;
  private DatabaseSession session;
  private ViolationsDao violationsDao;
  private static final Logger LOG = LoggerFactory.getLogger(DecoratorsExecutor.class);
  private DefaultSonarIndex index;

  public DecoratorsExecutor(BatchExtensionDictionnary extensionDictionnary, DefaultSonarIndex index, DatabaseSession session, ViolationsDao violationsDao) {
    this.decoratorsSelector = new DecoratorsSelector(extensionDictionnary);
    this.session = session;
    this.violationsDao = violationsDao;
    this.index = index;
  }


  public void execute(Project project, SensorContext sensorContext) {
    LoggerFactory.getLogger(DecoratorsExecutor.class).info("Execute decorators...");
    Collection<Decorator> decorators = decoratorsSelector.select(project);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Decorators: {}", StringUtils.join(decorators, " -> "));
    }

    decorateResource(project, decorators, true);
  }

  private DecoratorContext decorateResource(Resource resource, Collection<Decorator> decorators, boolean executeDecorators) {
    List<DecoratorContext> childrenContexts = new ArrayList<DecoratorContext>();
    for (Resource child : index.getChildren(resource)) {
      boolean isModule = (child instanceof Project);
      DefaultDecoratorContext childContext = (DefaultDecoratorContext) decorateResource(child, decorators, !isModule);
      childrenContexts.add(childContext.setReadOnly(true));
    }

    DefaultDecoratorContext context = new DefaultDecoratorContext(resource, index, childrenContexts, session, violationsDao);
    if (executeDecorators) {
      for (Decorator decorator : decorators) {
        decorator.decorate(resource, context);
      }
    }
    return context;
  }
}
