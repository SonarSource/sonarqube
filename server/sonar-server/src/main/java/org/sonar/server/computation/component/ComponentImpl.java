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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;
import static java.util.Objects.requireNonNull;

public class ComponentImpl implements Component {
  private final ComputationContext context;
  private final Type type;
  private final BatchReport.Component component;
  private final List<Component> children;
  private final EventRepository eventRepository = new SetEventRepository();

  // Mutable values
  private String key;
  private String uuid;

  public ComponentImpl(ComputationContext context, BatchReport.Component component, @Nullable Iterable<Component> children) {
    this.context = context;
    this.component = component;
    this.type = convertType(component.getType());
    this.children = children == null ? Collections.<Component>emptyList() : copyOf(filter(children, notNull()));
  }

  private static Type convertType(Constants.ComponentType type) {
    switch (type) {
      case PROJECT:
        return Type.PROJECT;
      case MODULE:
        return Type.MODULE;
      case DIRECTORY:
        return Type.DIRECTORY;
      case FILE:
        return Type.FILE;
      default:
        throw new IllegalArgumentException("Unsupported Constants.ComponentType value " + type);
    }
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public int getRef() {
    return component.getRef();
  }

  public String getUuid() {
    if (uuid == null) {
      throw new UnsupportedOperationException(String.format("Component uuid of ref '%s' has not be fed yet", getRef()));
    }
    return uuid;
  }

  public ComponentImpl setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getKey() {
    if (key == null) {
      throw new UnsupportedOperationException(String.format("Component key of ref '%s' has not be fed yet", getRef()));
    }
    return key;
  }

  public ComponentImpl setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public List<Component> getChildren() {
    return children;
  }

  @Override
  public org.sonar.server.computation.context.ComputationContext getContext() {
    return context;
  }

  @Override
  public EventRepository getEventRepository() {
    return eventRepository;
  }

  private static class SetEventRepository implements EventRepository {
    private final Set<Event> events = new HashSet<>();

    @Override
    public void add(Event event) {
      events.add(requireNonNull(event));
    }

    @Override
    public Iterable<Event> getEvents() {
      return events;
    }
  }

}
