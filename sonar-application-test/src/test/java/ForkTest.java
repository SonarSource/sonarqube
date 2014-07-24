import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ForkTest {

  private Orchestrator orchestrator;

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void start_and_stop() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    orchestrator = builder.build();
    orchestrator.start();

    String json = orchestrator.getServer().wsClient().get("/api/rules/search", Collections.<String, Object>emptyMap());
    assertThat(json).startsWith("{").endsWith("}");

    orchestrator.stop();
    try {
      orchestrator.getServer().wsClient().get("/api/rules/search", Collections.<String, Object>emptyMap());
      fail("Server is not stopped");
    } catch (Exception e) {
      // ok
    }
  }

}
