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
package org.sonar.server.platform.db.migration.version.v91;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
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
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;
import org.sonar.server.platform.db.migration.version.v91.MigratePortfoliosToNewTables.ViewXml.ViewDef;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.apache.commons.lang.StringUtils.trim;

public class MigratePortfoliosToNewTables extends DataChange {
  private static final Logger LOG = Loggers.get(MigratePortfoliosToNewTables.class);
  private static final String SELECT_DEFAULT_VISIBILITY = "select text_value from properties where prop_key = 'projects.default.visibility'";
  private static final String SELECT_UUID_VISIBILITY_BY_COMPONENT_KEY = "select c.uuid, c.private from components c where c.kee = ?";
  private static final String SELECT_PORTFOLIO_UUID_AND_SELECTION_MODE_BY_KEY = "select uuid,selection_mode from portfolios where kee = ?";
  private static final String SELECT_PROJECT_KEYS_BY_PORTFOLIO_UUID = "select p.kee from portfolio_projects pp "
    + "join projects p on pp.project_uuid = p.uuid where pp.portfolio_uuid = ?";
  private static final String SELECT_PROJECT_UUIDS_BY_KEYS = "select p.uuid,p.kee from projects p where p.kee in (PLACEHOLDER)";
  private static final String VIEWS_DEF_KEY = "views.def";
  private static final String PLACEHOLDER = "PLACEHOLDER";

  protected static final String PORTFOLIO_CONSISTENCY_ERROR = "Some issues were found in portfolio definition. Please verify "
    + VIEWS_DEF_KEY + " consistency in internal_properties datatable";
  protected static final String PORTFOLIO_ROOT_NOT_FOUND = "root with key: %s not found for portfolio with name: %s and key: %s";
  protected static final String PORTFOLIO_PARENT_NOT_FOUND = "parent with key: %s not found for portfolio with name: %s and key: %s";

  private final UuidFactory uuidFactory;
  private final System2 system;

  private boolean defaultPrivateFlag;

  public enum SelectionMode {
    NONE, MANUAL, REGEXP, REST, TAGS
  }

  public MigratePortfoliosToNewTables(Database db, UuidFactory uuidFactory, System2 system) {
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

    this.defaultPrivateFlag = ofNullable(context.prepareSelect(SELECT_DEFAULT_VISIBILITY)
      .get(row -> "private".equals(row.getString(1))))
        .orElse(false);

    try {
      Map<String, ViewXml.ViewDef> portfolioXmlMap = ViewXml.parse(xml);
      List<ViewXml.ViewDef> portfolios = new LinkedList<>(portfolioXmlMap.values());

      Map<String, PortfolioDb> portfolioDbMap = buildPortfolioDbMap(context, portfolios);
      verifyPortfoliosConsistency(portfolios, portfolioDbMap);
      // all portfolio has been created and new uuids assigned
      // update portfolio hierarchy parent/root
      insertReferences(context, portfolioXmlMap, portfolioDbMap);
      updateHierarchy(context, portfolioXmlMap, portfolioDbMap);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to migrate views definitions property.", e);
    }
  }

  private Map<String, PortfolioDb> buildPortfolioDbMap(Context context, List<ViewDef> portfolios) throws SQLException {
    Map<String, PortfolioDb> portfolioDbMap = new HashMap<>();
    for (ViewDef portfolio : portfolios) {
      PortfolioDb createdPortfolio = insertPortfolio(context, portfolio);
      if (createdPortfolio.selectionMode == SelectionMode.MANUAL) {
        insertPortfolioProjects(context, portfolio, createdPortfolio);
      }
      portfolioDbMap.put(createdPortfolio.kee, createdPortfolio);
    }
    return portfolioDbMap;
  }

