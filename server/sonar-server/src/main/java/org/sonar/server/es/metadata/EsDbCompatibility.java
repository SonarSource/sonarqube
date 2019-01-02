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
package org.sonar.server.es.metadata;

/**
 * Checks when Elasticsearch indices must be dropped because
 * of changes in database
 */
public interface EsDbCompatibility {

  /**
   * Whether the effective DB vendor equals the vendor
   * registered in Elasticsearch metadata.
   * Return {@code false} if at least one of the values is absent
   */
  boolean hasSameDbVendor();

  /**
   * Stores in Elasticsearch the metadata about database
   */
  void markAsCompatible();
}
