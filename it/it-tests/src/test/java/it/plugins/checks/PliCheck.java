package it.plugins.checks;

public class PliCheck implements Check {

  public static final String DIR = "src/pli";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
    validation.mustHaveComplexity(DIR);
    validation.mustHaveIssues(DIR + "/hasissues.pli");
  }
}
