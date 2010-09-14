package org.sonar.api.checks.profiles;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

/**
 * Created by IntelliJ IDEA.
 * User: simonbrandhof
 * Date: Sep 14, 2010
 * Time: 11:02:28 AM
 * To change this template use File | Settings | File Templates.
 */
@org.sonar.check.Check(priority = Priority.BLOCKER, isoCategory = IsoCategory.Maintainability)
@BelongsToProfile(title = "profile one", priority = Priority.MINOR)
class FakeCheckOne {

}