  private PortfolioDb insertPortfolio(Context context, ViewXml.ViewDef portfolioFromXml) throws SQLException {
    long now = system.now();
    PortfolioDb portfolioDb = context.prepareSelect(SELECT_PORTFOLIO_UUID_AND_SELECTION_MODE_BY_KEY)
      .setString(1, portfolioFromXml.key)
      .get(r -> new PortfolioDb(r.getString(1), portfolioFromXml.key, SelectionMode.valueOf(r.getString(2))));

    Optional<ComponentDb> componentDbOpt = ofNullable(context.prepareSelect(SELECT_UUID_VISIBILITY_BY_COMPONENT_KEY)
      .setString(1, portfolioFromXml.key)
      .get(row -> new ComponentDb(row.getString(1), row.getBoolean(2))));

    // no portfolio -> insert
    if (portfolioDb == null) {
      Upsert insertPortfolioQuery = context.prepareUpsert("insert into " +
        "portfolios(uuid, kee, private, name, description, root_uuid, parent_uuid, selection_mode, selection_expression, updated_at, created_at) " +
        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

      String portfolioUuid = componentDbOpt.map(c -> c.uuid).orElse(uuidFactory.create());
      insertPortfolioQuery.setString(1, portfolioUuid)
        .setString(2, portfolioFromXml.key)
        .setBoolean(3, componentDbOpt.map(c -> c.visibility).orElse(this.defaultPrivateFlag))
        .setString(4, portfolioFromXml.name)
        .setString(5, portfolioFromXml.desc)
        .setString(6, PLACEHOLDER)
        .setString(7, PLACEHOLDER);
      SelectionMode selectionMode = SelectionMode.NONE;
      String selectionExpression = null;
      if (portfolioFromXml.getProjects() != null && !portfolioFromXml.getProjects().isEmpty()) {
        selectionMode = SelectionMode.MANUAL;
      } else if (portfolioFromXml.regexp != null && !portfolioFromXml.regexp.isBlank()) {
        selectionMode = SelectionMode.REGEXP;
        selectionExpression = portfolioFromXml.regexp;
      } else if (portfolioFromXml.def) {
        selectionMode = SelectionMode.REST;
      } else if (portfolioFromXml.tagsAssociation != null && !portfolioFromXml.tagsAssociation.isEmpty()) {
        selectionMode = SelectionMode.TAGS;
        selectionExpression = String.join(",", portfolioFromXml.tagsAssociation);
      }

      insertPortfolioQuery.setString(8, selectionMode.name())
        .setString(9, selectionExpression)
        // set dates
        .setLong(10, now)
        .setLong(11, now)
        .execute()
        .commit();
      return new PortfolioDb(portfolioUuid, portfolioFromXml.key, selectionMode);
    }
    return portfolioDb;
  }

  private void insertPortfolioProjects(Context context, ViewDef portfolio, PortfolioDb createdPortfolio) throws SQLException {
    long now = system.now();
    // select all already added project uuids
    Set<String> alreadyAddedPortfolioProjects = new HashSet<>(
      context.prepareSelect(SELECT_PROJECT_KEYS_BY_PORTFOLIO_UUID)
        .setString(1, createdPortfolio.uuid)
        .list(r -> r.getString(1)));

    Set<String> projectKeysFromXml = new HashSet<>(portfolio.getProjects());
    Set<String> projectKeysToBeAdded = Sets.difference(projectKeysFromXml, alreadyAddedPortfolioProjects);

    if (!projectKeysToBeAdded.isEmpty()) {
      List<ProjectDb> projects = findPortfolioProjects(context, projectKeysToBeAdded);

      var upsert = context.prepareUpsert("insert into " +
        "portfolio_projects(uuid, portfolio_uuid, project_uuid, created_at) " +
        "values (?, ?, ?, ?)");
      for (ProjectDb projectDb : projects) {
        upsert.setString(1, uuidFactory.create())
          .setString(2, createdPortfolio.uuid)
          .setString(3, projectDb.uuid)
          .setLong(4, now)
          .addBatch();
      }
      if (!projects.isEmpty()) {
        upsert.execute()
          .commit();
      }
    }
  }

  private static List<ProjectDb> findPortfolioProjects(Context context, Set<String> projectKeysToBeAdded) {
    return DatabaseUtils.executeLargeInputs(projectKeysToBeAdded, keys -> {
      try {
        String selectQuery = SELECT_PROJECT_UUIDS_BY_KEYS.replace(PLACEHOLDER,
          keys.stream().map(key -> "'" + key + "'").collect(joining(",")));
        return context.prepareSelect(selectQuery)
          .list(r -> new ProjectDb(r.getString(1), r.getString(2)));
      } catch (SQLException e) {
        throw new IllegalStateException("Could not execute 'in' query", e);
      }
    });
  }

  private static void verifyPortfoliosConsistency(Collection<ViewDef> portfolios, Map<String, PortfolioDb> portfolioDbMap) {
    List<String> errors = findConsistencyErrors(portfolios, portfolioDbMap);
    if (!errors.isEmpty()) {
      LOG.error(PORTFOLIO_CONSISTENCY_ERROR);
      errors.forEach(LOG::error);
      throw new IllegalStateException();
    }
  }

  private static List<String> findConsistencyErrors(Collection<ViewDef> portfolios, Map<String, PortfolioDb> portfolioDbMap) {
    return portfolios.stream()
      .flatMap(portfolio -> findConsistencyErrors(portfolio, portfolioDbMap).stream())
      .toList();
  }

  private static List<String> findConsistencyErrors(ViewDef portfolio, Map<String, PortfolioDb> portfolioDbMap) {
    List<String> errors = new ArrayList<>();

    String root = portfolio.root;
    if (root != null && !portfolioDbMap.containsKey(root)) {
      errors.add(String.format(PORTFOLIO_ROOT_NOT_FOUND, root, portfolio.name, portfolio.key));
    }

    String parent = portfolio.parent;
    if (parent != null && !portfolioDbMap.containsKey(parent)) {
      errors.add(String.format(PORTFOLIO_PARENT_NOT_FOUND, parent, portfolio.name, portfolio.key));
    }

    return errors;
  }

  private void insertReferences(Context context, Map<String, ViewDef> portfolioXmlMap,
    Map<String, PortfolioDb> portfolioDbMap) throws SQLException {
    Upsert insertQuery = context.prepareUpsert("insert into portfolio_references(uuid, portfolio_uuid, reference_uuid, created_at) values (?, ?, ?, ?)");

    long now = system.now();
    boolean shouldExecuteQuery = false;
    for (ViewDef portfolio : portfolioXmlMap.values()) {
      var currentPortfolioUuid = portfolioDbMap.get(portfolio.key).uuid;
      Set<String> referencesFromXml = new HashSet<>(portfolio.getReferences());
      Set<String> referencesFromDb = new HashSet<>(context.prepareSelect("select pr.reference_uuid from portfolio_references pr where pr.portfolio_uuid = ?")
        .setString(1, currentPortfolioUuid)
        .list(row -> row.getString(1)));

      for (String appOrPortfolio : referencesFromXml) {
        // if portfolio and hasn't been added already
        if (portfolioDbMap.containsKey(appOrPortfolio) && !referencesFromDb.contains(portfolioDbMap.get(appOrPortfolio).uuid)) {
          insertQuery
            .setString(1, uuidFactory.create())
            .setString(2, currentPortfolioUuid)
            .setString(3, portfolioDbMap.get(appOrPortfolio).uuid)
            .setLong(4, now)
            .addBatch();
          shouldExecuteQuery = true;
        } else {
          // if application exist and haven't been added
          String appUuid = context.prepareSelect("select p.uuid from projects p where p.kee = ?")
            .setString(1, appOrPortfolio)
            .get(row -> row.getString(1));
          if (appUuid != null && !referencesFromDb.contains(appUuid)) {
            insertQuery
              .setString(1, uuidFactory.create())
              .setString(2, currentPortfolioUuid)
              .setString(3, appUuid)
              .setLong(4, now)
              .addBatch();
            shouldExecuteQuery = true;
          }
        }
      }
    }
    if (shouldExecuteQuery) {
      insertQuery
        .execute()
        .commit();
    }

  }

  private static void updateHierarchy(Context context, Map<String, ViewXml.ViewDef> defs, Map<String, PortfolioDb> portfolioDbMap) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select uuid, kee from portfolios where root_uuid = ? or parent_uuid = ?")
      .setString(1, PLACEHOLDER)
      .setString(2, PLACEHOLDER);
    massUpdate.update("update portfolios set root_uuid = ?, parent_uuid = ? where uuid = ?");
    massUpdate.execute((row, update) -> {
      String currentPortfolioUuid = row.getString(1);
      String currentPortfolioKey = row.getString(2);

      var currentPortfolio = defs.get(currentPortfolioKey);
      String parentUuid = ofNullable(currentPortfolio.parent).map(parent -> portfolioDbMap.get(parent).uuid).orElse(null);
      String rootUuid = ofNullable(currentPortfolio.root).map(root -> portfolioDbMap.get(root).uuid).orElse(currentPortfolioUuid);
      update.setString(1, rootUuid)
        .setString(2, parentUuid)
        .setString(3, currentPortfolioUuid);
      return true;
    });
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

  private static class ComponentDb {
    String uuid;
    boolean visibility;

    public ComponentDb(String uuid, boolean visibility) {
      this.uuid = uuid;
      this.visibility = visibility;
    }
  }

  private static class PortfolioDb {
    String uuid;
    String kee;
    SelectionMode selectionMode;

    PortfolioDb(String uuid, String kee,
      SelectionMode selectionMode) {
      this.uuid = uuid;
      this.kee = kee;
      this.selectionMode = selectionMode;
    }
  }

  private static class ProjectDb {
    String uuid;
    String kee;

    ProjectDb(String uuid, String kee) {
      this.uuid = uuid;
      this.kee = kee;
    }
  }

  static class ViewXml {
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
        result.put(def.key, def);
      }

      return result;
    }

    private static void validate(String xml) throws IOException, SAXException, ParserConfigurationException {
      // Replace bare, namespace unaware header with fully qualified header (with schema declaration)
      String fullyQualifiedXml = VIEWS_HEADER_BARE_PATTERN.matcher(xml).replaceFirst(VIEWS_HEADER_FQ);
      try (InputStream xsd = MigratePortfoliosToNewTables.class.getResourceAsStream(SCHEMA_VIEWS)) {
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
      } else if (StringUtils.equals(nodeName, "tagsAssociation")) {
        parseTagsAssociation(viewDef, viewCursor);
      }
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

    static class ViewDef {
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

      Set<String> tagsAssociation = new TreeSet<>();

      public List<String> getProjects() {
        return p;
      }

      public List<String> getReferences() {
        return vwRef;
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

      public ViewDef addProject(String project) {
        this.p.add(project);
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

      public ViewDef setQualifier(@Nullable String qualifier) {
        this.qualifier = qualifier;
        return this;
      }

      public ViewDef addTagAssociation(String tag) {
        this.tagsAssociation.add(tag);
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
  }
}
