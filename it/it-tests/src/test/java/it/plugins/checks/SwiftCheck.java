package it.plugins.checks;

public class SwiftCheck implements Check {

  public static final String DIR = "src/swift";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveIssues(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComplexity(DIR);
  }
}
