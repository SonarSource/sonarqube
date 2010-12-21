package org.sonar.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class InheritedProfilesTest extends AbstractDbUnitTestCase {
  private ProfilesManager profilesManager;

  @Before
  public void setUp() {
    profilesManager = new ProfilesManager(getSession(), null);
  }

  @Test
  public void shouldCheckCycles() {
    setupData("shouldCheckCycles");
    RulesProfile level1 = profilesManager.getProfile("java", "level1");
    RulesProfile level2 = profilesManager.getProfile("java", "level2");
    RulesProfile level3 = profilesManager.getProfile("java", "level3");

    assertThat(profilesManager.getParentProfile(level1), nullValue());
    assertThat(profilesManager.getParentProfile(level2), is(level1));
    assertThat(profilesManager.getParentProfile(level3), is(level2));

    assertThat(profilesManager.isCycle(level1, level1), is(true));
    assertThat(profilesManager.isCycle(level1, level3), is(true));
    assertThat(profilesManager.isCycle(level1, level2), is(true));
    assertThat(profilesManager.isCycle(level2, level3), is(true));
  }

  @Test
  public void shouldSetParent() {
    setupData("shouldSetParent");
    profilesManager.changeParentProfile(2, "parent");
    checkTables("shouldSetParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldChangeParent() {
    setupData("shouldChangeParent");
    profilesManager.changeParentProfile(3, "new_parent");
    checkTables("shouldChangeParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldRemoveParent() {
    setupData("shouldRemoveParent");
    profilesManager.changeParentProfile(2, null);
    checkTables("shouldRemoveParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldDeactivateInChildren() {
    setupData("shouldDeactivateInChildren");
    profilesManager.deactivated(1, 1);
    checkTables("shouldDeactivateInChildren", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldActivateInChildren() {
    setupData("shouldActivateInChildren");
    profilesManager.activatedOrChanged(1, 1);
    checkTables("shouldActivateInChildren", "active_rules", "rules_profiles", "active_rule_parameters");
  }

}
