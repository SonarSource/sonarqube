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
package org.sonarsource.api.sonarlint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for all the components available in container of sonarlint. Note that
 * injection of dependencies by constructor is used :
 * <pre>
 *   {@literal @}SonarLintSide
 *   public class Foo {
 *
 *   }
 *   {@literal @}SonarLintSide
 *   public class Bar {
 *     private final Foo foo;
 *     public Bar(Foo f) {
 *       this.foo = f;
 *     }
 *   }
 *
 * </pre>
 *
 * @since 6.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SonarLintSide {

  /**
   * The component will be instantiated for each analysis (could be single or multiple files analysis).
   */
  String SINGLE_ANALYSIS = "SINGLE_ANALYSIS";

  /**
   * The component will be instantiated once and reused by all analyses, as long as the SonarLint engine is not restarted.
   */
  String MULTIPLE_ANALYSES = "MULTIPLE_ANALYSES";

  /**
   * Control the lifecycle of the component in the IoC container.
   * @since 7.0
   */
  String lifespan() default SINGLE_ANALYSIS;

}
