/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.commons.lang.StringEscapeUtils.escapeXml;
import static org.apache.commons.lang.StringUtils.trim;

public class MigrateApplicationDefinitionsFromXmlToDb extends DataChange {
  static final int TEXT_VALUE_MAX_LENGTH = 4000;
  private static final String SELECT_APPLICATION_UUID_BY_KEY = "select uuid from projects where kee = ? and qualifier = 'APP'";
  private static final String SELECT_PROJECTS_BY_KEYS = "select kee,uuid from projects where kee in (%s) and qualifier = 'TRK'";
  private static final String SELECT_PROJECTS_BY_APP = "select project_uuid from app_projects where application_uuid = ?";
  private static final String UPDATE_INTERNAL_PROP_TEXT_VALUE = "update internal_properties set text_value = ?, clob_value = NULL where kee = ?";
  private static final String UPDATE_INTERNAL_PROP_CLOB_VALUE = "update internal_properties set clob_value = ?, text_value = NULL where kee = ?";
  private static final String VIEWS_DEF_KEY = "views.def";

  private final UuidFactory uuidFactory;
  private final System2 system;

  public MigrateApplicationDefinitionsFromXmlToDb(Database db, UuidFactory uuidFactory, System2 system) {
    super(db);

    this.uuidFactory = uuidFactory;
    this.system = system;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    String xml = getViewsDefinition(context);
    // skip migration if `views.def` does not exist in the db
    if (xml == null) {
      return;
    }

    try {
      Map<String, ViewXml.ViewDef> defs = ViewXml.parse(xml);
      List<ViewXml.ViewDef> applications = defs.values()
        .stream()
        .filter(v -> Qualifiers.APP.equals(v.getQualifier()))
        .collect(Collectors.toList());
      for (ViewXml.ViewDef app : applications) {
        insertApplication(context, app);
      }
      cleanUpViewsDefinitionsXml(context, defs.values());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to migrate views definitions property.", e);
    }

  }

  private void insertApplication(Context context, ViewXml.ViewDef app) throws SQLException {
    long now = system.now();
    String applicationUuid = context.prepareSelect(SELECT_APPLICATION_UUID_BY_KEY)
      .setString(1, app.getKey())
      .get(r -> r.getString(1));

    // skip migration if:
    // - application only exists in xml and not in the db. It will be removed from the xml at later stage of the migration.
    // - application contains no projects- it's already in a valid db state
    Set<String> uniqueProjects = new HashSet<>(app.getProjects());
    if (applicationUuid == null || uniqueProjects.isEmpty()) {
      return;
    }

    List<String> alreadyAddedProjects = context.prepareSelect(SELECT_PROJECTS_BY_APP).setString(1, applicationUuid)
      .list(r -> r.getString(1));

    String queryParam = uniqueProjects.stream().map(uuid -> "'" + uuid + "'").collect(Collectors.joining(","));
    Map<String, String> projectUuidsByKeys = context.prepareSelect(format(SELECT_PROJECTS_BY_KEYS, queryParam))
      .list(r -> new AbstractMap.SimpleEntry<>(r.getString(1), r.getString(2)))
      .stream()
      .filter(project -> !alreadyAddedProjects.contains(project.getValue()))
      .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    if (!projectUuidsByKeys.isEmpty()) {
      insertApplicationProjects(context, app, applicationUuid, projectUuidsByKeys, uniqueProjects, now);
    }
    if (!app.getApplicationBranches().isEmpty()) {
      insertApplicationBranchesProjects(context, app, applicationUuid, projectUuidsByKeys, now);
    }
  }

  private void insertApplicationProjects(Context context, ViewXml.ViewDef app, String applicationUuid,
    Map<String, String> projectUuidsByKeys, Set<String> uniqueProjects, long createdTime) throws SQLException {
    Upsert insertApplicationProjectsQuery = context.prepareUpsert("insert into " +
      "app_projects(uuid, application_uuid, project_uuid, created_at) " +
      "values (?, ?, ?, ?)");
    for (String projectKey : uniqueProjects) {
      String applicationProjectUuid = uuidFactory.create();
      String projectUuid = projectUuidsByKeys.get(projectKey);

      // ignore project if it does not exist anymore
      if (projectUuid == null) {
        continue;
      }

      insertApplicationProjectsQuery
        .setString(1, applicationProjectUuid)
        .setString(2, applicationUuid)
        .setString(3, projectUuid)
        .setLong(4, createdTime)
        .addBatch();
    }

    insertApplicationProjectsQuery.execute().commit();
  }

