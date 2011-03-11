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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.DecoratorsSelector;
import org.sonar.batch.DefaultDecoratorContext;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.MemoryOptimizer;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DecoratorsExecutor implements BatchComponent {

  private DecoratorsSelector decoratorsSelector;
  private DatabaseSession session;
  private static final Logger LOG = LoggerFactory.getLogger(DecoratorsExecutor.class);
  private DefaultIndex index;
  private MemoryOptimizer memoryOptimizer;

  public DecoratorsExecutor(BatchExtensionDictionnary extensionDictionnary, DefaultIndex index, DatabaseSession session,
                            MemoryOptimizer memoryOptimizer) {
    this.decoratorsSelector = new DecoratorsSelector(extensionDictionnary);
    this.session = session;
    this.index = index;
    this.memoryOptimizer = memoryOptimizer;
  }


  public void execute(Project project) {
    LoggerFactory.getLogger(DecoratorsExecutor.class).info("Execute decorators...");
    Collection<Decorator> decorators = decoratorsSelector.select(project);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Decorators: {}", StringUtils.join(decorators, " -> "));
    }

    DecoratorsProfiler profiler = new DecoratorsProfiler(decorators);
    decorateResource(project, decorators, true, profiler);
    session.commit();
    profiler.log();
  }

  private DecoratorContext decorateResource(Resource resource, Collection<Decorator> decorators, boolean executeDecorators, DecoratorsProfiler profiler) {
    List<DecoratorContext> childrenContexts = Lists.newArrayList();
    for (Resource child : index.getChildren(resource)) {
      boolean isModule = (child instanceof Project);
      DefaultDecoratorContext childContext = (DefaultDecoratorContext) decorateResource(child, decorators, !isModule, profiler);
      childrenContexts.add(childContext.setReadOnly(true));
    }

    DefaultDecoratorContext context = new DefaultDecoratorContext(resource, index, childrenContexts, session);
    if (executeDecorators) {
      for (Decorator decorator : decorators) {
        profiler.start(decorator);
        decorator.decorate(resource, context);
        memoryOptimizer.flushMemory();
        profiler.stop();
      }
    }
    return context;
  }


  static class DecoratorsProfiler {
    Collection<Decorator> decorators;
    Map<Decorator, Long> durations = new IdentityHashMap<Decorator, Long>();
    long startTime;
    Decorator currentDecorator;

    DecoratorsProfiler(Collection<Decorator> decorators) {
      this.decorators = decorators;
      for (Decorator decorator : decorators) {
        durations.put(decorator, 0L);
      }
    }

    void start(Decorator decorator) {
      this.startTime = System.currentTimeMillis();
      this.currentDecorator = decorator;
    }

    void stop() {
      Long cumulatedDuration = durations.get(currentDecorator);
      durations.put(currentDecorator, cumulatedDuration + (System.currentTimeMillis() - startTime));
    }

    void log() {
      LOG.debug(getMessage());
    }

    String getMessage() {
      StringBuilder sb = new StringBuilder("Decorator time:").append(SystemUtils.LINE_SEPARATOR);
      for (Decorator decorator : decorators) {
        sb.append("\t").append(decorator.toString()).append(": ").append(durations.get(decorator)).append("ms").append(SystemUtils.LINE_SEPARATOR);
      }
      return sb.toString();
    }
  }
}
