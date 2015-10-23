package it.plugins.checks;

import com.google.common.base.Joiner;
import com.sonar.orchestrator.Orchestrator;
import it.plugins.Project;
import java.io.File;
import org.hamcrest.Matchers;
import org.junit.rules.ErrorCollector;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Source;
import org.sonar.wsclient.services.SourceQuery;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 *
 * TODO must have syntax highlighting
 * TODO must have duplications
 * TODO must have issues with SQALE characteristic and debt
 * TODO must have rules with characteristic
 * TODO must have tests
 * TODO must have coverage
 */
public class Validation {

  private final Orchestrator orchestrator;
  private final ErrorCollector errorCollector;

  public Validation(Orchestrator orchestrator, ErrorCollector errorCollector) {
    this.orchestrator = orchestrator;
    this.errorCollector = errorCollector;
  }

  public void mustHaveIssues(String path) {
    // TODO use the WS api/issues
    mustHaveMeasuresGreaterThan(path, 1, "violations");
  }

  public void mustHaveComments(String path) {
    mustHaveMeasuresGreaterThan(path, 0, "comment_lines", "comment_lines_density");
  }

  public void mustHaveComplexity(String path) {
    mustHaveMeasuresGreaterThan(path, 0, "complexity");
  }

  public void mustHaveSize(String path) {
    mustHaveMeasuresGreaterThan(path, 0, "ncloc", "lines");
  }

  public void mustHaveMeasuresGreaterThan(String path, int min, String... metricKeys) {
    for (String filePath : toFiles(path)) {
      fileMustHaveMeasures(filePath, metricKeys, min);
    }
  }

  private void fileMustHaveMeasures(String filePath, String[] metricKeys, int min) {
    Resource resource = getMeasureForComponentKey(filePathToKey(filePath), metricKeys);
    errorCollector.checkThat("Measures " + Joiner.on(",").join(metricKeys) + " are set on file " + filePath, resource, notNullValue());
    if (resource != null) {
      for (String metricKey : metricKeys) {
        Measure measure = resource.getMeasure(metricKey);
        errorCollector.checkThat("Measure " + metricKey + " is set on file " + filePath, measure, notNullValue());
        if (measure != null && measure.getIntValue() != null) {
          errorCollector.checkThat("Measure " + metricKey + " is positive on file " + filePath, measure.getIntValue(), Matchers.greaterThanOrEqualTo(min));
        }
      }
    }
  }

  /**
   * Checks that each source file of the given directory is uploaded to server.
   * @param path relative path to source directory or source file
   */
  public void mustHaveNonEmptySource(String path) {
    mustHaveSourceWithAtLeast(path, 1);
  }

  public void mustHaveSource(String path) {
    mustHaveSourceWithAtLeast(path, 0);
  }

  private void mustHaveSourceWithAtLeast(String path, int minLines) {
    for (String filePath : toFiles(path)) {
      Source source = orchestrator.getServer().getWsClient().find(SourceQuery.create(filePathToKey(filePath)));
      errorCollector.checkThat("Source is set on file " + filePath, source, notNullValue());
      if (source != null) {
        errorCollector.checkThat("Source is not empty on file " + filePath, source.getLines().size(), Matchers.greaterThanOrEqualTo(minLines));
      }
    }
  }

  private Iterable<String> toFiles(String path) {
    File fileOrDir = new File(Project.basedir(), path);
    if (!fileOrDir.exists()) {
      throw new IllegalArgumentException("Path does not exist: " + fileOrDir);
    }
    if (fileOrDir.isDirectory()) {
      return Project.allFilesInDir(path);
    }
    return asList(path);
  }

  public Resource getMeasureForComponentKey(String resourceKey, String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resourceKey, metricKeys));
  }

  private String filePathToKey(String filePath) {
    return "all-langs:" + filePath;
  }
}
