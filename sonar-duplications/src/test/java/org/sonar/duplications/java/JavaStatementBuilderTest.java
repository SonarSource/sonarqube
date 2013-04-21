/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.duplications.java;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.duplications.DuplicationsTestUtil;
import org.sonar.duplications.statement.Statement;
import org.sonar.duplications.statement.StatementChunker;
import org.sonar.duplications.token.TokenChunker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class JavaStatementBuilderTest {

  private final TokenChunker tokenChunker = JavaTokenProducer.build();
  private final StatementChunker statementChunker = JavaStatementBuilder.build();

  private List<Statement> chunk(String sourceCode) {
    return statementChunker.chunk(tokenChunker.chunk(sourceCode));
  }

  @Test
  public void shouldIgnoreImportStatement() {
    assertThat(chunk("import org.sonar.duplications.java;").size(), is(0));
  }

  @Test
  public void shouldIgnorePackageStatement() {
    assertThat(chunk("package org.sonar.duplications.java;").size(), is(0));
  }

  @Test
  public void shouldHandleAnnotation() {
    List<Statement> statements = chunk("" +
      "@Entity" +
      "@Table(name = \"properties\")" +
      "@Column(updatable = true, nullable = true)");
    assertThat(statements.size(), is(3));
    assertThat(statements.get(0).getValue(), is("@Entity"));
    assertThat(statements.get(1).getValue(), is("@Table(name=$CHARS)"));
    assertThat(statements.get(2).getValue(), is("@Column(updatable=true,nullable=true)"));
  }

  @Test
  public void shouldHandleIf() {
    List<Statement> statements = chunk("if (a > b) { something(); }");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("if(a>b)"));
    assertThat(statements.get(1).getValue(), is("something()"));

    statements = chunk("if (a > b) { something(); } else { somethingOther(); }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("if(a>b)"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("else"));
    assertThat(statements.get(3).getValue(), is("somethingOther()"));

    statements = chunk("if (a > 0) { something(); } else if (a == 0) { somethingOther(); }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("if(a>$NUMBER)"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("elseif(a==$NUMBER)"));
    assertThat(statements.get(3).getValue(), is("somethingOther()"));
  }

  @Test
  public void shouldHandleFor() {
    List<Statement> statements = chunk("for (int i = 0; i < 10; i++) { something(); }");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("for(inti=$NUMBER;i<$NUMBER;i++)"));
    assertThat(statements.get(1).getValue(), is("something()"));

    statements = chunk("for (Item item : items) { something(); }");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("for(Itemitem:items)"));
    assertThat(statements.get(1).getValue(), is("something()"));
  }

  @Test
  public void shouldHandleWhile() {
    List<Statement> statements = chunk("while (i < args.length) { something(); }");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("while(i<args.length)"));
    assertThat(statements.get(1).getValue(), is("something()"));

    statements = chunk("while (true);");
    assertThat(statements.size(), is(1));
    assertThat(statements.get(0).getValue(), is("while(true)"));
  }

  @Test
  public void shouldHandleDoWhile() {
    List<Statement> statements = chunk("do { something(); } while (true);");
    assertThat(statements.size(), is(3));
    assertThat(statements.get(0).getValue(), is("do"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("while(true)"));
  }

  @Test
  public void shouldHandleSwitch() {
    List<Statement> statements = chunk("" +
      "switch (month) {" +
      "  case 1 : monthString=\"January\"; break;" +
      "  case 2 : monthString=\"February\"; break;" +
      "  default: monthString=\"Invalid\";" +
      "}");
    assertThat(statements.size(), is(6));
    assertThat(statements.get(0).getValue(), is("switch(month)"));
    assertThat(statements.get(1).getValue(), is("case$NUMBER:monthString=$CHARS"));
    assertThat(statements.get(2).getValue(), is("break"));
    assertThat(statements.get(3).getValue(), is("case$NUMBER:monthString=$CHARS"));
    assertThat(statements.get(4).getValue(), is("break"));
    assertThat(statements.get(5).getValue(), is("default:monthString=$CHARS"));
  }

  /**
   * See SONAR-2782
   */
  @Test
  public void shouldHandleNestedSwitch() {
    List<Statement> statements = chunk("" +
      "switch (a) {" +
      "  case 'a': case 'b': case 'c': something(); break;" +
      "  case 'd': case 'e': case 'f': somethingOther(); break;" +
      "}");
    assertThat(statements.size(), is(5));
    assertThat(statements.get(0).getValue(), is("switch(a)"));
    assertThat(statements.get(1).getValue(), is("case$CHARS:case$CHARS:case$CHARS:something()"));
    assertThat(statements.get(2).getValue(), is("break"));
    assertThat(statements.get(3).getValue(), is("case$CHARS:case$CHARS:case$CHARS:somethingOther()"));
    assertThat(statements.get(4).getValue(), is("break"));
  }

  @Test
  public void shouldHandleArray() {
    List<Statement> statements = chunk("new Integer[] { 1, 2, 3, 4 };");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("newInteger[]"));
    assertThat(statements.get(1).getValue(), is("{$NUMBER,$NUMBER,$NUMBER,$NUMBER}"));
  }

  /**
   * See SONAR-2837
   */
  @Test
  public void shouldHandleMultidimensionalArray() {
    List<Statement> statements = chunk("new Integer[][] { { 1, 2 }, {3, 4} };");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("newInteger[][]"));
    assertThat(statements.get(1).getValue(), is("{{$NUMBER,$NUMBER},{$NUMBER,$NUMBER}}"));

    statements = chunk("new Integer[][] { null, {3, 4} };");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("newInteger[][]"));
    assertThat(statements.get(1).getValue(), is("{null,{$NUMBER,$NUMBER}}"));
  }

  @Test
  public void shouldHandleTryCatch() {
    List<Statement> statements;
    statements = chunk("try { } catch (Exception e) { }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("{}"));
    assertThat(statements.get(2).getValue(), is("catch(Exceptione)"));
    assertThat(statements.get(3).getValue(), is("{}"));

    statements = chunk("try { something(); } catch (Exception e) { }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("catch(Exceptione)"));
    assertThat(statements.get(3).getValue(), is("{}"));

    statements = chunk("try { something(); } catch (Exception e) { onException(); }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("catch(Exceptione)"));
    assertThat(statements.get(3).getValue(), is("onException()"));

    statements = chunk("try { something(); } catch (Exception1 e) { onException1(); } catch (Exception2 e) { onException2(); }");
    assertThat(statements.size(), is(6));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("catch(Exception1e)"));
    assertThat(statements.get(3).getValue(), is("onException1()"));
    assertThat(statements.get(4).getValue(), is("catch(Exception2e)"));
    assertThat(statements.get(5).getValue(), is("onException2()"));
  }

  @Test
  public void shouldHandleTryFinnaly() {
    List<Statement> statements;
    statements = chunk("try { } finally { }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("{}"));
    assertThat(statements.get(2).getValue(), is("finally"));
    assertThat(statements.get(3).getValue(), is("{}"));

    statements = chunk("try { something(); } finally { }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("finally"));
    assertThat(statements.get(3).getValue(), is("{}"));

    statements = chunk("try { something(); } finally { somethingOther(); }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("finally"));
    assertThat(statements.get(3).getValue(), is("somethingOther()"));
  }

  @Test
  public void shouldHandleTryCatchFinally() {
    List<Statement> statements;
    statements = chunk("try { } catch (Exception e) {} finally { }");
    assertThat(statements.size(), is(6));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("{}"));
    assertThat(statements.get(2).getValue(), is("catch(Exceptione)"));
    assertThat(statements.get(3).getValue(), is("{}"));
    assertThat(statements.get(4).getValue(), is("finally"));
    assertThat(statements.get(5).getValue(), is("{}"));

    statements = chunk("try { something(); } catch (Exception e) { onException(); } finally { somethingOther(); }");
    assertThat(statements.size(), is(6));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("catch(Exceptione)"));
    assertThat(statements.get(3).getValue(), is("onException()"));
    assertThat(statements.get(4).getValue(), is("finally"));
    assertThat(statements.get(5).getValue(), is("somethingOther()"));
  }

  /**
   * Java 7.
   */
  @Test
  public void shouldHandleMultiCatch() {
    List<Statement> statements;
    statements = chunk("try { } catch (Exception1 | Exception2 e) { }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("{}"));
    assertThat(statements.get(2).getValue(), is("catch(Exception1|Exception2e)"));
    assertThat(statements.get(3).getValue(), is("{}"));

    statements = chunk("try { something(); } catch (Exception1 | Exception2 e) { }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("catch(Exception1|Exception2e)"));
    assertThat(statements.get(3).getValue(), is("{}"));

    statements = chunk("try { something(); } catch (Exception1 | Exception2 e) { onException(); }");
    assertThat(statements.size(), is(4));
    assertThat(statements.get(0).getValue(), is("try"));
    assertThat(statements.get(1).getValue(), is("something()"));
    assertThat(statements.get(2).getValue(), is("catch(Exception1|Exception2e)"));
    assertThat(statements.get(3).getValue(), is("onException()"));
  }

  /**
   * Java 7.
   */
  @Test
  public void shouldHandleTryWithResource() {
    List<Statement> statements;
    statements = chunk("try (FileInputStream in = new FileInputStream()) {}");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("try(FileInputStreamin=newFileInputStream())"));
    assertThat(statements.get(1).getValue(), is("{}"));

    statements = chunk("try (FileInputStream in = new FileInputStream(); FileOutputStream out = new FileOutputStream()) {}");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("try(FileInputStreamin=newFileInputStream();FileOutputStreamout=newFileOutputStream())"));
    assertThat(statements.get(1).getValue(), is("{}"));

    statements = chunk("try (FileInputStream in = new FileInputStream(); FileOutputStream out = new FileOutputStream();) {}");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("try(FileInputStreamin=newFileInputStream();FileOutputStreamout=newFileOutputStream();)"));
    assertThat(statements.get(1).getValue(), is("{}"));

    statements = chunk("try (FileInputStream in = new FileInputStream()) { something(); }");
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0).getValue(), is("try(FileInputStreamin=newFileInputStream())"));
    assertThat(statements.get(1).getValue(), is("something()"));
  }

  @Test
  public void realExamples() {
    assertThat(chunk(DuplicationsTestUtil.findFile("/java/MessageResources.java")).size(), greaterThan(0));
    assertThat(chunk(DuplicationsTestUtil.findFile("/java/RequestUtils.java")).size(), greaterThan(0));
  }

  private List<Statement> chunk(File file) {
    Reader reader = null;
    try {
      reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8);
      return statementChunker.chunk(tokenChunker.chunk(reader));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

}
