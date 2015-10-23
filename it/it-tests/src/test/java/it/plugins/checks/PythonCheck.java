package it.plugins.checks;

import it.plugins.Project;

public class PythonCheck implements Check {

  public static final String DIR = "src/python";

  @Override
  public void validate(Validation validation) {
    // all files have size measures, even empty __init__.py
    validation.mustHaveSize(DIR);

    for (String filePath : Project.allFilesInDir(DIR)) {
      if (filePath.endsWith("__init__.py")) {
        validation.mustHaveSource(filePath);
      } else {
        validation.mustHaveNonEmptySource(filePath);
        validation.mustHaveComments(filePath);
        validation.mustHaveComplexity(filePath);
      }
    }

    validation.mustHaveIssues(DIR + "/hasissues.py");
  }

}
