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
package org.sonar.api;

import org.sonar.api.batch.ScannerSide;

/**
 * Marker interface for all the components available in container of batch (code analyzer). Note that
 * injection of dependencies by constructor is used :
 * <pre>
 *   public class Foo implements BatchComponent {
 *
 *   }
 *   public class Bar implements BatchComponent {
 *     private final Foo foo;
 *     public Bar(Foo f) {
 *       this.foo = f;
 *     }
 *   }
 *
 * </pre>
 *
 * @since 2.2
 * @deprecated since 5.2 use {@link ScannerSide} annotation
 */
@Deprecated
@ScannerSide
public interface BatchComponent {
}
