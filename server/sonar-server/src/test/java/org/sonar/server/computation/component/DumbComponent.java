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
package org.sonar.server.computation.component;

import com.google.common.collect.ImmutableList;
import org.sonar.server.computation.context.ComputationContext;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.computation.measure.MeasureRepository;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DumbComponent implements Component {
  public static final Component DUMB_PROJECT = new DumbComponent(Type.PROJECT, 1, "PROJECT_KEY", "PROJECT_UUID");

  private static final String UNSUPPORTED_OPERATION_ERROR = "This node has no repository nor context";

  @CheckForNull
  private final ComputationContext context;
  private final Type type;
  private final int ref;
  private final String uuid;
  private final String key;
  private final List<Component> children;

  public DumbComponent(Type type, int ref, String uuid, String key, @Nullable Component... children) {
    this(null, type, ref, uuid, key, children);
  }

  public DumbComponent(@Nullable ComputationContext context, Type type, int ref, String uuid, String key, @Nullable Component... children) {
    this.context = context;
    this.type = type;
    this.ref = ref;
    this.uuid = uuid;
    this.key = key;
    this.children = children == null ? Collections.<Component>emptyList() : ImmutableList.copyOf(Arrays.asList(children));
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public int getRef() {
    return ref;
  }

  @Override
  public List<Component> getChildren() {
    return children;
  }

  @Override
  public ComputationContext getContext() {
    if (context == null) {
      throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_ERROR);
    }
    return context;
  }

  @Override
  public EventRepository getEventRepository() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_ERROR);
  }

  @Override
  public MeasureRepository getMeasureRepository() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_ERROR);
  }
}
