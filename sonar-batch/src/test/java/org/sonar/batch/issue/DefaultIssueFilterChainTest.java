package org.sonar.batch.issue;

import org.fest.assertions.Fail;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultIssueFilterChainTest {

  private final Issue issue = mock(Issue.class);

  @Test
  public void should_accept_when_no_filter() throws Exception {
    assertThat(new DefaultIssueFilterChain().accept(issue)).isTrue();
  }

  class PassingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      return chain.accept(issue);
    }
  }

  class AcceptingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      return true;
    }
  }

  class RefusingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      return false;
    }
  }

  class FailingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      Fail.fail();
      return false;
    }

  }

  @Test
  public void should_accept_if_all_filters_pass() throws Exception {
    assertThat(new DefaultIssueFilterChain(
        new PassingFilter(),
        new PassingFilter(),
        new PassingFilter()
      ).accept(issue)).isTrue();
  }

  @Test
  public void should_accept_and_not_go_further_if_filter_accepts() throws Exception {
    assertThat(new DefaultIssueFilterChain(
        new PassingFilter(),
        new AcceptingFilter(),
        new FailingFilter()
      ).accept(issue)).isTrue();
  }

  @Test
  public void should_refuse_and_not_go_further_if_filter_refuses() throws Exception {
    assertThat(new DefaultIssueFilterChain(
        new PassingFilter(),
        new RefusingFilter(),
        new FailingFilter()
      ).accept(issue)).isFalse();
  }
}
