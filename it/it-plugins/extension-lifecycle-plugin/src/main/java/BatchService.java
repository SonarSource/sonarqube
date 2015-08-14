import org.sonar.api.BatchExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class BatchService implements BatchExtension {
  private boolean started=false;
  private int projectServices=0;
  private Settings settings;
  
  public BatchService(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    if (!settings.getBoolean("extension.lifecycle")) {
      return;
    }
    System.out.println("Start BatchService");
    if (started) {
      throw new IllegalStateException("Already started");
    }
    if (projectServices>0) {
      throw new IllegalStateException("BatchService must be started before ProjectServices");
    }
    started=true;
  }

  public boolean isStarted() {
    return started;
  }

  public void stop() {
    if (!settings.getBoolean("extension.lifecycle")) {
      return;
    }
    System.out.println("Stop BatchService");
    if (!started) {
      System.out.println("BatchService is not started !");
      System.exit(1);
    }
    if (projectServices!=3) {
      // there are three maven modules in the project extension-lifecycle (pom + 2 modules)
      System.out.println("Invalid nb of ProjectServices: " + projectServices);
      System.exit(1);
    }
    started=false;
  }

  public void incrementProjectService() {
    projectServices++;
  }
}
