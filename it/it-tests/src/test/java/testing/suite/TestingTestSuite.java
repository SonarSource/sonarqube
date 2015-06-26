/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package testing.suite;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import util.ItUtils;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  CoverageTrackingTest.class,
  CoverageTest.class,
  TestExecutionTest.class
})
public class TestingTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin(ItUtils.xooPlugin())
    .build();
}
