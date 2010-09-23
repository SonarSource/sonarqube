package org.sonar.api.checks.checkers;

import org.sonar.check.Check;
import org.sonar.check.CheckProperty;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;

@Check(isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
class CheckWithUnsupportedPropertyType {

  @CheckProperty
  private StringBuilder max = null;

}
