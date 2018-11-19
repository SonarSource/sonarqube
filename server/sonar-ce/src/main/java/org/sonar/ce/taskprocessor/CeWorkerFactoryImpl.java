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
package org.sonar.ce.taskprocessor;

import java.util.HashSet;
import java.util.Set;
import org.sonar.ce.queue.InternalCeQueue;
import org.sonar.core.util.UuidFactory;

import static com.google.common.collect.ImmutableSet.copyOf;

public class CeWorkerFactoryImpl implements CeWorkerFactory {
  private final UuidFactory uuidFactory;
  private final Set<String> ceWorkerUUIDs = new HashSet<>();
  private final InternalCeQueue queue;
  private final CeTaskProcessorRepository taskProcessorRepository;
  private final EnabledCeWorkerController enabledCeWorkerController;
  private final CeWorker.ExecutionListener[] executionListeners;

  /**
   * Used by Pico when there is no {@link CeWorker.ExecutionListener} in the container.
   */
  public CeWorkerFactoryImpl(InternalCeQueue queue, CeTaskProcessorRepository taskProcessorRepository,
    UuidFactory uuidFactory, EnabledCeWorkerController enabledCeWorkerController) {
    this(queue, taskProcessorRepository, uuidFactory, enabledCeWorkerController, new CeWorker.ExecutionListener[0]);
  }

  public CeWorkerFactoryImpl(InternalCeQueue queue, CeTaskProcessorRepository taskProcessorRepository,
    UuidFactory uuidFactory, EnabledCeWorkerController enabledCeWorkerController,
    CeWorker.ExecutionListener[] executionListeners) {
    this.queue = queue;
    this.taskProcessorRepository = taskProcessorRepository;
    this.uuidFactory = uuidFactory;
    this.enabledCeWorkerController = enabledCeWorkerController;
    this.executionListeners = executionListeners;
  }

  @Override
  public CeWorker create(int ordinal) {
    String uuid = uuidFactory.create();
    ceWorkerUUIDs.add(uuid);
    return new CeWorkerImpl(ordinal, uuid, queue, taskProcessorRepository, enabledCeWorkerController, executionListeners);
  }

  @Override
  public Set<String> getWorkerUUIDs() {
    return copyOf(ceWorkerUUIDs);
  }
}