  private void insertApplicationBranchesProjects(Context context, ViewXml.ViewDef app, String applicationUuid,
    Map<String, String> projectUuidsByKeys, long createdTime) throws SQLException {
    Map<String, String> appBranchUuidByKey = context.prepareSelect("select kee,uuid from project_branches where project_uuid = ?")
      .setString(1, applicationUuid)
      .list(r -> new AbstractMap.SimpleEntry<>(r.getString(1), r.getString(2)))
      .stream()
      .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

    Upsert insertApplicationsBranchProjectsQuery = context.prepareUpsert("insert into " +
      "app_branch_project_branch(uuid, application_uuid, application_branch_uuid, project_uuid, project_branch_uuid, created_at) " +
      "values (?, ?, ?, ?, ?, ?)");
    boolean insert = false;
    for (ViewXml.ApplicationBranchDef branch : app.getApplicationBranches()) {
      String applicationBranchUuid = appBranchUuidByKey.get(branch.getKey());
      // ignore application branch if it does not exist in the DB anymore
      if (applicationBranchUuid == null) {
        continue;
      }

      if (insertApplicationBranchProjects(context, branch, applicationUuid, applicationBranchUuid, projectUuidsByKeys, createdTime,
        insertApplicationsBranchProjectsQuery)) {
        insert = true;
      }
    }

    if (insert) {
      insertApplicationsBranchProjectsQuery.execute().commit();
    }
  }

  private boolean insertApplicationBranchProjects(Context context, ViewXml.ApplicationBranchDef branch, String applicationUuid,
    String applicationBranchUuid, Map<String, String> projectUuidsByKeys, long createdTime,
    Upsert insertApplicationsBranchProjectsQuery) throws SQLException {

    boolean insert = false;
    for (ViewXml.ApplicationProjectDef appProjDef : branch.getProjects()) {
      String projectUuid = projectUuidsByKeys.get(appProjDef.getKey());

      // ignore projects that do not exist in the DB anymore
      if (projectUuid != null) {
        String projectBranchUuid = context.prepareSelect("select uuid from project_branches where project_uuid = ? and kee = ?")
          .setString(1, projectUuid)
          .setString(2, appProjDef.getBranch())
          .get(r -> r.getString(1));

        // ignore project branches that do not exist in the DB anymore
        if (projectBranchUuid != null) {
          String applicationBranchProjectUuid = uuidFactory.create();
          insertApplicationsBranchProjectsQuery
            .setString(1, applicationBranchProjectUuid)
            .setString(2, applicationUuid)
            .setString(3, applicationBranchUuid)
            .setString(4, projectUuid)
            .setString(5, projectBranchUuid)
            .setLong(6, createdTime)
            .addBatch();
          insert = true;
        }
      }
    }

    return insert;
  }

  @CheckForNull
  private static String getViewsDefinition(DataChange.Context context) throws SQLException {
    Select select = context.prepareSelect("select text_value,clob_value from internal_properties where kee=?");
    select.setString(1, VIEWS_DEF_KEY);
    return select.get(row -> {
      String v = row.getString(1);
      if (v != null) {
        return v;
      } else {
        return row.getString(2);
      }
    });
  }

  private static void cleanUpViewsDefinitionsXml(Context context, Collection<ViewXml.ViewDef> definitions) throws SQLException, IOException {
    definitions = definitions.stream()
      .filter(d -> !"APP".equals(d.getQualifier()))
      .collect(Collectors.toList());

    StringWriter output = new StringWriter();
    new ViewXml.ViewDefinitionsSerializer().write(definitions, output);
    String value = output.toString();
    String statement = UPDATE_INTERNAL_PROP_TEXT_VALUE;
    if (mustBeStoredInClob(value)) {
      statement = UPDATE_INTERNAL_PROP_CLOB_VALUE;
    }

    context.prepareUpsert(statement)
      .setString(1, output.toString())
      .setString(2, VIEWS_DEF_KEY)
      .execute()
      .commit();
  }

