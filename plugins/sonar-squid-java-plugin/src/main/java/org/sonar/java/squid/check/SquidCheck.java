package org.sonar.java.squid.check;

import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.CodeCheck;

public class SquidCheck extends SquidVisitor implements CodeCheck {

  public String getKey() {
    return getClass().getSimpleName();
  }

}
