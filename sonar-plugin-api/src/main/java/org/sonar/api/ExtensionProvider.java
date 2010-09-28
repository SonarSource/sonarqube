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
package org.sonar.api;

import org.picocontainer.injectors.Provider;

/**
 * Factory of extensions. It allows to dynamically create extensions depending upon runtime context. A use-case is
 * to create one rule repository by language.
 *
 * <p>Constraints are :
 * <ul>
 *   <li>the factory is declared in Plugin.getExtensions() as an instance but not as a class</li>
 *   <li>the factory must have a public method named "provide()"</li>
 *   <li>the method provide() must return an object or an array of objects. Collections and classes are excluded.</li>
 *   <li>the methode provide() can accept parameters. These parameters are IoC dependencies.
 * </ul>
 * </p>
 *
 * <p>Example:
 * <pre>
 * public class RuleRepositoryProvider extends ExtensionProvider {
 *   public RuleRepository[] provide(Language[] languages) {
 *     RuleRepository[] result = new RuleRepository[languages.length];
 *     for(int index=0; index &lt; languages.length ; index++) {
 *       Language language = languages[index];
 *       result[index] = new RuleRepository(...);
 *     }
 *     return result;
 *   }
 * }
 * </pre>
 * </p>
 * 
 * @since 2.3
 */
public abstract class ExtensionProvider implements Extension, Provider {

}