  private static boolean mustBeStoredInClob(String value) {
    return value.length() > TEXT_VALUE_MAX_LENGTH;
  }

  private static class ViewXml {
    static final String SCHEMA_VIEWS = "/static/views.xsd";
    static final String VIEWS_HEADER_BARE = "<views>";
    static final Pattern VIEWS_HEADER_BARE_PATTERN = Pattern.compile(VIEWS_HEADER_BARE);
    static final String VIEWS_HEADER_FQ = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      + "<views xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://sonarsource.com/schema/views\">";

    private ViewXml() {
      // nothing to do here
    }

    private static Map<String, ViewDef> parse(String xml) throws ParserConfigurationException, SAXException, IOException, XMLStreamException {
      if (StringUtils.isEmpty(xml)) {
        return new LinkedHashMap<>(0);
      }

      List<ViewDef> views;
      validate(xml);
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(new StringReader(xml));
      rootC.advance(); // <views>
      SMInputCursor cursor = rootC.childElementCursor();
      views = parseViewDefinitions(cursor);

      Map<String, ViewDef> result = new LinkedHashMap<>(views.size());
      for (ViewDef def : views) {
        result.put(def.getKey(), def);
      }

      return result;
    }

    private static void validate(String xml) throws IOException, SAXException, ParserConfigurationException {
      // Replace bare, namespace unaware header with fully qualified header (with schema declaration)
      String fullyQualifiedXml = VIEWS_HEADER_BARE_PATTERN.matcher(xml).replaceFirst(VIEWS_HEADER_FQ);
      try (InputStream xsd = MigrateApplicationDefinitionsFromXmlToDb.class.getResourceAsStream(SCHEMA_VIEWS)) {
        InputSource viewsDefinition = new InputSource(new InputStreamReader(toInputStream(fullyQualifiedXml, UTF_8), UTF_8));

        SchemaFactory saxSchemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        saxSchemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        saxSchemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
        parserFactory.setNamespaceAware(true);
        parserFactory.setSchema(saxSchemaFactory.newSchema(new SAXSource(new InputSource(xsd))));

        SAXParser saxParser = parserFactory.newSAXParser();
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        saxParser.parse(viewsDefinition, new ViewsValidator());
      }
    }

    private static List<ViewDef> parseViewDefinitions(SMInputCursor viewsCursor) throws XMLStreamException {
      List<ViewDef> views = new ArrayList<>();
      while (viewsCursor.getNext() != null) {
        ViewDef viewDef = new ViewDef();
        viewDef.setKey(viewsCursor.getAttrValue("key"));
        viewDef.setDef(Boolean.parseBoolean(viewsCursor.getAttrValue("def")));
        viewDef.setParent(viewsCursor.getAttrValue("parent"));
        viewDef.setRoot(viewsCursor.getAttrValue("root"));
        SMInputCursor viewCursor = viewsCursor.childElementCursor();
        while (viewCursor.getNext() != null) {
          String nodeName = viewCursor.getLocalName();
          parseChildElement(viewDef, viewCursor, nodeName);
        }
        views.add(viewDef);
      }
      return views;
    }

    private static void parseChildElement(ViewDef viewDef, SMInputCursor viewCursor, String nodeName) throws XMLStreamException {
      if (StringUtils.equals(nodeName, "name")) {
        viewDef.setName(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "desc")) {
        viewDef.setDesc(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "regexp")) {
        viewDef.setRegexp(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "language")) {
        viewDef.setLanguage(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "tag_key")) {
        viewDef.setTagKey(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "tag_value")) {
        viewDef.setTagValue(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "p")) {
        viewDef.addProject(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "vw-ref")) {
        viewDef.addReference(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "qualifier")) {
        viewDef.setQualifier(trim(viewCursor.collectDescendantText()));
      } else if (StringUtils.equals(nodeName, "branch")) {
        parseBranch(viewDef, viewCursor);
      } else if (StringUtils.equals(nodeName, "tagsAssociation")) {
        parseTagsAssociation(viewDef, viewCursor);
      }
    }

