package org.sonar.api.checks.checkers;

import org.sonar.check.Check;
import org.sonar.check.CheckProperty;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

/**
 * Created by IntelliJ IDEA.
 * User: simonbrandhof
 * Date: Sep 14, 2010
 * Time: 11:20:57 AM
 * To change this template use File | Settings | File Templates.
 */
@Check(isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
class CheckerWithPrimitiveProperties {

  @CheckProperty(description = "Maximum threshold")
  private int max = 50;

  @CheckProperty
  private boolean active;

  public int getMax() {
    return max;
  }

  public boolean isActive() {
    return active;
  }
}
