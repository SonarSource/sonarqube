import org.sonar.api.BatchExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class EncryptionVerifier implements BatchExtension {
  private Settings settings;

  public EncryptionVerifier(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    System.out.println("Start EncryptionVerifier");

    String decryptedValue = settings.getString("encryptedProperty");
    if (!"this is a secret".equals(decryptedValue)) {
      throw new IllegalStateException("The property 'encryptedProperty' can not be decrypted");
    }
  }
}
