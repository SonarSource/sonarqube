/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.CeTaskResult;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * A {@link org.junit.Rule} that implements the {@link CeTaskProcessorRepository} interface and
 * requires consumer to explicitly define if a specific Task type has an associated {@link CeTaskProcessor} or not.
 */
public class CeTaskProcessorRepositoryRule extends ExternalResource implements CeTaskProcessorRepository {

  private final Map<String, CeTaskProcessor> index = new HashMap<>();

  @Override
  protected void after() {
    index.clear();
  }

  public CeTaskProcessorRepositoryRule setNoProcessorForTask(String taskType) {
    index.put(requireNonNull(taskType), NoCeTaskProcessor.INSTANCE);
    return this;
  }

  public CeTaskProcessorRepositoryRule setProcessorForTask(String taskType, CeTaskProcessor taskProcessor) {
    index.put(requireNonNull(taskType), requireNonNull(taskProcessor));
    return this;
  }

  @Override
  public Optional<CeTaskProcessor> getForCeTask(CeTask ceTask) {
    CeTaskProcessor taskProcessor = index.get(ceTask.getType());
    checkState(taskProcessor != null, "CeTaskProcessor was not set in rule for task %s", ceTask);
    return taskProcessor instanceof NoCeTaskProcessor ? Optional.empty() : Optional.of(taskProcessor);
  }

  private enum NoCeTaskProcessor implements CeTaskProcessor {
    INSTANCE;

    private static final String UOE_MESSAGE = "NoCeTaskProcessor does not implement any method since it not supposed to be ever used";

    @Override
    public Set<String> getHandledCeTaskTypes() {
      throw new UnsupportedOperationException(UOE_MESSAGE);
    }

    @Override
    public CeTaskResult process(CeTask task) {
      throw new UnsupportedOperationException(UOE_MESSAGE);
    }
  }
}
