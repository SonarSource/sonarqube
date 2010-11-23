package org.sonar.java.squid;

import java.util.Collection;
import java.util.Collections;

import org.sonar.java.squid.check.SquidCheck;
import org.sonar.java.squid.visitor.SquidVisitor;
import org.sonar.squid.api.CodeScanner;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.SquidIndex;

public class SquidScanner extends CodeScanner<CodeVisitor> {

  private SquidIndex indexer;

  public SquidScanner(SquidIndex indexer) {
    this.indexer = indexer;
  }

  public void scan() {
    Collection<SourceCode> classes = indexer.search(new QueryByType(SourceClass.class));
    notifySquidVisitors(classes);
  }

  private void notifySquidVisitors(Collection<SourceCode> classes) {
    SquidVisitor[] visitorArray = getVisitors().toArray(new SquidVisitor[getVisitors().size()]);
    for (SourceCode sourceClass : classes) {
      SquidVisitorNotifier visitorNotifier = new SquidVisitorNotifier((SourceClass) sourceClass, visitorArray);
      visitorNotifier.notifyVisitors(indexer);
    }
  }

  @Override
  public Collection<Class<? extends CodeVisitor>> getVisitorClasses() {
    return Collections.emptyList();
  }

  @Override
  public void accept(CodeVisitor visitor) {
    if (visitor instanceof SquidCheck) {
      super.accept(visitor);
    }
  }

}
