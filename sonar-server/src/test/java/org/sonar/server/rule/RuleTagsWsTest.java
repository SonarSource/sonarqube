package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.SimpleRequest;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleTagsWsTest {

  @Mock
  RuleTags ruleTags;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new RuleTagsWs(ruleTags));
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/rule_tags");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/rule_tags");
    assertThat(controller.description()).isNotEmpty();

    WebService.Action search = controller.action("list");
    assertThat(search).isNotNull();
    assertThat(search.key()).isEqualTo("list");
    assertThat(search.handler()).isNotNull();
    assertThat(search.since()).isEqualTo("4.2");
    assertThat(search.isPost()).isFalse();

    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.key()).isEqualTo("create");
    assertThat(create.handler()).isNotNull();
    assertThat(create.since()).isEqualTo("4.2");
    assertThat(create.isPost()).isTrue();
    assertThat(create.params()).hasSize(1);
    assertThat(create.param("tag")).isNotNull();
  }

  @Test
  public void list_tags() throws Exception {
    when(ruleTags.listAllTags()).thenReturn(ImmutableList.of("tag1", "tag2", "tag3"));
    SimpleRequest request = new SimpleRequest();
    tester.execute("list", request).assertJson(getClass(), "list.json");
    verify(ruleTags).listAllTags();
  }

  @Test
  public void create_ok() throws Exception {
    String tag = "newtag";
    Long tagId = 42L;
    RuleTagDto newTag = new RuleTagDto().setId(tagId).setTag(tag);
    when(ruleTags.create("newtag")).thenReturn(newTag);
    SimpleRequest request = new SimpleRequest();
    request.setParam("tag", tag);
    tester.execute("create", request).assertJson(getClass(), "create_ok.json");
    verify(ruleTags).create(tag);
  }

  @Test(expected=IllegalArgumentException.class)
  public void create_missing_parameter() throws Exception {
    SimpleRequest request = new SimpleRequest();
    try {
      tester.execute("create", request);
    } finally {
      verifyZeroInteractions(ruleTags);
    }
  }
}
