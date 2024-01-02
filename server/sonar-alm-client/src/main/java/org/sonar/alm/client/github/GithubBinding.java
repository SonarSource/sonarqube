/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.alm.client.github;

import com.google.gson.annotations.SerializedName;
import java.util.List;

import static org.sonar.alm.client.github.GithubApplicationClient.Repository;

public class GithubBinding {

  private GithubBinding() {
    //nothing to do
  }

  public static class GsonInstallations {
    @SerializedName("total_count")
    int totalCount;
    @SerializedName("installations")
    List<GsonInstallation> installations;

    public GsonInstallations() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }
  }

  public static class GsonInstallation {
    @SerializedName("id")
    long id;
    @SerializedName("target_type")
    String targetType;
    @SerializedName("permissions")
    Permissions permissions;

    @SerializedName("account")
    GsonAccount account;

    public GsonInstallation(long id, String targetType, Permissions permissions, GsonAccount account) {
      this.id = id;
      this.targetType = targetType;
      this.permissions = permissions;
      this.account = account;
    }

    public GsonInstallation() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public long getId() {
      return id;
    }

    public String getTargetType() {
      return targetType;
    }

    public Permissions getPermissions() {
      return permissions;
    }

    public GsonAccount getAccount() {
      return account;
    }

    public static class Permissions {
      @SerializedName("checks")
      String checks;

      public Permissions(String checks) {
        this.checks = checks;
      }

      public Permissions() {
        // even if empty constructor is not required for Gson, it is strongly
        // recommended:
        // http://stackoverflow.com/a/18645370/229031
      }

      public String getChecks() {
        return checks;
      }
    }

    public static class GsonAccount {
      @SerializedName("id")
      long id;
      @SerializedName("login")
      String login;

      public GsonAccount() {
        // even if empty constructor is not required for Gson, it is strongly
        // recommended:
        // http://stackoverflow.com/a/18645370/229031
      }
    }
  }

  public static class GsonRepositorySearch {
    @SerializedName("total_count")
    int totalCount;
    @SerializedName("items")
    List<GsonGithubRepository> items;

    public GsonRepositorySearch() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }
  }

  public static class GsonGithubRepository {
    @SerializedName("id")
    long id;
    @SerializedName("name")
    String name;
    @SerializedName("full_name")
    String fullName;
    @SerializedName("private")
    boolean isPrivate;
    @SerializedName("html_url")
    String htmlUrl;
    @SerializedName("default_branch")
    String defaultBranch;

    public GsonGithubRepository() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public Repository toRepository() {
      return new Repository(this.id, this.name, this.isPrivate, this.fullName,
        this.htmlUrl, this.defaultBranch);
    }
  }

  public static class GsonGithubCodeScanningAlert {
    @SerializedName("number")
    long id;
    @SerializedName("state")
    GithubCodeScanningAlertState state;
    @SerializedName("dismissed_reason")
    String dismissedReason;
    @SerializedName("dismissed_comment")
    String dismissedComment;
    @SerializedName("tool")
    GsonGithubCodeScanningAlertTool tool;
    @SerializedName("most_recent_instance")
    GsonGithubCodeScanningAlertInstance mostRecentInstance;

    public GsonGithubCodeScanningAlert() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public long getId() {
      return id;
    }

    public GithubCodeScanningAlertState getState() {
      return state;
    }

    public String getDismissedReason() {
      return dismissedReason;
    }

    public String getDismissedComment() {
      return dismissedComment;
    }

    public GsonGithubCodeScanningAlertTool getTool() {
      return tool;
    }

    public GsonGithubCodeScanningAlertInstance getMostRecentInstance() {
      return mostRecentInstance;
    }

    public String getMessageText() {
      return getMostRecentInstance().getMessageText();
    }
  }

  public static class GsonGithubCodeScanningAlertWebhookPayload {
    @SerializedName("action")
    String action;
    @SerializedName("alert")
    GsonGithubCodeScanningAlert alert;
    @SerializedName("sender")
    GsonGithubCodeScanningAlertWebhookPayloadSender sender;

    public GsonGithubCodeScanningAlertWebhookPayload() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public String getAction() {
      return action;
    }

    public GsonGithubCodeScanningAlert getAlert() {
      return alert;
    }

    public GsonGithubCodeScanningAlertWebhookPayloadSender getSender() {
      return sender;
    }
  }

  public static class GsonGithubCodeScanningAlertInstance {
    @SerializedName("state")
    GithubCodeScanningAlertState state;
    @SerializedName("message")
    GsonGithubCodeScanningAlertMessage message;

    public GsonGithubCodeScanningAlertInstance() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public GsonGithubCodeScanningAlertMessage getMessage() {
      return message;
    }

    public String getMessageText() {
      return getMessage().getText();
    }
  }

  public enum GithubCodeScanningAlertState {
    @SerializedName("open")
    OPEN,
    @SerializedName("fixed")
    FIXED,
    @SerializedName("dismissed")
    DISMISSED
  }

  public static class GsonGithubCodeScanningAlertMessage {
    @SerializedName("text")
    String text;

    public GsonGithubCodeScanningAlertMessage() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public String getText() {
      return text;
    }
  }

  public static class GsonGithubCodeScanningAlertWebhookPayloadSender {
    @SerializedName("login")
    String login;

    public GsonGithubCodeScanningAlertWebhookPayloadSender() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public String getLogin() {
      return login;
    }
  }

  public static class GsonGithubCodeScanningAlertTool {
    @SerializedName("name")
    String name;

    public GsonGithubCodeScanningAlertTool() {
      // even if empty constructor is not required for Gson, it is strongly
      // recommended:
      // http://stackoverflow.com/a/18645370/229031
    }

    public String getName() {
      return name;
    }
  }
}
