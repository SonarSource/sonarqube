package org.sonar.batch.components;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PastViolationsLoaderTest extends AbstractDbUnitTestCase {

  private ResourcePersister resourcePersister;
  private PastViolationsLoader loader;

  @Before
  public void setUp() {
    setupData("shared");
    resourcePersister = mock(ResourcePersister.class);
    loader = new PastViolationsLoader(getSession(), resourcePersister);
  }

  @Test
  public void shouldGetPastResourceViolations() {
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);
    doReturn(snapshot).when(resourcePersister)
        .getSnapshot(any(Resource.class));
    doReturn(snapshot).when(resourcePersister)
        .getLastSnapshot(any(Snapshot.class), anyBoolean());

    List<RuleFailureModel> violations = loader.getPastViolations(new JavaFile("file"));

    assertThat(violations.size(), is(2));
  }

  @Test
  public void shouldReturnEmptyList() {
    List<RuleFailureModel> violations = loader.getPastViolations(null);

    assertThat(violations, notNullValue());
    assertThat(violations.size(), is(0));
  }

}