    private static void parseBranch(ViewDef def, SMInputCursor viewCursor) throws XMLStreamException {
      List<ApplicationProjectDef> projects = new ArrayList<>();
      String key = viewCursor.getAttrValue("key");
      SMInputCursor projectCursor = viewCursor.childElementCursor();
      while (projectCursor.getNext() != null) {
        if (Objects.equals(projectCursor.getLocalName(), "p")) {
          String branch = projectCursor.getAttrValue("branch");
          String projectKey = trim(projectCursor.collectDescendantText());
          projects.add(new ApplicationProjectDef().setKey(projectKey).setBranch(branch));
        }
      }
      def.getApplicationBranches().add(new ApplicationBranchDef()
        .setKey(key)
        .setProjects(projects));
    }

    private static void parseTagsAssociation(ViewDef def, SMInputCursor viewCursor) throws XMLStreamException {
      SMInputCursor projectCursor = viewCursor.childElementCursor();
      while (projectCursor.getNext() != null) {
        def.addTagAssociation(trim(projectCursor.collectDescendantText()));
      }
    }

    private static SMInputFactory initStax() {
      XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
      xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
      xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
      // just so it won't try to load DTD in if there's DOCTYPE
      xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
      xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
      return new SMInputFactory(xmlFactory);
    }

    private static class ViewDef {
      String key = null;

      String parent = null;

      String root = null;

      boolean def = false;

      List<String> p = new ArrayList<>();

      List<String> vwRef = new ArrayList<>();

      String name = null;

      String desc = null;

      String regexp = null;

      String language = null;

      String tagKey = null;

      String tagValue = null;

      String qualifier = null;

      List<ApplicationBranchDef> applicationBranches = new ArrayList<>();

      Set<String> tagsAssociation = new TreeSet<>();

      public String getKey() {
        return key;
      }

      public String getParent() {
        return parent;
      }

      @CheckForNull
      public String getRoot() {
        return root;
      }

      public boolean isDef() {
        return def;
      }

      public List<String> getProjects() {
        return p;
      }

      public List<String> getReferences() {
        return vwRef;
      }

      public String getName() {
        return name;
      }

      public String getDesc() {
        return desc;
      }

      @CheckForNull
      public String getRegexp() {
        return regexp;
      }

      @CheckForNull
      public String getLanguage() {
        return language;
      }

      @CheckForNull
      public String getTagKey() {
        return tagKey;
      }

      @CheckForNull
      public String getTagValue() {
        return tagValue;
      }

      @CheckForNull
      public String getQualifier() {
        return qualifier;
      }

      public List<ApplicationBranchDef> getApplicationBranches() {
        return applicationBranches;
      }

      public Set<String> getTagsAssociation() {
        return tagsAssociation;
      }

      public ViewDef setKey(String key) {
        this.key = key;
        return this;
      }

      public ViewDef setParent(String parent) {
        this.parent = parent;
        return this;
      }

      public ViewDef setRoot(@Nullable String root) {
        this.root = root;
        return this;
      }

      public ViewDef setDef(boolean def) {
        this.def = def;
        return this;
      }

      public ViewDef setProjects(List<String> projects) {
        this.p = projects;
        return this;
      }

      public ViewDef addProject(String project) {
        this.p.add(project);
        return this;
      }

      public ViewDef removeProject(String project) {
        this.p.remove(project);
        return this;
      }

      public ViewDef setName(String name) {
        this.name = name;
        return this;
      }

      public ViewDef setDesc(@Nullable String desc) {
        this.desc = desc;
        return this;
      }

      public ViewDef setRegexp(@Nullable String regexp) {
        this.regexp = regexp;
        return this;
      }

      public ViewDef setLanguage(@Nullable String language) {
        this.language = language;
        return this;
      }

      public ViewDef setTagKey(@Nullable String tagKey) {
        this.tagKey = tagKey;
        return this;
      }

      public ViewDef setTagValue(@Nullable String tagValue) {
        this.tagValue = tagValue;
        return this;
      }

      public ViewDef addReference(String reference) {
        this.vwRef.add(reference);
        return this;
      }

      public ViewDef removeReference(String reference) {
        this.vwRef.remove(reference);
        return this;
      }

      public ViewDef setReferences(List<String> vwRef) {
        this.vwRef = vwRef;
        return this;
      }

      public ViewDef setQualifier(@Nullable String qualifier) {
        this.qualifier = qualifier;
        return this;
      }

