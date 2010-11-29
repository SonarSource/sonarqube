package org.sonar.java.squid;

import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;

public class SquidVisitorNotifier {

  private final SourceFile sourceFile;
  private final SquidVisitor[] squidVisitors;

  public SquidVisitorNotifier(SourceFile sourceFile, SquidVisitor... squidVisitors) {
    this.sourceFile = sourceFile;
    this.squidVisitors = new SquidVisitor[squidVisitors.length];
    System.arraycopy(squidVisitors, 0, this.squidVisitors, 0, squidVisitors.length);
  }

  public void notifyVisitors() {
    callVisitFile();
  }

  private void callVisitFile() {
    for (SquidVisitor visitor : squidVisitors) {
      visitor.visitFile(sourceFile);
    }
    visitChildren(sourceFile);
  }

  private void visitChildren(SourceCode sourceCode) {
    if (sourceCode instanceof SourceClass) {
      for (SquidVisitor visitor : squidVisitors) {
        visitor.visitClass((SourceClass) sourceCode);
      }
    } else if (sourceCode instanceof SourceMethod) {
      for (SquidVisitor visitor : squidVisitors) {
        visitor.visitMethod((SourceMethod) sourceCode);
      }
    }

    if (sourceCode.hasChildren()) {
      for (SourceCode child : sourceCode.getChildren()) {
        visitChildren(child);
      }
    }
  }
}
