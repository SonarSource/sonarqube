package org.sonar.java.squid.check;

import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

@Rule(key = "MaximumInheritanceDepth", name = "Avoid too deep inheritance tree", isoCategory = IsoCategory.Maintainability,
    priority = Priority.MAJOR, description = "<p>Inheritance is certainly one of the most valuable concept of object-oriented "
        + "programming. It's a way to compartmentalize and reuse code by creating collections of attributes and behaviors called "
        + "classes which can be based on previously created classes. But abusing of this concept by creating a deep inheritance tree "
        + "can lead to very complex and unmaintainable source code.</p>"
        + "<p>Most of the time a too deep inheritance tree is due to bad object oriented design which has led to systematically use "
        + "'inheritance' when 'composition' would suit better.</p>")
public class DITCheck extends SquidCheck {

  @RuleProperty(description = "Maximum depth of the inheritance tree.", defaultValue = "5")
  private Integer max;

  @Override
  public void visitClass(SourceClass sourceClass) {
    int dit = sourceClass.getInt(Metric.DIT);
    if (dit > max) {
      CheckMessage message = new CheckMessage(this, "This class has " + dit + " parents which is greater than " + max + " authorized.");
      message.setLine(sourceClass.getStartAtLine());
      message.setCost(dit - max);
      sourceClass.getParent(SourceFile.class).log(message);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }
}
