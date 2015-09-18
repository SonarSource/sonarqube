package analysis.suite.measure;

import analysis.suite.AnalysisTestSuite;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;


public class DifferentialPeriodsTest {

  @ClassRule
  public static final Orchestrator orchestrator = AnalysisTestSuite.ORCHESTRATOR;

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @After
  public void tearDown() throws Exception {
    String propertyKey = "sonar.timemachine.period4";
    unsetProperty(propertyKey);
    unsetProperty("sonar.timemachine.period5");
  }

  /**
   * SONAR-6787
   */
  @Test
  public void ensure_differential_period_4_and_5_defined_at_project_level_is_taken_into_account() throws Exception {
    setProperty("sonar.timemachine.period4", "30");
    setProperty("sonar.timemachine.period5", "previous_analysis");

    // Execute an analysis in the past to have a past snapshot without any issues
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "empty");
    orchestrator.executeBuild(SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
        .setProperty("sonar.projectDate", "2013-01-01"));

    // Second analysis -> issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measure/suite/one-issue-per-line.xml"));
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    orchestrator.executeBuild(SonarRunner.create(ItUtils.projectDir("shared/xoo-sample")));

    // New technical debt only comes from new issues
    Resource newTechnicalDebt = orchestrator.getServer().getWsClient()
        .find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation4()).isEqualTo(17);
    assertThat(measures.get(0).getVariation5()).isEqualTo(17);

    // Third analysis, with exactly the same profile -> no new issues so no new technical debt
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    orchestrator.executeBuild(SonarRunner.create(ItUtils.projectDir("shared/xoo-sample")));

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(
        ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true)
    );

    // No variation => measure is purged
    assertThat(newTechnicalDebt).isNull();
  }

  private static void unsetProperty(String propertyKey) {
    setProperty(propertyKey, "");
  }

  private static void setProperty(String propertyKey, String propertyValue) {
    orchestrator.getServer().adminWsClient().post(
        "/api/properties?",
        "id", propertyKey,
        "value", propertyValue
    );
  }

}

