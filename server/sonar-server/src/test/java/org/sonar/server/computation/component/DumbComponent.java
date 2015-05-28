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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Implementation of {@link Component} for unit tests.
 */
public class DumbComponent implements Component {
  private final Type type;
  private final int ref;
  private final String uuid;
  private final String key;
  private final List<Component> children;

  public DumbComponent(Type type, int ref, @Nullable String uuid, @Nullable String key, @Nullable Component... children) {
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

}
