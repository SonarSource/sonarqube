package it.plugins.checks;

public class AbapCheck implements Check {

  public static final String DIR = "src/abap";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveIssues(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
    validation.mustHaveComplexity(DIR);
  }
}
