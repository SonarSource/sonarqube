/**
 * no violation, because documented
 */
class UndocumentedApi {
  private String key;

  public UndocumentedApi() { // no violation, because empty constructor
  }

  public UndocumentedApi(String key) { // violation
    this.key = key;
  }

  public void run() { // violation
  }

  public interface InnerUndocumentedInterface { // violation
  }

  /**
   * no violation, because documented
   */
  public void run2() {
  }

  public void setKey(String key) { // no violation, because setter
    this.key = key;
  }

  public String getKey() { // no violation, because getter
    return key;
  }

  @Override
  public String toString() { // no violation, because method with override annotation
    return key;
  }

}
