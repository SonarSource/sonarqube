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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.String.format;

/**
 * {@link CeTaskProcessorRepository} implementation which provides access to the {@link CeTaskProcessor} existing in the
 * PicoContainer the current object belongs to.
 */
public class CeTaskProcessorRepositoryImpl implements CeTaskProcessorRepository {
  private static final Joiner COMMA_JOINER = Joiner.on(", ");

  private final Map<String, CeTaskProcessor> taskProcessorByCeTaskType;

  public CeTaskProcessorRepositoryImpl(CeTaskProcessor[] taskProcessors) {
    this.taskProcessorByCeTaskType = indexTaskProcessors(taskProcessors);
  }

  @Override
  public Optional<CeTaskProcessor> getForCeTask(CeTask ceTask) {
    return Optional.ofNullable(taskProcessorByCeTaskType.get(ceTask.getType()));
  }

  private static Map<String, CeTaskProcessor> indexTaskProcessors(CeTaskProcessor[] taskProcessors) {
    Multimap<String, CeTaskProcessor> permissiveIndex = buildPermissiveCeTaskProcessorIndex(taskProcessors);
    checkUniqueHandlerPerCeTaskType(permissiveIndex);
    return ImmutableMap.copyOf(Maps.transformValues(permissiveIndex.asMap(), CeTaskProcessorCollectionToFirstElement.INSTANCE));
  }

  private static Multimap<String, CeTaskProcessor> buildPermissiveCeTaskProcessorIndex(CeTaskProcessor[] taskProcessors) {
    Multimap<String, CeTaskProcessor> permissiveIndex = ArrayListMultimap.create(taskProcessors.length, 1);
    for (CeTaskProcessor taskProcessor : taskProcessors) {
      for (String ceTaskType : taskProcessor.getHandledCeTaskTypes()) {
        permissiveIndex.put(ceTaskType, taskProcessor);
      }
    }
    return permissiveIndex;
  }

  private static void checkUniqueHandlerPerCeTaskType(Multimap<String, CeTaskProcessor> permissiveIndex) {
    for (Map.Entry<String, Collection<CeTaskProcessor>> entry : permissiveIndex.asMap().entrySet()) {
      checkArgument(
        entry.getValue().size() == 1,
        format(
          "There can be only one CeTaskProcessor instance registered as the processor for CeTask type %s. " +
            "More than one found. Please fix your configuration: %s",
          entry.getKey(),
          COMMA_JOINER.join(from(entry.getValue()).transform(ToClassName.INSTANCE).toSortedList(CASE_INSENSITIVE_ORDER))));
    }
  }

  private enum ToClassName implements Function<Object, String> {
    INSTANCE;

    @Override
    @Nonnull
    public String apply(@Nonnull Object input) {
      return input.getClass().getName();
    }
  }

  private enum CeTaskProcessorCollectionToFirstElement implements Function<Collection<CeTaskProcessor>, CeTaskProcessor> {
    INSTANCE;

    @Override
    @Nonnull
    public CeTaskProcessor apply(@Nonnull Collection<CeTaskProcessor> input) {
      return input.iterator().next();
    }
  }
}
