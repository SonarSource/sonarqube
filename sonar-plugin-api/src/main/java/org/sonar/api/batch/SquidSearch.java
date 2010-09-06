/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import java.util.Collection;

import org.sonar.api.BatchExtension;
import org.sonar.squid.api.Query;
import org.sonar.squid.api.SourceCode;

/**
 * The extension point to access the Squid data tree
 *
 * @since 1.11
 */
public interface SquidSearch extends BatchExtension {
  /**
   * Returns a list of SourceCode objects base a set of queries given
   *
   * @param query the set of query
   * @return SourceCode objects
   */
  Collection<SourceCode> search(Query... query);

  /**
   * Returns a SourceObject given its key
   *
   * @param key the key
   * @return SourceCode object
   */
  SourceCode search(String key);
}
