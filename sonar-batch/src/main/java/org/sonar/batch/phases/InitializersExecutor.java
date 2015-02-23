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
package org.sonar.batch.phases;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Initializer;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.batch.bootstrap.BatchExtensionDictionnary;
import org.sonar.batch.events.EventBus;

import java.util.Collection;

public class InitializersExecutor {

  private static final Logger LOG = Loggers.get(SensorsExecutor.class);

  private Project project;
  private BatchExtensionDictionnary selector;
  private EventBus eventBus;

  public InitializersExecutor(BatchExtensionDictionnary selector, Project project, EventBus eventBus) {
    this.selector = selector;
    this.project = project;
    this.eventBus = eventBus;
  }

  public void execute() {
    Collection<Initializer> initializers = selector.select(Initializer.class, project, true, null);
    eventBus.fireEvent(new InitializersPhaseEvent(Lists.newArrayList(initializers), true));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Initializers : {}", StringUtils.join(initializers, " -> "));
    }

    for (Initializer initializer : initializers) {
      eventBus.fireEvent(new InitializerExecutionEvent(initializer, true));

      Profiler profiler = Profiler.create(LOG).startInfo("Initializer " + initializer);
      initializer.execute(project);
      profiler.stopInfo();
      eventBus.fireEvent(new InitializerExecutionEvent(initializer, false));
    }

    eventBus.fireEvent(new InitializersPhaseEvent(Lists.newArrayList(initializers), false));
  }

}
