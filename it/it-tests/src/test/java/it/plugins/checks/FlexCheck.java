package it.plugins.checks;

public class FlexCheck implements Check {

  public static final String DIR = "src/flex";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
    validation.mustHaveComplexity(DIR);
    validation.mustHaveIssues(DIR + "/HasIssues.as");
  }
}
