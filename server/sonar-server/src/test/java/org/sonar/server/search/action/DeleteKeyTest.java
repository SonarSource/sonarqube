package org.sonar.server.search.action;

import org.elasticsearch.action.delete.DeleteRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.server.search.Index;
import org.sonar.server.search.IndexDefinition;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DeleteKeyTest {

  IndexDefinition TEST_INDEX = IndexDefinition.createFor("TEST", "TESTING");

  @Mock
  Index index;

  @Before
  public void setUp() throws Exception {
    when(index.getIndexName()).thenReturn(TEST_INDEX.getIndexName());

  }

  @Test
  public void get_delete_request() throws Exception {
    String key = "test_key";
    DeleteKey<String> deleteAction = new DeleteKey<String>(TEST_INDEX.getIndexType(), key);

    List<DeleteRequest> requests = deleteAction.doCall(index);
    assertThat(requests).hasSize(1);

    DeleteRequest request = requests.get(0);
    assertThat(request.type()).isEqualTo(TEST_INDEX.getIndexType());
    assertThat(request.index()).isEqualTo(TEST_INDEX.getIndexName());
    assertThat(request.id()).isEqualTo(key);
    assertThat(request.refresh()).isTrue();
  }
}
