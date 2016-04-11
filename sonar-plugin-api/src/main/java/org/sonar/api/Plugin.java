/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.api.utils.Version;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Entry-point for plugins to inject extensions into SonarQube.
 * <p>The JAR manifest must declare the name of the implementation class in the property <code>Plugin-Class</code>.
 * This property is automatically set by sonar-packaging-maven-plugin when building plugin.
 * <p>Example of implementation
 * <pre>
 * package com.mycompany.sonarqube;
 * public class MyPlugin implements Plugin {
 *  {@literal @}Override
 *   public void define(Context context) {
 *     context.addExtensions(MySensor.class, MyRules.class);
 *     if (context.getSonarQubeVersion().isGreaterThanOrEqual(SonarQubeVersion.V5_6)) {
 *       // Extension which supports only versions 5.6 and greater
 *       // See org.sonar.api.SonarQubeVersion for more details.
 *       context.addExtension(MyNewExtension.class);
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Example of pom.xml
 * <pre>
 * &lt;project&gt;
 *   ...
 *   &lt;packaging&gt;sonar-plugin&lt;/packaging&gt;
 *
 *   &lt;build&gt;
 *     &lt;plugins&gt;
 *       &lt;plugin&gt;
 *         &lt;groupId&gt;org.sonarsource.sonar-packaging-maven-plugin&lt;/groupId&gt;
 *         &lt;artifactId&gt;sonar-packaging-maven-plugin&lt;/artifactId&gt;
 *         &lt;extensions&gt;true&lt;/extensions&gt;
 *         &lt;configuration&gt;
 *           &lt;pluginClass&gt;com.mycompany.sonarqube.MyPlugin&lt;/pluginClass&gt;
 *         &lt;/configuration&gt;
 *       &lt;/plugin&gt;
 *     &lt;/plugins&gt;
 *   &lt;/build&gt;
 * &lt;/project&gt;
 * </pre>
 *
 * <p>Example of test
 * <pre>
 * MyPlugin underTest = new MyPlugin();
 *
 *{@literal @}Test
 * public void test_plugin_extensions_compatible_with_5_5() {
 *   Plugin.Context context = new Plugin.Context(SonarQubeVersion.V5_5);
 *   underTest.define(context);
 *   assertThat(context.getExtensions()).hasSize(4);
 * }
 * </pre>
 *
 * @since 5.5
 */
@Beta
public interface Plugin {

  class Context {
    private final Version version;
    private final List extensions = new ArrayList();

    public Context(Version version) {
      this.version = version;
    }

    /**
     * Runtime version of SonarQube
     */
    public Version getSonarQubeVersion() {
      return version;
    }

    /**
     * Add an extension as :
     * <ul>
     *   <li>a Class that is annotated with {@link org.sonar.api.batch.BatchSide}, {@link org.sonar.api.server.ServerSide}
     *   or {@link org.sonar.api.ce.ComputeEngineSide}. The extension will be instantiated once. Its dependencies are
     *   injected through constructor parameters.</li>
     *   <li>an instance that is annotated with {@link org.sonar.api.batch.BatchSide}, {@link org.sonar.api.server.ServerSide}
     *   or {@link org.sonar.api.ce.ComputeEngineSide}.</li>
     * </ul>
     * Only a single component can be registered for a class. It's not allowed for example to register:
     * <ul>
     *   <li>two MyExtension.class</li>
     *   <li>MyExtension.class and new MyExtension()</li>
     * </ul>
     */
    public Context addExtension(Object extension) {
      requireNonNull(extension);
      this.extensions.add(extension);
      return this;
    }

    /**
     * @see #addExtension(Object)
     */
    public Context addExtensions(Collection extensions) {
      this.extensions.addAll(extensions);
      return this;
    }

    /**
     * @see #addExtension(Object)
     */
    public Context addExtensions(Object first, Object second, Object... others) {
      addExtension(first);
      addExtension(second);
      addExtensions(asList(others));
      return this;
    }

    public List getExtensions() {
      return extensions;
    }
  }

  /**
   * This method is executed at runtime when:
   * <ul>
   *   <li>Web Server starts</li>
   *   <li>Compute Engine starts</li>
   *   <li>Scanner starts</li>
   * </ul>
   */
  void define(Context context);
}
