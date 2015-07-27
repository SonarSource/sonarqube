import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.config.GlobalPropertyChangeHandler;

@Properties(@Property(key = "globalPropertyChange.received", name = "Check that extension has correctly been notified by global property change", category = "fake"))
public final class FakeGlobalPropertyChange extends GlobalPropertyChangeHandler {

  @Override
  public void onChange(PropertyChange propertyChange) {
    System.out.println("Received change: " + propertyChange);
  }
}
