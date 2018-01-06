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
package org.sonar.api;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * Factory of extensions. It allows to dynamically create extensions depending upon runtime context. One use-case is
 * to create one rule repository by language.
 *
 * <p>Notes :
 * <ul>
 * <li>the provider is declared in Plugin.getExtensions()</li>
 * <li>the provider must also add annotation {@link ServerSide}, {@link ComputeEngineSide} and/or {@link BatchSide}</li>
 * <li>the provider can accept dependencies (parameters) in its constructors.</li>
 * <li>the method provide() is executed once by the platform</li>
 * <li>the method provide() must return an object, a class or an Iterable of objects. <strong>Arrays are excluded</strong>.</li>
 * </ul>
 * 
 *
 * <p>Example:
 * <pre>
 * {@code
 * {@literal @}ServerSide
 * public class RuleRepositoryProvider extends ExtensionProvider {
 *   private Language[] languages;
 *
 *   public RuleRepositoryProvider(Language[] languages) {
 *     this.languages = languages;
 *   }
 *
 *   public List<RuleRepository> provide() {
 *     List<RuleRepository> result = new ArrayList<RuleRepository>();
 *     for(Language language: languages) {
 *       result.add(new RuleRepository(..., language, ...));
 *     }
 *     return result;
 *   }
 * }
 * }
 * </pre>
 * 
 *
 * @since 2.3
 * @deprecated since 6.0 should no more be used
 */
@Deprecated
@ExtensionPoint
public abstract class ExtensionProvider {

  public abstract Object provide();
}
