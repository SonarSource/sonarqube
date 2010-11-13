package org.sonar.java.ast.check;

import java.util.List;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitor.ClassVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.measures.Metric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

@Rule(key = "ComplexityCheck", isoCategory = IsoCategory.Maintainability)
public class ComplexityCheck extends JavaAstCheck {

  @RuleProperty
  private Integer threshold;

  @Override
  public List<Integer> getWantedTokens() {
    return ClassVisitor.wantedTokens;
  }

  @Override
  public void leaveToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    int complexity = calculateComplexity(currentResource);
    if (complexity > threshold) {
      CheckMessage message = new CheckMessage(this, "Complexity exceeds " + threshold + ".");
      message.setLine(ast.getLineNo());
      SourceFile sourceFile = currentResource.getParent(SourceFile.class);
      sourceFile.log(message);
    }
  }

  private int calculateComplexity(SourceCode sourceCode) {
    int result = 0;
    if (sourceCode.getChildren() != null) {
      for (SourceCode child : sourceCode.getChildren()) {
        result += calculateComplexity(child);
      }
    }
    result += sourceCode.getInt(Metric.COMPLEXITY);
    return result;
  }

  public void setThreshold(int threshold) {
    this.threshold = threshold;
  }

}
