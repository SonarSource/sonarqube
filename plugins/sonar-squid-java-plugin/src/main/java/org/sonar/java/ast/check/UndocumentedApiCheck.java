package org.sonar.java.ast.check;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.IsoCategory;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.PatternUtils;
import org.sonar.java.ast.visitor.PublicApiVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

import java.util.List;

@Rule(key = "UndocumentedApi", name = "Undocumented API", isoCategory = IsoCategory.Usability, priority = Priority.MAJOR, description = "<p>Check that each public class, interface, method and constructor has a Javadoc comment. " +
    "The following public methods/constructors are not concerned by this rule :</p>" +
    "<ul><li>Getter / Setter</li>" +
    "<li>Method with @Override annotation</li>" +
    "<li>Empty constructor</li></ul>")
public class UndocumentedApiCheck extends JavaAstCheck {

  @RuleProperty(description = "Optional. If this property is not defined, all classes should adhere to this constraint. Ex : **.api.**")
  private String forClasses = new String();

  private WildcardPattern[] patterns;

  @Override
  public List<Integer> getWantedTokens() {
    return PublicApiVisitor.TOKENS;
  }

  @Override
  public void visitToken(DetailAST ast) {
    SourceCode currentResource = peekSourceCode();
    SourceClass sourceClass = peekParentClass();
    if (WildcardPattern.match(getPatterns(), sourceClass.getKey())) {
      if (PublicApiVisitor.isPublicApi(ast) && !PublicApiVisitor.isDocumentedApi(ast, getFileContents())) {
        SourceFile sourceFile = currentResource.getParent(SourceFile.class);
        CheckMessage message = new CheckMessage(this, "Avoid undocumented API");
        message.setLine(ast.getLineNo());
        sourceFile.log(message);
      }
    }
  }

  private WildcardPattern[] getPatterns() {
    if (patterns == null) {
      patterns = PatternUtils.createPatterns(StringUtils.defaultIfEmpty(forClasses, "**"));
    }
    return patterns;
  }

  public String getForClasses() {
    return forClasses;
  }

  public void setForClasses(String forClasses) {
    this.forClasses = forClasses;
  }

}
