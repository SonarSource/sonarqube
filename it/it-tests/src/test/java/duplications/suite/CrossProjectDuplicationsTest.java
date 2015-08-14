/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package duplications.suite;

import util.ItUtils;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@Ignore("Cross project duplications are temporary disabled, waiting to be reimplemented in CE or correctly implemented in the batch")
public class CrossProjectDuplicationsTest {

  @ClassRule
  public static Orchestrator orchestrator = DuplicationsTestSuite.ORCHESTRATOR;

  @Before
  public void analyzeProjects() {
    orchestrator.resetData();

    MavenBuild build = MavenBuild.create(ItUtils.projectPom("duplications/cross-project/a"))
      .setCleanSonarGoals()
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    build = MavenBuild.create(ItUtils.projectPom("duplications/cross-project/b"))
      .setCleanSonarGoals()
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    build = MavenBuild.create(ItUtils.projectPom("duplications/cross-project/b"))
      .setCleanSonarGoals()
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.branch", "branch")
      .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);
  }

  @Test
  public void testMeasures() throws Exception {

    Resource project = getResource("com.sonarsource.it.samples.duplications:a");
    assertThat(project, notNullValue());
    assertThat(project.getMeasureIntValue("duplicated_lines"), is(0));

    project = getResource("com.sonarsource.it.samples.duplications:b");
    assertThat(project, notNullValue());
    assertThat(project.getMeasureIntValue("duplicated_lines"), is(10));

    project = getResource("com.sonarsource.it.samples.duplications:b:branch");
    assertThat(project, notNullValue());
    assertThat(project.getMeasureIntValue("duplicated_lines"), is(0));
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "duplicated_lines"));
  }

}
