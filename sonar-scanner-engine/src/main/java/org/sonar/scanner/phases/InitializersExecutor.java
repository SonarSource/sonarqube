/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.phases;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.events.EventBus;

import com.google.common.collect.Lists;

public class InitializersExecutor {

  private static final Logger LOG = Loggers.get(SensorsExecutor.class);

  private final DefaultInputModule module;
  private final ScannerExtensionDictionnary selector;
  private final EventBus eventBus;

  public InitializersExecutor(ScannerExtensionDictionnary selector, DefaultInputModule module, EventBus eventBus) {
    this.selector = selector;
    this.module = module;
    this.eventBus = eventBus;
  }

  public void execute() {
    Collection<Initializer> initializers = selector.select(Initializer.class, module, true, null);
    eventBus.fireEvent(new InitializersPhaseEvent(Lists.newArrayList(initializers), true));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Initializers : {}", StringUtils.join(initializers, " -> "));
    }

    Project project = new Project(module);
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
