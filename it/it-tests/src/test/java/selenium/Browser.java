package selenium;

import org.openqa.selenium.firefox.FirefoxDriver;

public enum Browser {
  FIREFOX;

  private final ThreadLocal<SeleniumDriver> perThreadDriver = new ThreadLocal<SeleniumDriver>() {
    @Override
    protected SeleniumDriver initialValue() {
      return ThreadSafeDriver.makeThreadSafe(new FirefoxDriver());
    }
  };

  public SeleniumDriver getDriverForThread() {
    return perThreadDriver.get();
  }
}
