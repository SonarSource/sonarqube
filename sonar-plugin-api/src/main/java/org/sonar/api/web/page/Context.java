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
package org.sonar.api.web.page;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;

/**
 * @see PageDefinition
 * @since 6.3
 */
public final class Context {
  private final Map<String, Page> pagesByPath = new HashMap<>();

  public Context addPage(Page page) {
    Page existingPageWithSameKey = pagesByPath.putIfAbsent(page.getKey(), page);
    if (existingPageWithSameKey != null) {
      throw new IllegalArgumentException(format("Page '%s' cannot be loaded. Another page with key '%s' already exists.", page.getName(), page.getKey()));
    }

    return this;
  }

  public Collection<Page> getPages() {
    return unmodifiableCollection(pagesByPath.values());
  }
}
