package org.sonar.api.checks;

import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.check.Rule;

@Rule(key = "CheckWithKey", isoCategory = IsoCategory.Efficiency, priority = Priority.CRITICAL)
public class CheckWithKey {

}
