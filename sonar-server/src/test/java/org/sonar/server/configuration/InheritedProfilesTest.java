package org.sonar.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class InheritedProfilesTest extends AbstractDbUnitTestCase {
  private ProfilesManager profilesManager;

  @Before
  public void setUp() {
    profilesManager = new ProfilesManager(getSession(), null, null, null);
  }

  @Test
  public void shouldSetParent() {
    setupData("shouldSetParent");
    profilesManager.changeParentProfile(2, 1);
    checkTables("shouldSetParent", "active_rules", "rules_profiles");
  }

  @Test
  public void shouldChangeParent() {
    setupData("shouldChangeParent");
    profilesManager.changeParentProfile(3, 2);
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
