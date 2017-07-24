package org.sonar.server.computation.task.projectanalysis.step;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.duplication.DuplicationMeasures;
import org.sonar.server.computation.task.projectanalysis.duplication.IncrementalDuplicationMeasures;
import org.sonar.server.computation.task.step.ComputationStep;

public class DuplicationMeasuresStepTest extends BaseStepTest {
  @Mock
  private DuplicationMeasures defaultDuplicationMeasures;
  @Mock
  private IncrementalDuplicationMeasures incrementalDuplicationMeasures;
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private DuplicationMeasuresStep underTest;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    underTest = new DuplicationMeasuresStep(analysisMetadataHolder, defaultDuplicationMeasures, incrementalDuplicationMeasures);
  }

  @Test
  public void incremental_analysis_mode() {
    analysisMetadataHolder.setIncrementalAnalysis(true);
    underTest.execute();
    verify(incrementalDuplicationMeasures).execute();
    verifyZeroInteractions(defaultDuplicationMeasures);
  }

  @Test
  public void full_analysis_mode() {
    analysisMetadataHolder.setIncrementalAnalysis(false);
    underTest.execute();
    verify(defaultDuplicationMeasures).execute();
    verifyZeroInteractions(incrementalDuplicationMeasures);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
