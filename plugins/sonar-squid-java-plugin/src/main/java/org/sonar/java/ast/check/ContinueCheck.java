package org.sonar.java.ast.check;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.IsoCategory;
import org.sonar.check.Rule;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

@Rule(key = "AvoidUsageOfContinue", isoCategory = IsoCategory.Maintainability)
public class ContinueCheck extends JavaAstCheck {

  @Override
  public List<Integer> getWantedTokens() {
    return wantedTokens;
  }

  @Override
  public void visitToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    CheckMessage message = new CheckMessage(this, "Avoid usage of continue");
    message.setLine(ast.getLineNo());
    SourceFile sourceFile = currentResource.getParent(SourceFile.class);
    sourceFile.log(message);
  }

  private static final List<Integer> wantedTokens = Arrays.asList(TokenTypes.LITERAL_CONTINUE);

}
