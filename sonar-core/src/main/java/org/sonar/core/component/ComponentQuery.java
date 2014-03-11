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
package org.sonar.core.component;

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @since 4.3
 */
public class ComponentQuery {

  private Collection<Long> ids;

  private Collection<String> qualifiers;

  private ComponentQuery() {
    this.ids = Sets.newHashSet();
    this.qualifiers = Sets.newHashSet();
  }

  public static ComponentQuery create() {
    return new ComponentQuery();
  }

  public Collection<Long> ids() {
    return Collections.unmodifiableCollection(ids);
  }

  public ComponentQuery addIds(Long... ids) {
    this.ids.addAll(Arrays.asList(ids));
    return this;
  }

  public Collection<String> qualifiers() {
    return Collections.unmodifiableCollection(qualifiers);
  }

  public ComponentQuery addQualifiers(String... qualifiers) {
    this.qualifiers.addAll(Arrays.asList(qualifiers));
    return this;
  }
}
