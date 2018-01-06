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
package org.sonar.db;

/**
 * The RuntimeException thrown by default when a element is not found at the DAO layer.
 * When selecting by id or key, the methods respect one of the following pattern:
 * <ul>
 *   <li>selectOrFailByKey return the element or throws a RowNotFoundException</li>
 *   <li>selectByUuid return an Optional (now) or a nullable element (legacy)</li>
 * </ul>
 */
public class RowNotFoundException extends RuntimeException {
  public RowNotFoundException(String message) {
    super(message);
  }
}
