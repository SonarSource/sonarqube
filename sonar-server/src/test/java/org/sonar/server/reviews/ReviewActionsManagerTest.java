package org.sonar.server.reviews;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.reviews.LinkReviewAction;
import org.sonar.api.reviews.ReviewAction;

import java.util.Collection;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ReviewActionsManagerTest {

  private ReviewAction fakeLinkReviewAction = new FakeLinkReviewAction();
  private ReviewAction simpleReviewAction = new SimpleReviewAction();
  private ReviewActionsManager manager;

  @Before
  public void init() throws Exception {
    manager = new ReviewActionsManager(new ReviewAction[] {fakeLinkReviewAction, simpleReviewAction});
  }

  @Test
  public void shouldReturnActionsById() throws Exception {
    assertThat(manager.getAction("fake-link-review"), is(fakeLinkReviewAction));
    assertThat(manager.getAction("simple-review-action"), is(simpleReviewAction));
  }

  @Test
  public void shouldReturnActionsByInterfaceName() throws Exception {
    Collection<ReviewAction> reviewActions = manager.getActions("org.sonar.api.reviews.LinkReviewAction");
    assertThat(reviewActions.size(), is(1));
    assertThat(reviewActions, hasItem(fakeLinkReviewAction));
  }

  class FakeLinkReviewAction implements LinkReviewAction {
    public String getId() {
      return "fake-link-review";
    }

    public String getName() {
      return "Fake action";
    }

    public void execute(Map<String, String> reviewContext) {
    }
  }
  class SimpleReviewAction implements ReviewAction {
    public String getId() {
      return "simple-review-action";
    }

    public String getName() {
      return "Simple action";
    }

    public void execute(Map<String, String> reviewContext) {
    }
  }
}
