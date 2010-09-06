package org.sonar.server.rules;

import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileExporter;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.StringWriter;
import java.io.Writer;

public final class ProfileBackuper implements ServerComponent {

  private DatabaseSessionFactory sessionFactory;
  private XMLProfileExporter exporter;

  public ProfileBackuper(DatabaseSessionFactory sessionFactory, XMLProfileExporter exporter) {
    this.sessionFactory = sessionFactory;
    this.exporter = exporter;
  }

  public String exportProfile(int profileId) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = session.getSingleResult(RulesProfile.class, "id", profileId);
    if (profile != null) {
      Writer writer = new StringWriter();
      exporter.exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }
}
