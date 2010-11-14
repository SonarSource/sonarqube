package org.sonar.java.ast.check;

import java.util.List;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitor.MethodVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

@Rule(key = "MethodComplexityCheck", isoCategory = IsoCategory.Maintainability)
public class MethodComplexityCheck extends JavaAstCheck {

  @RuleProperty
  private Integer threshold;

  @Override
  public List<Integer> getWantedTokens() {
    return MethodVisitor.wantedTokens;
  }

  @Override
  public void leaveToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    int complexity = currentResource.getInt(Metric.COMPLEXITY) + currentResource.getInt(Metric.BRANCHES) + 1;
    if (complexity > threshold) {
      CheckMessage message = new CheckMessage(this, "Method complexity exceeds " + threshold + ".");
      message.setLine(ast.getLineNo());
      message.setCost(complexity - threshold);
      SourceFile sourceFile = currentResource.getParent(SourceFile.class);
      sourceFile.log(message);
    }
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

}
