package org.sonar.application.config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeVersionHelperTest {
  @Test
  public void getSonarQubeVersion_must_not_return_an_empty_string() {
    assertThat(SonarQubeVersionHelper.getSonarqubeVersion()).isNotEmpty();
  }

  @Test
  public void getSonarQubeVersion_must_always_return_same_value() {
    String sonarqubeVersion = SonarQubeVersionHelper.getSonarqubeVersion();
    for (int i = 0; i < 3; i++) {
      assertThat(SonarQubeVersionHelper.getSonarqubeVersion()).isEqualTo(sonarqubeVersion);
    }
  }
}
