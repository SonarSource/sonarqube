import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TempFolder;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@Properties({
  @Property(
    key = TempFolderExtension.CREATE_TEMP_FILES,
    type = PropertyType.BOOLEAN,
    name = "Property to decide if it should create temp files",
    defaultValue = "false")
})
public class TempFolderExtension implements ServerExtension {

  private static final Logger LOG = Loggers.get(TempFolderExtension.class);

  public static final String CREATE_TEMP_FILES = "sonar.createTempFiles";

  private Settings settings;

  private TempFolder tempFolder;

  public TempFolderExtension(Settings settings, TempFolder tempFolder) {
    this.settings = settings;
    this.tempFolder = tempFolder;
    start();
  }

  public void start() {
    if (settings.getBoolean(CREATE_TEMP_FILES)) {
      LOG.info("Creating temp directory: " + tempFolder.newDir("sonar-it").getAbsolutePath());
      LOG.info("Creating temp file: " + tempFolder.newFile("sonar-it", ".txt").getAbsolutePath());
    }
  }

}
