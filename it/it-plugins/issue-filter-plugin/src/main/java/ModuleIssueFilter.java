import org.sonar.api.scan.issue.filter.FilterableIssue;

import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.issue.filter.IssueFilter;
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
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    if (issue.componentKey() == null) {
      throw new IllegalStateException("Issue component is not set");
    }
    if (issue.ruleKey() == null) {
      throw new IllegalStateException("Issue rule is not set");
    }

    boolean b = !settings.getBoolean("enableIssueFilters") || !ONE_ISSUE_PER_MODULE_RULEKEY.equals(issue.ruleKey());

    if (!b) {
      return false;
    }

    return chain.accept(issue);
  }
}
