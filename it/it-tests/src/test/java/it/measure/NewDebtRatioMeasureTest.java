package it.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static util.ItUtils.setServerProperty;

/**
 * SONAR-5876
 */
public class NewDebtRatioMeasureTest {

  private static final String NEW_DEBT_RATIO_METRIC_KEY = "new_sqale_debt_ratio";

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @Test
  public void new_debt_ratio_is_computed_from_nes_debt_and_new_ncloc_count_per_file() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over 30 days"

    // run analysis on the day of after the first commit (2015-09-01), with 'one-issue-per-line' profile
    // => some issues at date 2015-09-02
    defineQualityProfile("one-issue-per-line");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-line");
    runSampleProjectAnalysis("v1", "sonar.projectDate", "2015-09-02");

    // first analysis, no previous snapshot => periods not resolved => no value
    assertNoNewDebtRatio();

    // run analysis on the day after of second commit (2015-09-17) 'one-issue-per-line' profile*
    // => 3 new issues will be created at date 2015-09-18
    runSampleProjectAnalysis("v2", "sonar.projectDate", "2015-09-18");
    assertNewDebtRatio(4.44, 4.44);

    // run analysis on the day after of third commit (2015-09-20) 'one-issue-per-line' profile*
    // => 4 new issues will be created at date 2015-09-21
    runSampleProjectAnalysis("v3", "sonar.projectDate", "2015-09-21");
    assertNewDebtRatio(4.17, 4.28);
  }

  private void assertNoNewDebtRatio() {
    assertThat(getFileResourceWithVariations(NEW_DEBT_RATIO_METRIC_KEY)).isNull();
  }

  private void assertNewDebtRatio(@Nullable Double valuePeriod1, @Nullable Double valuePeriod2) {
    Resource newTechnicalDebt = getFileResourceWithVariations(NEW_DEBT_RATIO_METRIC_KEY);
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(valuePeriod1, within(0.01));
    assertThat(measures.get(0).getVariation2()).isEqualTo(valuePeriod2, within(0.01));
  }

  private void setSampleProjectQualityProfile(String qualityProfileKey) {
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", qualityProfileKey);
  }

  private void provisionSampleProject() {
    orchestrator.getServer().provisionProject("sample", "sample");
  }

  private void defineQualityProfile(String qualityProfileKey) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measure/" + qualityProfileKey + ".xml"));
  }

  private void runSampleProjectAnalysis(String projectVersion, String... properties) {
    ItUtils.runVerboseProjectAnalysis(
      NewDebtRatioMeasureTest.orchestrator,
      "measure/xoo-new-debt-ratio-" + projectVersion,
      ItUtils.concat(properties,
        // disable standard scm support so that it does not interfere with Xoo Scm sensor
        "sonar.scm.disabled", "false")
      );
  }

  private Resource getFileResourceWithVariations(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", metricKey).setIncludeTrends(true));
  }

}
