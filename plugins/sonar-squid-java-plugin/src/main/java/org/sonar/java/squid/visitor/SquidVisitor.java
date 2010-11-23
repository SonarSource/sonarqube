package org.sonar.java.squid.visitor;

import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceMethod;

public interface SquidVisitor extends CodeVisitor {

  void visitClass(SourceClass sourceClass);

  void visitMethod(SourceMethod sourceMethod);

}
