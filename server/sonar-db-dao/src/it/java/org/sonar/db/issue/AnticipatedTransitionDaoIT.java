package org.sonar.db.issue;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class AnticipatedTransitionDaoIT {
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final AnticipatedTransitionDao underTest = db.getDbClient().anticipatedTransitionDao();

  @Test
  public void select_anticipated_transition() {
    final String projectUuid = "project147852";
    String atUuid = "uuid_123456";
    AnticipatedTransitionDto transition = new AnticipatedTransitionDto(
      atUuid,
      projectUuid,
      "userUuid",
      "transition",
      "status",
      "comment",
      1,
      "message",
      "lineHash",
      "ruleKey");

    // insert one
    underTest.insert(db.getSession(), transition);

    // select all
    var anticipatedTransitionDtos = underTest.selectByProjectUuid(db.getSession(), projectUuid);
    assertThat(anticipatedTransitionDtos).hasSize(1);
    assertThat(anticipatedTransitionDtos.get(0))
      .extracting("uuid").isEqualTo(atUuid);

    // delete one
    underTest.delete(db.getSession(), atUuid);

    // select all
    var anticipatedTransitionDtosDeleted = underTest.selectByProjectUuid(db.getSession(), projectUuid);
    assertThat(anticipatedTransitionDtosDeleted).isEmpty();
  }
}
