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

import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Version;

import static java.util.Objects.requireNonNull;

/**
 * Version of SonarQube at runtime. This component can be injected as a dependency
 * of plugin extensions. The main usage for a plugin is to benefit from new APIs
 * while keeping backward-compatibility with previous versions of SonarQube.
 * <br><br>
 * 
 * Example 1: a {@link Sensor} wants to use an API introduced in version 5.5 and still requires to support older versions
 * at runtime.
 * <pre>
 * public class MySensor implements Sensor {
 *
 *   public void execute(SensorContext context) {
 *     if (context.getSonarQubeVersion().isGreaterThanOrEqual(SonarQubeVersion.V5_5)) {
 *       context.newMethodIntroducedIn5_5();
 *     }
 *   }
 * }
 * </pre>
 *
 * Example 2: a plugin needs to use an API introduced in version 5.6 ({@code AnApi} in the following
 * snippet) and still requires to support version 5.5 at runtime.
 * <br>
 * <pre>
 * // Component provided by sonar-plugin-api
 * // @since 5.5
 * public interface AnApi {
 *   // implicitly since 5.5
 *   public void foo();
 *
 *   // @since 5.6
 *   public void bar();
 * }
 * 
 * // Component provided by plugin
 * public class MyExtension {
 *   private final SonarQubeVersion sonarQubeVersion;
 *   private final AnApi api;
 *
 *   public MyExtension(SonarQubeVersion sonarQubeVersion, AnApi api) {
 *     this.sonarQubeVersion = sonarQubeVersion;
 *     this.api = api;
 *   }
 *
 *   public void doSomething() {
 *     // assume that runtime is 5.5+
 *     api.foo();
 *
 *     if (sonarQubeVersion.isGreaterThanOrEqual(SonarQubeVersion.V5_6)) {
 *       api.bar();
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * The minimal supported version of SonarQube is verified at runtime. As plugin is built
 * with sonar-plugin-api 5.6, we assume that the plugin requires v5.6 or greater at runtime.
 * For this reason the plugin must default which is the minimal supported version
 * in the configuration of sonar-packaging-maven-plugin 1.16+:
 * <p>
 * <pre>
 * &lt;packaging&gt;sonar-plugin&lt;/packaging&gt;
 *
 * &lt;dependencies&gt;
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.sonarsource.sonarqube&lt;/groupId&gt;
 *     &lt;artifactId&gt;sonar-plugin-api&lt;/artifactId&gt;
 *     &lt;version&gt;5.6&lt;/version&gt;
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
 *        &lt;!-- Override the default value 5.6 which is guessed from sonar-plugin-api dependency --&gt;
 *        &lt;sonarQubeMinVersion&gt;5.5&lt;/sonarQubeMinVersion&gt;
 *      &lt;/configuration&gt;
 *    &lt;/plugin&gt;
 *  &lt;/plugins&gt;
 * &lt;/build&gt;
 * </pre>
 *
 *
 * @since 5.5
 */
@BatchSide
@ServerSide
@ComputeEngineSide
@Immutable
public class SonarQubeVersion {

  /**
   * Constant for version 5.5
   */
  public static final Version V5_5 = Version.create(5, 5);

  /**
   * Constant for version 5.6
   */
  public static final Version V5_6 = Version.create(5, 6);

  private final Version version;

  public SonarQubeVersion(Version version) {
    requireNonNull(version);
    this.version = version;
  }

  public Version get() {
    return this.version;
  }

  public boolean isGreaterThanOrEqual(Version than) {
    return this.version.isGreaterThanOrEqual(than);
  }

}
