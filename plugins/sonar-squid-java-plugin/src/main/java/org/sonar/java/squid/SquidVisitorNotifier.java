package org.sonar.java.squid;

import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.indexer.SquidIndex;

public class SquidVisitorNotifier {

  private final SourceFile sourceFile;
  private final SquidVisitor[] squidVisitors;

  public SquidVisitorNotifier(SourceFile sourceFile, SquidVisitor[] squidVisitors) {
    this.sourceFile = sourceFile;
    this.squidVisitors = new SquidVisitor[squidVisitors.length];
    System.arraycopy(squidVisitors, 0, this.squidVisitors, 0, squidVisitors.length);
  }

  public void notifyVisitors(SquidIndex indexer) {
    callVisitFile();
  }

  private void callVisitFile() {
    for (SquidVisitor visitor : squidVisitors) {
      visitor.visitFile(sourceFile);
    }
    callVisitClass(sourceFile);
  }

  private void callVisitClass(SourceFile sourceFile) {
    if ( !sourceFile.hasChildren()) {
      return;
    }
    for (SourceCode sourceCode : sourceFile.getChildren()) {
      if (sourceCode instanceof SourceClass) {
        SourceClass sourceClass = (SourceClass) sourceCode;
        for (SquidVisitor visitor : squidVisitors) {
          visitor.visitClass(sourceClass);
        }
        callVisitMethod(sourceClass);
      }
    }
  }

  private void callVisitMethod(SourceClass sourceClass) {
    if ( !sourceClass.hasChildren()) {
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
