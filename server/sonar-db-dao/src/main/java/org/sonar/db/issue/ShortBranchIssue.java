package org.sonar.db.issue;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.tracking.Trackable;

@Immutable
public class ShortBranchIssue implements Trackable {
  private final Integer line;
  private final String message;
  private final String lineHash;
  private final RuleKey ruleKey;
  private final String status;
  private final String resolution;

  public ShortBranchIssue(ShortBranchIssueDto dto) {
    this.line = dto.getLine();
    this.message = dto.getMessage();
    this.lineHash = dto.getChecksum();
    this.ruleKey = dto.getRuleKey();
    this.status = dto.getStatus();
    this.resolution = dto.getResolution();
  }

  @CheckForNull
  @Override
  public Integer getLine() {
    return line;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @CheckForNull
  @Override
  public String getLineHash() {
    return lineHash;
  }

  @Override
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public String getStatus() {
    return status;
  }

  public String getResolution() {
    return resolution;
  }
}
