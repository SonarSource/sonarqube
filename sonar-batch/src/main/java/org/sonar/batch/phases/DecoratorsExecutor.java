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
package org.sonar.batch.phases;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.DecoratorsSelector;
import org.sonar.batch.DefaultDecoratorContext;
import org.sonar.batch.events.DecoratorExecutionEvent;
import org.sonar.batch.events.DecoratorsPhaseEvent;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;

public class DecoratorsExecutor implements BatchComponent {

  private DecoratorsSelector decoratorsSelector;
  private DatabaseSession session;
  private DefaultIndex index;
  private EventBus eventBus;

  public DecoratorsExecutor(BatchExtensionDictionnary extensionDictionnary, DefaultIndex index, DatabaseSession session, EventBus eventBus) {
    this.decoratorsSelector = new DecoratorsSelector(extensionDictionnary);
    this.session = session;
    this.index = index;
    this.eventBus = eventBus;
  }

  public void execute(Project project) {
    Collection<Decorator> decorators = decoratorsSelector.select(project);
    eventBus.fireEvent(new DecoratorsPhaseEvent(decorators, true));
    decorateResource(project, decorators, true);
    eventBus.fireEvent(new DecoratorsPhaseEvent(decorators, false));
  }

  private DecoratorContext decorateResource(Resource resource, Collection<Decorator> decorators, boolean executeDecorators) {
    List<DecoratorContext> childrenContexts = Lists.newArrayList();
    for (Resource child : index.getChildren(resource)) {
      boolean isModule = (child instanceof Project);
      DefaultDecoratorContext childContext = (DefaultDecoratorContext) decorateResource(child, decorators, !isModule);
      childrenContexts.add(childContext.setReadOnly(true));
    }

    DefaultDecoratorContext context = new DefaultDecoratorContext(resource, index, childrenContexts, session);
    if (executeDecorators) {
      for (Decorator decorator : decorators) {
        eventBus.fireEvent(new DecoratorExecutionEvent(decorator, true));
        decorator.decorate(resource, context);
        eventBus.fireEvent(new DecoratorExecutionEvent(decorator, false));
      }
    }
    return context;
  }

}
