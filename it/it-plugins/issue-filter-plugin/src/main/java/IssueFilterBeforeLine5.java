import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFilter;

/**
 * This filter removes the issues that are on line < 5
 * <p/>
 * Issue filters have been introduced in 3.6.
 */
public class IssueFilterBeforeLine5 implements IssueFilter {

  private final Settings settings;

  public IssueFilterBeforeLine5(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean accept(Issue issue) {
    if (issue.componentKey() == null) {
      throw new IllegalStateException("Issue component is not set");
    }
    if (issue.ruleKey() == null) {
      throw new IllegalStateException("Issue rule is not set");
    }

    return !settings.getBoolean("enableIssueFilters") || issue.line() == null || issue.line() >= 5;
  }
}
