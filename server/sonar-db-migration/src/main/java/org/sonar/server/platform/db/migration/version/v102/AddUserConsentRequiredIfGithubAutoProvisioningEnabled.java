package org.sonar.server.platform.db.migration.version.v102;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class AddUserConsentRequiredIfGithubAutoProvisioningEnabled extends DataChange {

  private static final Logger LOG = LoggerFactory.getLogger(AddUserConsentRequiredIfGithubAutoProvisioningEnabled.class);
  @VisibleForTesting
  static final String PROVISIONING_GITHUB_ENABLED_PROP_KEY = "provisioning.github.enabled";

  @VisibleForTesting
  static final String PROP_KEY = "sonar.auth.github.userConsentForPermissionProvisioningRequired";

  private static final String INSERT_QUERY = """
    INSERT INTO PROPERTIES (UUID, PROP_KEY, IS_EMPTY, CREATED_AT)
    VALUES (?, ?, ?, ?)
    """;

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AddUserConsentRequiredIfGithubAutoProvisioningEnabled(Database db, System2 system2, UuidFactory uuidFactory) {
    super(db);
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  @Override
  protected void execute(DataChange.Context context) throws SQLException {
    if (!isGithubAutoProvisioningEnabled(context)) {
      return;
    }
    if (isUserConsentAlreadyRequired(context)) {
      return;
    }
    LOG.warn("Automatic synchronization was previously activated for GitHub. It requires user consent to continue working as new " +
             " features were added with the synchronization. Please read the upgrade notes.");
    Upsert upsert = context.prepareUpsert(INSERT_QUERY);
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, PROP_KEY)
      .setBoolean(3, true)
      .setLong(4, system2.now())
      .execute()
      .commit();
  }

  private static boolean isUserConsentAlreadyRequired(Context context) throws SQLException {
    return Optional.ofNullable(context.prepareSelect("select count(*) from properties where prop_key = ?")
      .setString(1, PROP_KEY)
      .get(t -> 1 == t.getInt(1)))
      .orElseThrow();
  }

  private static boolean isGithubAutoProvisioningEnabled(Context context) throws SQLException {
    return Optional.ofNullable(context.prepareSelect("select count(*) from internal_properties where kee = ? and text_value = ?")
      .setString(1, PROVISIONING_GITHUB_ENABLED_PROP_KEY)
      .setString(2, "true")
      .get(t -> 1 == t.getInt(1)))
      .orElseThrow();
  }

}
