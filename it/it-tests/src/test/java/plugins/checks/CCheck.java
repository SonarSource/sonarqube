package plugins.checks;

public class CCheck implements Check {

  public static final String DIR = "src/c";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveIssues(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
    validation.mustHaveComplexity(DIR);
  }
}