      public ViewDef setApplicationBranches(List<ApplicationBranchDef> branches) {
        this.applicationBranches = branches;
        return this;
      }

      public ViewDef addTagAssociation(String tag) {
        this.tagsAssociation.add(tag);
        return this;
      }

      public ViewDef setTagsAssociation(Set<String> tagsAssociation) {
        this.tagsAssociation = tagsAssociation;
        return this;
      }
    }

    private static class ApplicationProjectDef {
      private String key = null;
      private String branch = null;

      public String getKey() {
        return key;
      }

      public ApplicationProjectDef setKey(String key) {
        this.key = key;
        return this;
      }

      @CheckForNull
      public String getBranch() {
        return branch;
      }

      public ApplicationProjectDef setBranch(@Nullable String branch) {
        this.branch = branch;
        return this;
      }
    }

    private static class ApplicationBranchDef {

      private String key = null;
      private List<ApplicationProjectDef> p = new ArrayList<>();

      public String getKey() {
        return key;
      }

      public ApplicationBranchDef setKey(String key) {
        this.key = key;
        return this;
      }

      public List<ApplicationProjectDef> getProjects() {
        return p;
      }

      public ApplicationBranchDef setProjects(List<ApplicationProjectDef> p) {
        this.p = p;
        return this;
      }
    }

    private static final class ViewsValidator extends DefaultHandler {
      @Override
      public void error(SAXParseException exception) throws SAXException {
        throw exception;
      }

      @Override
      public void warning(SAXParseException exception) throws SAXException {
        throw exception;
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
      }
    }

    static class ViewDefinitionsSerializer {

      public void write(Collection<ViewDef> definitions, Writer writer) throws IOException {
        writer.append(VIEWS_HEADER_BARE);

        for (ViewDef def : definitions) {
          writer.append("<vw");
          writer.append(" key=\"").append(escapeXml(def.getKey())).append("\"");
          writer.append(" def=\"").append(Boolean.toString(def.isDef())).append("\"");
          String parent = def.getParent();
          if (parent != null) {
            writer.append(" root=\"").append(escapeXml(def.getRoot())).append("\"");
            writer.append(" parent=\"").append(escapeXml(parent)).append("\"");
          }
          writer.append(">");

          writer.append("<name><![CDATA[").append(def.getName()).append("]]></name>");
          writeOptionalElements(writer, def);

          for (String project : def.getProjects()) {
            writer.append("<p>").append(project).append("</p>");
          }
          for (String ref : def.getReferences()) {
            writer.append("<vw-ref><![CDATA[").append(ref).append("]]></vw-ref>");
          }
          writeTagsAssociation(writer, def);
          writer.append("</vw>");
        }

        writer.append("</views>");
      }

      private static void writeOptionalElements(Writer writer, ViewDef def) throws IOException {
        String description = def.getDesc();
        if (description != null) {
          writer.append("<desc><![CDATA[").append(description).append("]]></desc>");
        }
        String regexp = def.getRegexp();
        if (regexp != null) {
          writer.append("<regexp><![CDATA[").append(regexp).append("]]></regexp>");
        }
        String language = def.getLanguage();
        if (language != null) {
          writer.append("<language><![CDATA[").append(language).append("]]></language>");
        }
        String customMeasureKey = def.getTagKey();
        if (customMeasureKey != null) {
          writer.append("<tag_key><![CDATA[").append(customMeasureKey).append("]]></tag_key>");
        }
        String customMeasureValue = def.getTagValue();
        if (customMeasureValue != null) {
          writer.append("<tag_value><![CDATA[").append(customMeasureValue).append("]]></tag_value>");
        }
        String qualifier = def.getQualifier();
        if (qualifier != null) {
          writer.append("<qualifier><![CDATA[").append(qualifier).append("]]></qualifier>");
        }
      }

      private static void writeTagsAssociation(Writer writer, ViewDef definition) throws IOException {
        Set<String> tagsAssociation = definition.getTagsAssociation();
        if (tagsAssociation.isEmpty()) {
          return;
        }
        writer.append("<tagsAssociation>");
        for (String tag : tagsAssociation) {
          writer.append("<tag>").append(tag).append("</tag>");
        }
        writer.append("</tagsAssociation>");
      }

    }
  }
}
