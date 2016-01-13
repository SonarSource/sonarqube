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
package it.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.MavenBuild;
import it.Category3Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenTest {

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void shouldSupportJarWithoutSources() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/project-with-module-without-sources"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.project-with-module-without-sources:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);

    Resource subProject = orchestrator.getServer().getWsClient().find(ResourceQuery.create("com.sonarsource.it.samples.project-with-module-without-sources:without-sources"));
    assertThat(subProject).isNotNull();
  }

  /**
   * See SONAR-594
   */
  @Test
  public void shouldSupportJeeProjects() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/jee"))
      .setGoals("clean install", "sonar:sonar");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.jee:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);

    List<Resource> modules = orchestrator.getServer().getWsClient().findAll(ResourceQuery.create("com.sonarsource.it.samples.jee:parent").setDepth(-1).setQualifiers("BRC"));
    assertThat(modules).hasSize(4);
  }

  /**
   * See SONAR-222
   */
  @Test
  public void shouldSupportMavenExtensions() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/maven-extensions"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-extensions", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);
  }

  /**
   * This test should be splitted. It checks multiple use-cases at the same time : SONAR-518, SONAR-519 and SONAR-593
   */
  @Test
  public void testBadMavenParameters() {
    // should not fail
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/maven-bad-parameters"))
      .setCleanSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples.maven-bad-parameters:parent", "files"));
    assertThat(project.getMeasureIntValue("files")).isGreaterThan(0);
  }

  @Test
  public void shouldAnalyzeMultiModules() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/modules-order"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    Sonar sonar = orchestrator.getServer().getWsClient();
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:root")).getName()).isEqualTo("Sonar tests - modules order");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:parent")).getName()).isEqualTo("Parent");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_a")).getName()).isEqualTo("Module A");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_a:src/main/java/HelloA.java")).getName()).isEqualTo("HelloA.java");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_b")).getName()).isEqualTo("Module B");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-order:module_b:src/main/java/HelloB.java")).getName()).isEqualTo("HelloB.java");
  }

  /**
   * See SONAR-2735
   */
  @Test
  public void shouldSupportDifferentDeclarationsForModules() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/modules-declaration"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
    Sonar sonar = orchestrator.getServer().getWsClient();

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:root")).getName()).isEqualTo("Root");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_a")).getName()).isEqualTo("Module A");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_a:src/main/java/HelloA.java")).getName()).isEqualTo("HelloA.java");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_b")).getName()).isEqualTo("Module B");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_b:src/main/java/HelloB.java")).getName()).isEqualTo("HelloB.java");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_c")).getName()).isEqualTo("Module C");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_c:src/main/java/HelloC.java")).getName()).isEqualTo("HelloC.java");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_d")).getName()).isEqualTo("Module D");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_d:src/main/java/HelloD.java")).getName()).isEqualTo("HelloD.java");

    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_e")).getName()).isEqualTo("Module E");
    assertThat(sonar.find(new ResourceQuery("org.sonar.tests.modules-declaration:module_e:src/main/java/HelloE.java")).getName()).isEqualTo("HelloE.java");
  }

  /**
   * See SONAR-3843
   */
  @Test
  public void should_support_shade_with_dependency_reduced_pom_with_clean_install_sonar_goals() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/shade-with-dependency-reduced-pom"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setGoals("clean", "install", "sonar:sonar");

    orchestrator.executeBuild(build);
  }

  /**
   * SONAR-4245
   */
  @Test
  @Ignore("This test should be moved to a Medium test of the Compute Engine")
  public void should_prevent_analysis_of_module_then_project() {
    MavenBuild scan = MavenBuild.create(ItUtils.projectPom("shared/multi-modules-sample/module_a"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setCleanSonarGoals();
    orchestrator.executeBuild(scan);

    scan = MavenBuild.create(ItUtils.projectPom("shared/multi-modules-sample"))
      .setProperty("sonar.dynamicAnalysis", "false")
      .setCleanSonarGoals();
    BuildResult result = orchestrator.executeBuildQuietly(scan);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains("The project 'com.sonarsource.it.samples:module_a' is already defined in SonarQube "
      + "but not as a module of project 'com.sonarsource.it.samples:multi-modules-sample'. "
      + "If you really want to stop directly analysing project 'com.sonarsource.it.samples:module_a', "
      + "please first delete it from SonarQube and then relaunch the analysis of project 'com.sonarsource.it.samples:multi-modules-sample'.");
  }

  /**
   * src/main/java is missing
   */
  @Test
  public void maven_project_with_only_test_dir() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/maven-only-test-dir")).setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-only-test-dir", "tests", "files"));
    assertThat(project.getMeasureIntValue("tests")).isEqualTo(1);
    assertThat(project.getMeasure("files")).isNull();
  }

  /**
   * The property sonar.sources overrides the source dirs as declared in Maven
   */
  @Test
  public void override_sources() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/maven-override-sources")).setGoals("sonar:sonar");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:maven-override-sources", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);

    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.create("com.sonarsource.it.samples:maven-override-sources:src/main/java2/Hello2.java"));
    assertThat(file).isNotNull();
  }

  /**
   * The property sonar.inclusions overrides the property sonar.sources
   */
  @Test
  public void inclusions_apply_to_source_dirs() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/inclusions_apply_to_source_dirs")).setGoals("sonar:sonar");
    orchestrator.executeBuild(build);

    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:inclusions_apply_to_source_dirs", "files"));
    assertThat(project.getMeasureIntValue("files")).isEqualTo(1);

    Resource file = orchestrator.getServer().getWsClient().find(ResourceQuery.create("com.sonarsource.it.samples:inclusions_apply_to_source_dirs:src/main/java/Hello2.java"));
    assertThat(file).isNotNull();
  }

  /**
   * The property sonar.sources has a typo -> fail, like in sonar-runner
   */
  @Test
  public void fail_if_bad_value_of_sonar_sources_property() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/maven-bad-sources-property")).setGoals("sonar:sonar");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains(
      "java2' does not exist for Maven module com.sonarsource.it.samples:maven-bad-sources-property:jar:1.0-SNAPSHOT. Please check the property sonar.sources");
  }

  /**
   * The property sonar.sources has a typo -> fail, like in sonar-runner
   */
  @Test
  public void fail_if_bad_value_of_sonar_tests_property() {
    MavenBuild build = MavenBuild.create(ItUtils.projectPom("maven/maven-bad-tests-property")).setGoals("sonar:sonar");
    BuildResult result = orchestrator.executeBuildQuietly(build);
    assertThat(result.getStatus()).isNotEqualTo(0);
    assertThat(result.getLogs()).contains(
      "java2' does not exist for Maven module com.sonarsource.it.samples:maven-bad-tests-property:jar:1.0-SNAPSHOT. Please check the property sonar.tests");
  }

}
