/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package duplications.suite;

import util.ItUtils;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class DuplicationsTest {

  private static final String DUPLICATIONS = "com.sonarsource.it.samples:duplications";
  private static final String DUPLICATIONS_WITH_EXCLUSIONS = "com.sonarsource.it.samples:duplications-with-exclusions";
  @ClassRule
  public static Orchestrator orchestrator = DuplicationsTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();

    MavenBuild build = MavenBuild.create(ItUtils.projectPom("duplications/file-duplications"))
      .setCleanPackageSonarGoals();
    orchestrator.executeBuild(build);

    // Use a new project key to avoid conflict with other tests
    String projectKey = DUPLICATIONS_WITH_EXCLUSIONS;
    build = MavenBuild.create(ItUtils.projectPom("duplications/file-duplications"))
      .setCleanPackageSonarGoals()
      .setProperties("sonar.projectKey", projectKey,
        "sonar.cpd.exclusions", "**/Class*");
    orchestrator.executeBuild(build);

  }

  @Test
  public void duplicated_lines_within_same_class() {
    Resource file = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_within_same_class/DuplicatedLinesInSameClass.java");
    assertThat(file, not(nullValue()));
    assertThat(file.getMeasureValue("duplicated_blocks"), is(2.0));
    assertThat(file.getMeasureValue("duplicated_lines"), is(27.0 * 2)); // 2 blocks with 27 lines
    assertThat(file.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file.getMeasureValue("duplicated_lines_density"), is(60.0));
  }

  @Test
  public void duplicated_same_lines_within_3_classes() {
    Resource file1 = getResource(DUPLICATIONS + ":src/main/java/duplicated_same_lines_within_3_classes/Class1.java");
    assertThat(file1, not(nullValue()));
    assertThat(file1.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines"), is(29.0));
    assertThat(file1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines_density"), is(47.5));

    Resource file2 = getResource(DUPLICATIONS + ":src/main/java/duplicated_same_lines_within_3_classes/Class2.java");
    assertThat(file2, not(nullValue()));
    assertThat(file2.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines"), is(29.0));
    assertThat(file2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines_density"), is(48.3));

    Resource file3 = getResource(DUPLICATIONS + ":src/main/java/duplicated_same_lines_within_3_classes/Class3.java");
    assertThat(file3, not(nullValue()));
    assertThat(file3.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file3.getMeasureValue("duplicated_lines"), is(29.0));
    assertThat(file3.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file3.getMeasureValue("duplicated_lines_density"), is(46.0));

    Resource pkg = getResource(DUPLICATIONS + ":src/main/java/duplicated_same_lines_within_3_classes");
    assertThat(pkg, not(nullValue()));
    assertThat(pkg.getMeasureValue("duplicated_blocks"), is(3.0));
    assertThat(pkg.getMeasureValue("duplicated_lines"), is(29.0 * 3)); // 3 blocks with 29 lines
    assertThat(pkg.getMeasureValue("duplicated_files"), is(3.0));
    assertThat(pkg.getMeasureValue("duplicated_lines_density"), is(47.3));
  }

  @Test
  public void duplicated_lines_within_package() {
    Resource file1 = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_within_package/DuplicatedLinesInSamePackage1.java");
    assertThat(file1, not(nullValue()));
    assertThat(file1.getMeasureValue("duplicated_blocks"), is(4.0));
    assertThat(file1.getMeasureValue("duplicated_lines"), is(72.0));
    assertThat(file1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines_density"), is(58.1));

    Resource file2 = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_within_package/DuplicatedLinesInSamePackage2.java");
    assertThat(file2, not(nullValue()));
    assertThat(file2.getMeasureValue("duplicated_blocks"), is(3.0));
    assertThat(file2.getMeasureValue("duplicated_lines"), is(58.0));
    assertThat(file2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines_density"), is(64.4));

    Resource pkg = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_within_package");
    assertThat(pkg, not(nullValue()));
    assertThat(pkg.getMeasureValue("duplicated_blocks"), is(4.0 + 3.0));
    assertThat(pkg.getMeasureValue("duplicated_lines"), is(72.0 + 58.0));
    assertThat(pkg.getMeasureValue("duplicated_files"), is(2.0));
    assertThat(pkg.getMeasureValue("duplicated_lines_density"), is(60.7));
  }

  @Test
  public void duplicated_lines_with_other_package() {
    Resource file1 = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_with_other_package1/DuplicatedLinesWithOtherPackage.java");
    assertThat(file1, not(nullValue()));
    assertThat(file1.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(file1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file1.getMeasureValue("duplicated_lines_density"), is(60.0));

    Resource pkg1 = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_with_other_package1");
    assertThat(pkg1, not(nullValue()));
    assertThat(pkg1.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(pkg1.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(pkg1.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(pkg1.getMeasureValue("duplicated_lines_density"), is(60.0));

    Resource file2 = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_with_other_package2/DuplicatedLinesWithOtherPackage.java");
    assertThat(file2, not(nullValue()));
    assertThat(file2.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(file2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(file2.getMeasureValue("duplicated_lines_density"), is(60.0));

    Resource pkg2 = getResource(DUPLICATIONS + ":src/main/java/duplicated_lines_with_other_package2");
    assertThat(pkg2, not(nullValue()));
    assertThat(pkg2.getMeasureValue("duplicated_blocks"), is(1.0));
    assertThat(pkg2.getMeasureValue("duplicated_lines"), is(36.0));
    assertThat(pkg2.getMeasureValue("duplicated_files"), is(1.0));
    assertThat(pkg2.getMeasureValue("duplicated_lines_density"), is(60.0));
  }

  @Test
  public void consolidation() {
    Resource project = getResource(DUPLICATIONS);
    assertThat(project, not(nullValue()));
    assertThat(project.getMeasureValue("duplicated_blocks"), is(14.0));
    assertThat(project.getMeasureValue("duplicated_lines"), is(343.0));
    assertThat(project.getMeasureValue("duplicated_files"), is(8.0));
    assertThat(project.getMeasureValue("duplicated_lines_density"), is(56.4));
  }

  /**
   * SONAR-3108
   */
  @Test
  public void use_duplication_exclusions() {
    Resource project = getResource(DUPLICATIONS_WITH_EXCLUSIONS);
    assertThat(project, not(nullValue()));
    assertThat(project.getMeasureValue("duplicated_blocks"), is(11.0));
    assertThat(project.getMeasureValue("duplicated_lines"), is(256.0));
    assertThat(project.getMeasureValue("duplicated_files"), is(5.0));
    assertThat(project.getMeasureValue("duplicated_lines_density"), is(42.1));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(key, "duplicated_lines", "duplicated_blocks", "duplicated_files", "duplicated_lines_density"));
  }

}
