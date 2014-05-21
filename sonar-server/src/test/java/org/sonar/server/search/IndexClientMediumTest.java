package org.sonar.server.search;

import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.server.rule2.index.RuleIndex;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;

public class IndexClientMediumTest{

  @ClassRule
  public static ServerTester tester = new ServerTester();


  @Test
  public void get_index_class(){
    IndexClient indexClient = tester.get(IndexClient.class);
    assertThat(tester.get(RuleIndex.class))
      .isEqualTo(indexClient.get(RuleIndex.class));
  }
}