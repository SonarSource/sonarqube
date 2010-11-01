package org.sonar.java.ast.check;

import org.sonar.java.ast.visitor.JavaAstVisitor;
import org.sonar.squid.api.CodeCheck;

public abstract class JavaAstCheck extends JavaAstVisitor implements CodeCheck {

  public String getKey() {
    return getClass().getSimpleName();
  }
}
