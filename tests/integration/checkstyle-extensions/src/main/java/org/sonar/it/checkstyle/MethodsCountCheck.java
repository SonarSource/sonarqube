package org.sonar.it.checkstyle;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class MethodsCountCheck extends Check {

  private int minMethodsCount = 1;
  
  private int methodsCount = 0;
  private DetailAST classAST = null;
  
  public void setMinMethodsCount(int minMethodsCount) {
    this.minMethodsCount = minMethodsCount;
  }
  
  public int[] getDefaultTokens() {
    return new int[]{TokenTypes.CLASS_DEF, TokenTypes.METHOD_DEF};
  }
  
  public void beginTree(DetailAST rootAST) {
    methodsCount = 0;
    classAST = null;
  }
  
  public void visitToken(DetailAST ast) {
    //ensure this is an unit test.
    if ( ast.getType() == TokenTypes.CLASS_DEF ) {
      classAST = ast;
      
    } else if ( ast.getType() == TokenTypes.METHOD_DEF ) {
      methodsCount++;
    }
  }

  public void finishTree(DetailAST rootAST) {
    super.finishTree(rootAST);
    if (classAST != null && methodsCount < minMethodsCount) {
        log(classAST.getLineNo(), classAST.getColumnNo(), "Too few methods (" + methodsCount + ") in class" );
    }
  }
}
