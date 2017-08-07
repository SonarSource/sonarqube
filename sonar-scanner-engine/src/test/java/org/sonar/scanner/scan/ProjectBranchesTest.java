package org.sonar.scanner.scan;

import java.util.Collections;
import java.util.Optional;
import org.junit.*;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.core.config.ScannerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProjectBranchesTest {

  Configuration settings;

  @Before
  public void setUp() {
    settings = mock(Configuration.class);
    when(settings.get(anyString())).thenReturn(Optional.empty());
  }

  @Test
  public void should_be_longLived_when_branchName_missing() {
    assertThat(ProjectBranches.create(settings, Collections.emptyList()).branchType()).isEqualTo(ProjectBranches.BranchType.LONG);
  }

  @Test
  public void should_be_longLived_when_branchName_new_and_matches_pattern() {
    String branchName = "long";
    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of(branchName));
    when(settings.get(eq(CoreProperties.LONG_LIVED_BRANCHES_REGEX))).thenReturn(Optional.of(branchName));
    assertThat(ProjectBranches.create(settings, Collections.emptyList()).branchType()).isEqualTo(ProjectBranches.BranchType.LONG);
  }

  @Test
  public void should_be_shortLived_when_branchName_new_and_does_not_match_pattern() {
    String branchName = "long";
    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of(branchName));
    when(settings.get(eq(CoreProperties.LONG_LIVED_BRANCHES_REGEX))).thenReturn(Optional.of(branchName + "x"));
    assertThat(ProjectBranches.create(settings, Collections.emptyList()).branchType()).isEqualTo(ProjectBranches.BranchType.SHORT);
  }

  @Test
  public void should_be_shortLived_when_branchName_exists_regardless_of_pattern() {
    String branchName = "long";
    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of(branchName));
    when(settings.get(eq(CoreProperties.LONG_LIVED_BRANCHES_REGEX))).thenReturn(Optional.of(branchName));
    assertThat(ProjectBranches.create(settings, Collections.singletonList(
      new ProjectBranches.BranchInfo(branchName, false)
    )).branchType()).isEqualTo(ProjectBranches.BranchType.SHORT);
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_when_longLived_regex_property_missing() {
    String branchName = "long";
    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of(branchName));
    ProjectBranches.create(settings, Collections.emptyList()).branchType();
  }

  @Test(expected = IllegalStateException.class)
  public void should_fail_when_branchTarget_nonexistent() {
    String branchName = "long";
    when(settings.get(eq(ScannerProperties.BRANCH_NAME))).thenReturn(Optional.of(branchName));
    when(settings.get(eq(ScannerProperties.BRANCH_TARGET))).thenReturn(Optional.of("nonexistent"));
    ProjectBranches.create(settings, Collections.emptyList()).branchType();
  }
}
