import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFilter;
import org.sonar.api.rule.RuleKey;

/**
 * This filter removes the issues that are raised by Xoo plugin on modules.
 * <p/>
 * Issue filters have been introduced in 3.6.
 */
public class ModuleIssueFilter implements IssueFilter {

  private static final RuleKey ONE_ISSUE_PER_MODULE_RULEKEY = RuleKey.of("xoo", "OneIssuePerModule");

  private final Settings settings;

  public ModuleIssueFilter(Settings settings) {
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

    return !settings.getBoolean("enableIssueFilters") || !ONE_ISSUE_PER_MODULE_RULEKEY.equals(issue.ruleKey());
  }
}
