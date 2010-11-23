package org.sonar.java.squid;

import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.indexer.SquidIndex;

public class SquidVisitorNotifier {

  private final SourceClass sourceClass;
  private final SquidVisitor[] squidVisitors;

  public SquidVisitorNotifier(SourceClass sourceClass, SquidVisitor[] squidVisitors) {
    this.sourceClass = sourceClass;
    this.squidVisitors = new SquidVisitor[squidVisitors.length];
    System.arraycopy(squidVisitors, 0, this.squidVisitors, 0, squidVisitors.length);
  }

  public void notifyVisitors(SquidIndex indexer) {
    callVisitClass();
    callVisitMethod();
  }

  private void callVisitClass() {
    for (SquidVisitor visitor : squidVisitors) {
      visitor.visitClass(sourceClass);
    }
  }

  private void callVisitMethod() {
    if (sourceClass.getChildren() == null) { // TODO Most probably SourceCode#hasChildren() shouldn't be protected
      return;
    }
    for (SourceCode sourceCode : sourceClass.getChildren()) {
      if (sourceCode instanceof SourceMethod) {
        SourceMethod sourceMethod = (SourceMethod) sourceCode;
        for (SquidVisitor visitor : squidVisitors) {
          visitor.visitMethod(sourceMethod);
        }
      }
    }
  }
}
