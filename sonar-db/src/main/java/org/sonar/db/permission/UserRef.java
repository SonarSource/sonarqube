package org.sonar.db.permission;

public class UserRef {
  private String login;
  private String email;
  private String name;

  public String getLogin() {
    return login;
  }

  public UserRef setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getEmail() {
    return email;
  }

  public UserRef setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getName() {
    return name;
  }

  public UserRef setName(String name) {
    this.name = name;
    return this;
  }
}
