package it.plugins.checks;

public class VbCheck implements Check {

  public static final String DIR = "src/vb";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveIssues(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
    validation.mustHaveComplexity(DIR);
  }
}
