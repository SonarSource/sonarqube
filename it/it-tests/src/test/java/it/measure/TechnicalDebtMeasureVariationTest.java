package it.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import java.util.List;
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
import static util.ItUtils.setServerProperty;

/**
 * SONAR-4776
 */
public class TechnicalDebtMeasureVariationTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_analysis");
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
  public void new_technical_debt_measures_from_new_issues() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", "2013-01-01");

    // Second analysis -> issues will be created
    defineQualityProfile("one-issue-per-line");
    setSampleProjectQualityProfile("one-issue-per-line");
    runSampleProjectAnalysis();

    // New technical debt only comes from new issues
    Resource newTechnicalDebt = getSampleProjectResourceWithVariations("new_technical_debt");
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(17);
    assertThat(measures.get(0).getVariation2()).isEqualTo(17);

    // Third analysis, with exactly the same profile -> no new issues so no new technical debt
    runSampleProjectAnalysis();

    newTechnicalDebt = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));

    // No variation => measure is purged
    assertThat(newTechnicalDebt).isNull();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_previous_analysis() throws Exception {
    // This test assumes that period 1 is "since previous analysis"

    // Execute twice analysis
    defineQualityProfile("one-issue-per-file");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();
    runSampleProjectAnalysis();

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");

    Resource newTechnicalDebt = getSampleProjectResourceWithVariations("new_technical_debt");
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(90);

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");

    newTechnicalDebt = getSampleProjectResourceWithVariations("new_technical_debt");
    measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(0);
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_30_days() throws Exception {
    // This test assumes that period 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", "2013-01-01");

    // Second analysis -> issues will be created
    String profileXmlFile = "one-issue-per-file";
    defineQualityProfile(profileXmlFile);
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");

    Resource newTechnicalDebt = getSampleProjectResourceWithVariations("new_technical_debt");
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation2()).isEqualTo(90);

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis but still since 30
    // days
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");

    newTechnicalDebt = getSampleProjectResourceWithVariations("new_technical_debt");
    measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation2()).isEqualTo(90);
  }

  /**
   * SONAR-5059
   */
  @Test
  public void new_technical_debt_measures_should_never_be_negative() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis with a big effort to fix
    defineQualityProfile("one-issue-per-file");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "100");

    // Execute a second analysis with a smaller effort to fix -> Added technical debt should be 0, not negative
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");

    Resource newTechnicalDebt = getSampleProjectResourceWithVariations("new_technical_debt");
    assertThat(newTechnicalDebt).isNull();
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

  private void runSampleProjectAnalysis(String... properties) {
    ItUtils.runVerboseProjectAnalysis(TechnicalDebtMeasureVariationTest.orchestrator, "shared/xoo-sample", properties);
  }

  private Resource getSampleProjectResourceWithVariations(String metricKey) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", metricKey).setIncludeTrends(true));
  }

}
