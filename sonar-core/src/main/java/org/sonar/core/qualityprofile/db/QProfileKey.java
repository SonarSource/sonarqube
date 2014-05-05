package org.sonar.core.qualityprofile.db;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * Created by gamars on 05/05/14.
 *
 * @since 4.4
 */
public class QProfileKey implements Serializable{
  private final String qProfile, lang;

  protected QProfileKey(String qProfile, String lang) {
    this.lang = lang;
    this.qProfile = qProfile;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static QProfileKey of(String qProfile, String lang) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(qProfile), "QProfile must be set");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(lang), "Lang must be set");
    return new QProfileKey(qProfile, lang);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static QProfileKey parse(String s) {
    String[] split = s.split(":");
    Preconditions.checkArgument(split.length == 3, "Bad format of activeRule key: " + s);
    return QProfileKey.of(split[0], split[1]);
  }

  /**
   * Never null
   */
  public String lang() {
    return lang;
  }

  /**
   * Never null
   */
  public String qProfile() {
    return qProfile;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QProfileKey qProfileKey = (QProfileKey) o;
    if (!lang.equals(qProfileKey.lang)) {
      return false;
    }
    if (!qProfile.equals(qProfileKey.qProfile)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = qProfile.hashCode();
    result = 31 * result + lang.hashCode();
    return result;
  }

  /**
   * Format is "qProfile:lang", for example "Java:javascript"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", qProfile, lang);
  }
}
