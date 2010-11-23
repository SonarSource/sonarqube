package org.sonar.java.squid.visitor;

import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.indexer.SquidIndex;

public class SquidVisitor implements CodeVisitor {

  SquidIndex index;

  public void visitClass(SourceClass sourceClass) {
  }

  public void visitMethod(SourceMethod sourceMethod) {
  }

  protected final SourceFile getSourceFile(SourceCode sourceCode) {
    return sourceCode.getParent(SourceFile.class);
  }

  public void setSquidIndex(SquidIndex index) {
    this.index = index;
  }

}
