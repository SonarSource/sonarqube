package it.plugins.checks;

public class CssCheck implements Check {

  public static final String DIR = "src/css";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveIssues(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
  }
}
