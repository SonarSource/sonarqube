package plugins.checks;

public class WebCheck implements Check {

  public static final String DIR = "src/web";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(DIR);
    validation.mustHaveIssues(DIR);
    validation.mustHaveSize(DIR);
    validation.mustHaveComments(DIR);
  }
}
