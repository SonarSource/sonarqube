package it.plugins.checks;

public class JavascriptCheck implements Check {

  public static final String SRC_DIR = "src/js";

  @Override
  public void validate(Validation validation) {
    validation.mustHaveNonEmptySource(SRC_DIR);
    validation.mustHaveSize(SRC_DIR);
    validation.mustHaveComments(SRC_DIR);
    validation.mustHaveComplexity(SRC_DIR);
    validation.mustHaveIssues(SRC_DIR + "/HasIssues.js");
    validation.mustHaveMeasuresGreaterThan(SRC_DIR + "/Person.js", 0, "coverage");
  }
}
