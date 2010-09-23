package org.sonar.api.checks.checkers;

import org.sonar.check.Check;
import org.sonar.check.CheckProperty;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

@Check(isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
class CheckerWithStringProperty {

  @CheckProperty(key = "maiximum")
  private String max;

  public String getMax() {
    return max;
  }
}
