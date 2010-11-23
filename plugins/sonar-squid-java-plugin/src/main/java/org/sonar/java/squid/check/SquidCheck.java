package org.sonar.java.squid.check;

import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.CodeCheck;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceMethod;

public class SquidCheck implements SquidVisitor, CodeCheck {

  public String getKey() {
    return getClass().getSimpleName();
  }

  public void visitClass(SourceClass sourceClass) {
  }

  public void visitMethod(SourceMethod sourceMethod) {
  }

}
