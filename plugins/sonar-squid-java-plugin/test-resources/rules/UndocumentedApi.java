/**
 * no violation, because documented
 */
class UndocumentedApi {
  private String key;

  public UndocumentedApi() { // no violation, because empty constructor
  }

  public void run() { // violation
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
