package org.sonar.java.squid.check;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.measures.Metric;

@Rule(key = "MethodComplexityCheck", name = "MethodComplexityCheck", isoCategory = IsoCategory.Maintainability)
public class MethodComplexityCheck extends SquidCheck {

  @RuleProperty(description = "Threshold.")
  private Integer threshold;

  @Override
  public void visitMethod(SourceMethod sourceMethod) {
    int complexity = sourceMethod.getInt(Metric.COMPLEXITY);
    if (complexity > threshold) {
      CheckMessage message = new CheckMessage(this, "Method complexity exceeds " + threshold + ".");
      message.setLine(sourceMethod.getStartAtLine());
      message.setCost(complexity - threshold);
      getSourceFile(sourceMethod).log(message);
    }
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

}
