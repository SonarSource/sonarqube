package org.sonar.plugins.core.timemachine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

public class PastViolationsLoaderTest extends AbstractDbUnitTestCase {

  private PastViolationsLoader loader;

  @Before
  public void setUp() {
    setupData("shared");
    loader = new PastViolationsLoader(getSession(), null);
  }

  @Test
  public void shouldGetPastResourceViolations() {
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);
    List<RuleFailureModel> violations = loader.getPastViolations(snapshot);

    assertThat(violations.size(), is(2));
  }

  @Test
  public void shouldReturnEmptyList() {
    List<RuleFailureModel> violations = loader.getPastViolations(null);

    assertThat(violations, notNullValue());
    assertThat(violations.size(), is(0));
  }

}
