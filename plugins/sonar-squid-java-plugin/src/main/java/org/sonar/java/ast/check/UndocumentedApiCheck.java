package org.sonar.java.ast.check;

import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitor.PublicApiVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.CodeCheck;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

@Rule(key = "UndocumentedApi", name = "Undocumented API", isoCategory = IsoCategory.Usability, priority = Priority.MAJOR, description = "")
public class UndocumentedApiCheck extends PublicApiVisitor implements CodeCheck {

  @Override
  public void visitToken(DetailAST ast) {
    if (isPublicApi(ast) && !isDocumentedApi(ast)) {
      SourceCode currentResource = peekSourceCode();
      SourceFile sourceFile = currentResource.getParent(SourceFile.class);
      CheckMessage message = new CheckMessage(this, "Avoid undocumented API");
      message.setLine(ast.getLineNo());
      sourceFile.log(message);
    }
  }

  public String getKey() {
    return getClass().getSimpleName();
  }

}
