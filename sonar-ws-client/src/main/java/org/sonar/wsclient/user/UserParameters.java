package org.sonar.wsclient.user;

import java.util.HashMap;
import java.util.Map;

public class UserParameters {

  private final Map<String, Object> params = new HashMap<String, Object>();

  private UserParameters() {
  }

  public static UserParameters create() {
    return new UserParameters();
  }

  public Map<String, Object> urlParams() {
    return params;
  }

  public UserParameters login(String s) {
    params.put("login", s);
    return this;
  }

  public UserParameters name(String s) {
    params.put("name", s);
    return this;
  }

  public UserParameters password(String s) {
    params.put("password", s);
    return this;
  }

  public UserParameters passwordConfirmation(String s) {
    params.put("password_confirmation", s);
    return this;
  }

  public UserParameters email(String s) {
    params.put("email", s);
    return this;
  }
}
