import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;

/**
 * As many instances as projects (maven modules)
 */
public class ProjectService implements BatchExtension {

  private BatchService batchService;
  private Settings settings;

  public ProjectService(BatchService batchService, Settings settings) {
    this.batchService = batchService;
    this.settings = settings;
  }

  public void start() {
    if (!settings.getBoolean("extension.lifecycle")) {
      return;
    }
    System.out.println("Start ProjectService");

    if (!batchService.isStarted()) {
      throw new IllegalStateException("ProjectService must be started after BatchService");
    }
    batchService.incrementProjectService();
  }

  public void stop() {
    if (!settings.getBoolean("extension.lifecycle")) {
      return;
    }
    System.out.println("Stop ProjectService");
    if (!batchService.isStarted()) {
      System.out.println("ProjectService must be stopped before BatchService");
      System.exit(1);
    }
  }
}
