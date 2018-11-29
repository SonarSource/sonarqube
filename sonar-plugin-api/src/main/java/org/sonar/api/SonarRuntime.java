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

import javax.annotation.concurrent.Immutable;

import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Version;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Information about runtime environment.
 *
 * <p>
 * A usage for plugins is to benefit from new APIs
 * while keeping backward-compatibility with previous versions of SonarQube
 * or SonarLint.
 * </p>
 *
 * <p>
 * Example: a plugin extension wants to use a new feature of API 6.1 without
 * breaking compatibility with version 6.0 at runtime. This new feature
 * would be enabled only in 6.1 and greater runtimes.
 * </p>
 * <pre>
 * // Component provided by sonar-plugin-api
 * // @since 6.0
 * public interface AnApi {
 *   // implicitly since 6.0
 *   public void foo();
 *
 *   // @since 6.1
 *   public void bar();
 * }
 * 
 * // Plugin extension
 * public class MyExtension {
 *   private final SonarRuntime sonarRuntime;
 *   private final AnApi api;
 *
 *   public MyExtension(SonarRuntime sonarRuntime, AnApi api) {
 *     this.sonarRuntime = sonarRuntime;
 *     this.api = api;
 *   }
 *
 *   public void doSomething() {
 *     // assume that minimal supported runtime is 6.0
 *     api.foo();
 *
 *     if (sonarRuntime.getApiVersion().isGreaterThanOrEqual(Version.create(6, 1))) {
 *       api.bar();
 *     }
 *   }
 * }
 * </pre>
 *
 *
 * <p>
 *   Note that {@link Sensor} extensions can directly get {@link SonarRuntime} through
 * {@link SensorContext#runtime()}, without using constructor injection:
 * </p>
 * <pre>
 * public class MySensor implements Sensor {
 *
 *   public void execute(SensorContext context) {
 *     if (context.runtime().getApiVersion().isGreaterThanOrEqual(Version.create(6, 1)) {
 *       context.newMethodIntroducedIn6_0();
 *     }
 *   }
 *
 * }
 * </pre>
 *
 * <p>
 * The minimal supported version of plugin API is verified at runtime. As plugin is built
 * with sonar-plugin-api 6.1, we assume that the plugin requires v6.1 or greater at runtime.
 * For this reason the plugin must override the minimal supported version
 * in the configuration of sonar-packaging-maven-plugin 1.16+:
 * <p>
 * <pre>
 * &lt;packaging&gt;sonar-plugin&lt;/packaging&gt;
 *
 * &lt;dependencies&gt;
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.sonarsource.sonarqube&lt;/groupId&gt;
 *     &lt;artifactId&gt;sonar-plugin-api&lt;/artifactId&gt;
 *     &lt;version&gt;6.1&lt;/version&gt;
 *     &lt;scope&gt;provided&lt;/scope&gt;
 *   &lt;/dependency&gt;
 * &lt;/dependencies&gt;
 *
 * &lt;build&gt;
 *  &lt;plugins&gt;
 *    &lt;plugin&gt;
 *      &lt;groupId&gt;org.sonarsource.sonar-packaging-maven-plugin&lt;/groupId&gt;
 *      &lt;artifactId&gt;sonar-packaging-maven-plugin&lt;/artifactId&gt;
 *      &lt;version&gt;1.16&lt;/version&gt;
 *      &lt;extensions&gt;true&lt;/extensions&gt;
 *      &lt;configuration&gt;
 *        &lt;!-- Override the default value 6.0 which is guessed from sonar-plugin-api dependency --&gt;
 *        &lt;sonarQubeMinVersion&gt;6.0&lt;/sonarQubeMinVersion&gt;
 *      &lt;/configuration&gt;
 *    &lt;/plugin&gt;
 *  &lt;/plugins&gt;
 * &lt;/build&gt;
 * </pre>
 *
 * <p>
 *   As this component was introduced in version 6.0, the pattern described above can't be
 *   exactly applied when plugin must support version 5.6 Long Term Support. In this case plugin
 *   should use {@link SonarQubeVersion}, for example through {@link Plugin.Context#getSonarQubeVersion()} or
 *   {@link SensorContext#getSonarQubeVersion()}.
 * </p>
 *
 * <p>
 * Unit tests of plugin extensions can create instances of {@link SonarRuntime}
 * via {@link org.sonar.api.internal.SonarRuntimeImpl}.
 * </p>
 * @since 6.0
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
@SonarLintSide
@Immutable
public interface SonarRuntime {

  /**
   * Version of API (sonar-plugin-api artifact) at runtime.
   * It can be helpful to call some API classes/methods without checking their availability at
   * runtime by using reflection.
   * <br/>
   * Since 6.3, the returned version includes the build number in the fourth field, for
   * example {@code "6.3.0.12345"}.
  */
  Version getApiVersion();

  /**
   * The product being executed at runtime. It targets analysers so that they can implement
   * different behaviours in SonarQube and SonarLint.
   */
  SonarProduct getProduct();

  /**
   * The SonarQube stack being executed at runtime.
   * @throws UnsupportedOperationException if {@link #getProduct()} is not equal to {@link SonarProduct#SONARQUBE}
   */
  SonarQubeSide getSonarQubeSide();

}
