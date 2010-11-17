package org.sonar.java.bytecode.check;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.measures.Metric;

@Rule(key = "DIT", name = "DIT", isoCategory = IsoCategory.Maintainability)
public class DITCheck extends BytecodeCheck {

  @RuleProperty(description = "Threshold.")
  private Integer threshold;

  @Override
  public void visitClass(AsmClass asmClass) {
    SourceClass sourceClass = getSourceClass(asmClass);
    int dit = sourceClass.getInt(Metric.DIT);
    if (dit > threshold) {
      CheckMessage message = new CheckMessage(this, "Depth of inheritance exceeds " + threshold + ".");
      message.setLine(sourceClass.getStartAtLine());
      getSourceFile(asmClass).log(message);
    }
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }
}
