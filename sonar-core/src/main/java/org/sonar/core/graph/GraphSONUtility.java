/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.graph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.ElementFactory;
import com.tinkerpop.blueprints.util.io.graphson.ElementPropertyConfig;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONTokens;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tinkerpop.blueprints.util.io.graphson.ElementPropertyConfig.ElementPropertiesRule;

/**
 * Helps write individual graph elements to TinkerPop JSON format known as GraphSON.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class GraphSONUtility {

  private final GraphSONMode mode;
  private final Set<String> vertexPropertyKeys;
  private final Set<String> edgePropertyKeys;
  private final ElementFactory factory;
  private final boolean hasEmbeddedTypes;
  private final ElementPropertiesRule vertexPropertiesRule;
  private final ElementPropertiesRule edgePropertiesRule;
  private final boolean includeReservedVertexId;
  private final boolean includeReservedEdgeId;
  private final boolean includeReservedVertexType;
  private final boolean includeReservedEdgeType;
  private final boolean includeReservedEdgeLabel;
  private final boolean includeReservedEdgeOutV;
  private final boolean includeReservedEdgeInV;
  private JSONParser parser = new JSONParser();

  /**
   * A GraphSONUtiltiy that includes all properties of vertices and edges.
   */
  public GraphSONUtility(final GraphSONMode mode, final ElementFactory factory) {
    this(mode, factory, ElementPropertyConfig.AllProperties);
  }

  /**
   * A GraphSONUtility that includes the specified properties.
   */
  public GraphSONUtility(final GraphSONMode mode, final ElementFactory factory,
                         final Set<String> vertexPropertyKeys, final Set<String> edgePropertyKeys) {
    this(mode, factory, ElementPropertyConfig.IncludeProperties(vertexPropertyKeys, edgePropertyKeys));
  }

  public GraphSONUtility(final GraphSONMode mode, final ElementFactory factory,
                         final ElementPropertyConfig config) {
    this.vertexPropertyKeys = config.getVertexPropertyKeys();
    this.edgePropertyKeys = config.getEdgePropertyKeys();
    this.vertexPropertiesRule = config.getVertexPropertiesRule();
    this.edgePropertiesRule = config.getEdgePropertiesRule();

    this.mode = mode;
    this.factory = factory;
    this.hasEmbeddedTypes = mode == GraphSONMode.EXTENDED;

    this.includeReservedVertexId = includeReservedKey(mode, GraphSONTokens._ID, vertexPropertyKeys, this.vertexPropertiesRule);
    this.includeReservedEdgeId = includeReservedKey(mode, GraphSONTokens._ID, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedVertexType = includeReservedKey(mode, GraphSONTokens._TYPE, vertexPropertyKeys, this.vertexPropertiesRule);
    this.includeReservedEdgeType = includeReservedKey(mode, GraphSONTokens._TYPE, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedEdgeLabel = includeReservedKey(mode, GraphSONTokens._LABEL, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedEdgeOutV = includeReservedKey(mode, GraphSONTokens._OUT_V, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedEdgeInV = includeReservedKey(mode, GraphSONTokens._IN_V, edgePropertyKeys, this.edgePropertiesRule);
  }

  /**
   * Creates a Jettison JSONObject from a graph element.
   *
   * @param element      the graph element to convert to JSON.
   * @param propertyKeys The property keys at the root of the element to serialize.  If null, then all keys are serialized.
   * @param mode         the type of GraphSON to be generated.
   */
  public static JSONObject jsonFromElement(final Element element, final Set<String> propertyKeys,
                                           final GraphSONMode mode) {
    final GraphSONUtility graphson = element instanceof Edge ? new GraphSONUtility(mode, null, null, propertyKeys)
        : new GraphSONUtility(mode, null, propertyKeys, null);
    return graphson.jsonFromElement(element);
  }

  /**
   * Creates a Jackson ObjectNode from a graph element.
   *
   * @param element      the graph element to convert to JSON.
   * @param propertyKeys The property keys at the root of the element to serialize.  If null, then all keys are serialized.
   * @param mode         The type of GraphSON to generate.
   */
  public static JSONObject objectNodeFromElement(final Element element, final Set<String> propertyKeys, final GraphSONMode mode) {
    final GraphSONUtility graphson = element instanceof Edge ? new GraphSONUtility(mode, null, null, propertyKeys)
        : new GraphSONUtility(mode, null, propertyKeys, null);
    return graphson.objectNodeFromElement(element);
  }

  /**
   * Reads an individual Vertex from JSON.  The vertex must match the accepted GraphSON format.
   *
   * @param json         a single vertex in GraphSON format as Jettison JSONObject
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include on reading of element properties
   */
  public static Vertex vertexFromJson(final JSONObject json, final ElementFactory factory, final GraphSONMode mode,
                                      final Set<String> propertyKeys) throws IOException {
    final GraphSONUtility graphson = new GraphSONUtility(mode, factory, propertyKeys, null);
    return graphson.vertexFromJson(json);
  }

  /**
   * Reads an individual Vertex from JSON.  The vertex must match the accepted GraphSON format.
   *
   * @param json         a single vertex in GraphSON format as a String.
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include on reading of element properties
   */
  public static Vertex vertexFromJson(final String json, final ElementFactory factory, final GraphSONMode mode,
                                      final Set<String> propertyKeys) throws ParseException {
    final GraphSONUtility graphson = new GraphSONUtility(mode, factory, propertyKeys, null);
    return graphson.vertexFromJson(json);
  }

  /**
   * Reads an individual Vertex from JSON.  The vertex must match the accepted GraphSON format.
   *
   * @param json         a single vertex in GraphSON format as an InputStream.
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include on reading of element properties
   */
  public static Vertex vertexFromJson(final InputStream json, final ElementFactory factory, final GraphSONMode mode,
                                      final Set<String> propertyKeys) throws IOException, ParseException {
    final GraphSONUtility graphson = new GraphSONUtility(mode, factory, propertyKeys, null);
    return graphson.vertexFromJson(json);
  }

  private static boolean includeReservedKey(final GraphSONMode mode, final String key,
                                            final Set<String> propertyKeys,
                                            final ElementPropertiesRule rule) {
    // the key is always included in modes other than compact.  if it is compact, then validate that the
    // key is in the property key list
    return mode != GraphSONMode.COMPACT || includeKey(key, propertyKeys, rule);
  }

  private static boolean includeKey(final String key, final Set<String> propertyKeys,
                                    final ElementPropertiesRule rule) {
    if (propertyKeys == null) {
      // when null always include the key and shortcut this piece
      return true;
    }

    // default the key situation.  if it's included then it should be explicitly defined in the
    // property keys list to be included or the reverse otherwise
    boolean keySituation = rule == ElementPropertiesRule.INCLUDE;

    switch (rule) {
      case INCLUDE:
        keySituation = propertyKeys.contains(key);
        break;
      case EXCLUDE:
        keySituation = !propertyKeys.contains(key);
        break;
    }

    return keySituation;
  }

  /**
   * Reads an individual Edge from JSON.  The edge must match the accepted GraphSON format.
   *
   * @param json         a single edge in GraphSON format as a String
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include when reading of element properties
   */
  public static Edge edgeFromJson(final String json, final Vertex out, final Vertex in,
                                  final ElementFactory factory, final GraphSONMode mode,
                                  final Set<String> propertyKeys) throws IOException, ParseException {
    final GraphSONUtility graphson = new GraphSONUtility(mode, factory, null, propertyKeys);
    return graphson.edgeFromJson(json, out, in);
  }

  /**
   * Reads an individual Edge from JSON.  The edge must match the accepted GraphSON format.
   *
   * @param json         a single edge in GraphSON format as an InputStream
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include when reading of element properties
   */
  public static Edge edgeFromJson(final InputStream json, final Vertex out, final Vertex in,
                                  final ElementFactory factory, final GraphSONMode mode,
                                  final Set<String> propertyKeys) throws IOException, ParseException {
    final GraphSONUtility graphson = new GraphSONUtility(mode, factory, null, propertyKeys);
    return graphson.edgeFromJson(json, out, in);
  }

  /**
   * Reads an individual Edge from JSON.  The edge must match the accepted GraphSON format.
   *
   * @param json         a single edge in GraphSON format as a Jackson JsonNode
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include when reading of element properties
   */
  public static Edge edgeFromJson(final JSONObject json, final Vertex out, final Vertex in,
                                  final ElementFactory factory, final GraphSONMode mode,
                                  final Set<String> propertyKeys) throws IOException {
    final GraphSONUtility graphson = new GraphSONUtility(mode, factory, null, propertyKeys);
    return graphson.edgeFromJson(json, out, in);
  }

  static Map<String, Object> readProperties(final JSONObject node, final boolean ignoreReservedKeys, final boolean hasEmbeddedTypes) {
    final Map<String, Object> map = new HashMap<String, Object>();

    for (Object objKey : node.keySet()) {
      String key = (String) objKey;
      Object value = node.get(key);

      if (!ignoreReservedKeys || !isReservedKey(key)) {
        map.put(key, readProperty(value, hasEmbeddedTypes));
      }
    }

    return map;
  }

  private static boolean isReservedKey(final String key) {
    return key.equals(GraphSONTokens._ID) || key.equals(GraphSONTokens._TYPE) || key.equals(GraphSONTokens._LABEL)
        || key.equals(GraphSONTokens._OUT_V) || key.equals(GraphSONTokens._IN_V);
  }

  private static JSONArray createJSONList(final List list, final Set<String> propertyKeys, final boolean showTypes) {
    JSONArray jsonList = new JSONArray();
    for (Object item : list) {
      if (item instanceof Element) {
        jsonList.add(objectNodeFromElement((Element) item, propertyKeys,
            showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL));
      } else if (item instanceof List) {
        jsonList.add(createJSONList((List) item, propertyKeys, showTypes));
      } else if (item instanceof Map) {
        jsonList.add(createJSONMap((Map) item, propertyKeys, showTypes));
      } else if (item != null && item.getClass().isArray()) {
        jsonList.add(createJSONList(convertArrayToList(item), propertyKeys, showTypes));
      } else {
        addObject(jsonList, item);
      }
    }
    return jsonList;
  }

  //
  private static JSONObject createJSONMap(final Map map, final Set<String> propertyKeys, final boolean showTypes) {
    final JSONObject jsonMap = new JSONObject();
    for (Object key : map.keySet()) {
      Object value = map.get(key);
      if (value != null) {
        if (value instanceof List) {
          value = createJSONList((List) value, propertyKeys, showTypes);
        } else if (value instanceof Map) {
          value = createJSONMap((Map) value, propertyKeys, showTypes);
        } else if (value instanceof Element) {
          value = objectNodeFromElement((Element) value, propertyKeys,
              showTypes ? GraphSONMode.EXTENDED : GraphSONMode.NORMAL);
        } else if (value.getClass().isArray()) {
          value = createJSONList(convertArrayToList(value), propertyKeys, showTypes);
        }
      }

      putObject(jsonMap, key.toString(), getValue(value, showTypes));
    }
    return jsonMap;

  }

  private static Object readProperty(final Object node, final boolean hasEmbeddedTypes) {
    Object propertyValue;

    if (hasEmbeddedTypes) {
      JSONObject json = (JSONObject) node;
      if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_UNKNOWN)) {
        propertyValue = null;
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_BOOLEAN)) {
        propertyValue = json.get(GraphSONTokens.VALUE);
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_FLOAT)) {
        propertyValue = ((Double) json.get(GraphSONTokens.VALUE)).floatValue();
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_DOUBLE)) {
        propertyValue = json.get(GraphSONTokens.VALUE);
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_INTEGER)) {
        propertyValue = ((Long) json.get(GraphSONTokens.VALUE)).intValue();
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_LONG)) {
        propertyValue = json.get(GraphSONTokens.VALUE);
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_STRING)) {
        propertyValue = json.get(GraphSONTokens.VALUE);
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_LIST)) {
        propertyValue = readProperties(((JSONArray) json.get(GraphSONTokens.VALUE)).iterator(), hasEmbeddedTypes);
      } else if (json.get(GraphSONTokens.TYPE).equals(GraphSONTokens.TYPE_MAP)) {
        propertyValue = readProperties((JSONObject) json.get(GraphSONTokens.VALUE), false, hasEmbeddedTypes);
      } else {
        propertyValue = node.toString();
      }
    } else {
      if (node == null) {
        propertyValue = null;
      } else if (node instanceof Boolean) {
        propertyValue = node;
      } else if (node instanceof Double) {
        propertyValue = node;
      } else if (node instanceof Integer) {
        propertyValue = node;
      } else if (node instanceof Long) {
        propertyValue = node;
      } else if (node instanceof String) {
        propertyValue = node;
      } else if (node instanceof JSONArray) {
        propertyValue = readProperties(((JSONArray) node).iterator(), hasEmbeddedTypes);
      } else if (node instanceof JSONObject) {
        propertyValue = readProperties((JSONObject) node, false, hasEmbeddedTypes);
      } else {
        propertyValue = node;
      }
    }

    return propertyValue;
  }

  private static void putObject(final JSONObject jsonMap, final String key, final Object value) {
    if (value == null) {
      jsonMap.put(key, null);
    } else if (value instanceof Boolean) {
      jsonMap.put(key, value);
    } else if (value instanceof Long) {
      jsonMap.put(key, value);
    } else if (value instanceof Integer) {
      jsonMap.put(key, value);
    } else if (value instanceof Float) {
      jsonMap.put(key, value);
    } else if (value instanceof Double) {
      jsonMap.put(key, value);
    } else if (value instanceof String) {
      jsonMap.put(key, value);
    } else if (value instanceof JSONObject) {
      jsonMap.put(key, value);
    } else if (value instanceof JSONArray) {
      jsonMap.put(key, value);
    } else {
      jsonMap.put(key, value.toString());
    }
  }

  private static List readProperties(final Iterator<JSONObject> listOfNodes, final boolean hasEmbeddedTypes) {
    final List array = new ArrayList();

    while (listOfNodes.hasNext()) {
      array.add(readProperty(listOfNodes.next(), hasEmbeddedTypes));
    }

    return array;
  }

  private static void addObject(final JSONArray jsonList, final Object value) {
    if (value == null) {
      jsonList.add(null);
    } else if (value instanceof Boolean) {
      jsonList.add(value);
    } else if (value instanceof Long) {
      jsonList.add(value);
    } else if (value instanceof Integer) {
      jsonList.add(value);
    } else if (value instanceof Float) {
      jsonList.add(value);
    } else if (value instanceof Double) {
      jsonList.add(value);
    } else if (value instanceof String) {
      jsonList.add(value);
    } else if (value instanceof JSONObject) {
      jsonList.add(value);
    } else if (value instanceof JSONArray) {
      jsonList.add(value);
    } else {
      jsonList.add(value.toString());
    }
  }

  private static Map createPropertyMap(final Element element, final Set<String> propertyKeys, final ElementPropertiesRule rule) {
    final Map map = new HashMap<String, Object>();

    if (propertyKeys == null) {
      for (String key : element.getPropertyKeys()) {
        map.put(key, element.getProperty(key));
      }
    } else {
      if (rule == ElementPropertiesRule.INCLUDE) {
        for (String key : propertyKeys) {
          Object valToPutInMap = element.getProperty(key);
          if (valToPutInMap != null) {
            map.put(key, valToPutInMap);
          }
        }
      } else {
        for (String key : element.getPropertyKeys()) {
          if (!propertyKeys.contains(key)) {
            map.put(key, element.getProperty(key));
          }
        }
      }
    }

    return map;
  }

  private static Object getValue(Object value, final boolean includeType) {

    Object returnValue = value;

    // if the includeType is set to true then show the data types of the properties
    if (includeType) {

      // type will be one of: map, list, string, long, int, double, float.
      // in the event of a complex object it will call a toString and store as a
      // string
      String type = determineType(value);

      JSONObject valueAndType = new JSONObject();
      valueAndType.put(GraphSONTokens.TYPE, type);

      if (type.equals(GraphSONTokens.TYPE_LIST)) {

        // values of lists must be accumulated as ObjectNode objects under the value key.
        // will return as a ArrayNode. called recursively to traverse the entire
        // object graph of each item in the array.
        JSONArray list = (JSONArray) value;

        // there is a set of values that must be accumulated as an array under a key
        JSONArray valueArray = new JSONArray();
        valueAndType.put(GraphSONTokens.VALUE, valueArray);
        for (int ix = 0; ix < list.size(); ix++) {
          // the value of each item in the array is a node object from an ArrayNode...must
          // get the value of it.
          addObject(valueArray, getValue(list.get(ix), includeType));
        }

      } else if (type.equals(GraphSONTokens.TYPE_MAP)) {

        // maps are converted to a ObjectNode.  called recursively to traverse
        // the entire object graph within the map.
        JSONObject convertedMap = new JSONObject();
        JSONObject jsonObject = (JSONObject) value;

        for (Object key : jsonObject.keySet()) {

          // no need to getValue() here as this is already a ObjectNode and should have type info
          convertedMap.put(key, jsonObject.get(key));
        }

        valueAndType.put(GraphSONTokens.VALUE, convertedMap);

      } else {

        // this must be a primitive value or a complex object.  if a complex
        // object it will be handled by a call to toString and stored as a
        // string value
        putObject(valueAndType, GraphSONTokens.VALUE, value);
      }

      // this goes back as a JSONObject with data type and value
      returnValue = valueAndType;
    }

    return returnValue;
  }

  private static List convertArrayToList(final Object value) {

    // is there seriously no better way to do this...bah!
    List list = new ArrayList();
    if (value instanceof int[]) {
      int[] arr = (int[]) value;
      for (int ix = 0; ix < arr.length; ix++) {
        list.add(arr[ix]);
      }
    } else if (value instanceof double[]) {
      double[] arr = (double[]) value;
      for (int ix = 0; ix < arr.length; ix++) {
        list.add(arr[ix]);
      }
    } else if (value instanceof float[]) {
      float[] arr = (float[]) value;
      for (int ix = 0; ix < arr.length; ix++) {
        list.add(arr[ix]);
      }
    } else if (value instanceof long[]) {
      long[] arr = (long[]) value;
      for (int ix = 0; ix < arr.length; ix++) {
        list.add(arr[ix]);
      }
    } else if (value instanceof boolean[]) {
      boolean[] arr = (boolean[]) value;
      for (int ix = 0; ix < arr.length; ix++) {
        list.add(arr[ix]);
      }
    } else {
      list = Arrays.asList((Object[]) value);
    }

    return list;
  }

  private static String determineType(final Object value) {
    String type = GraphSONTokens.TYPE_STRING;
    if (value == null) {
      type = "unknown";
    } else if (value instanceof Double) {
      type = GraphSONTokens.TYPE_DOUBLE;
    } else if (value instanceof Float) {
      type = GraphSONTokens.TYPE_FLOAT;
    } else if (value instanceof Integer) {
      type = GraphSONTokens.TYPE_INTEGER;
    } else if (value instanceof Long) {
      type = GraphSONTokens.TYPE_LONG;
    } else if (value instanceof Boolean) {
      type = GraphSONTokens.TYPE_BOOLEAN;
    } else if (value instanceof JSONArray) {
      type = GraphSONTokens.TYPE_LIST;
    } else if (value instanceof JSONObject) {
      type = GraphSONTokens.TYPE_MAP;
    }

    return type;
  }

  /**
   * Creates a vertex from GraphSON using settings supplied in the constructor.
   */
  public Vertex vertexFromJson(final InputStream json) throws ParseException, IOException {
    return this.vertexFromJson((JSONObject) parser.parse(new InputStreamReader(json)));
  }

  /**
   * Creates an edge from GraphSON using settings supplied in the constructor.
   */
  public Edge edgeFromJson(final String json, final Vertex out, final Vertex in) throws IOException, ParseException {
    return this.edgeFromJson((JSONObject) parser.parse(json), out, in);
  }

  /**
   * Creates an edge from GraphSON using settings supplied in the constructor.
   */
  public Edge edgeFromJson(final InputStream json, final Vertex out, final Vertex in) throws IOException, ParseException {
    return this.edgeFromJson((JSONObject) parser.parse(new InputStreamReader(json)), out, in);
  }

  /**
   * Creates an edge from GraphSON using settings supplied in the constructor.
   */
  public Edge edgeFromJson(final JSONObject json, final Vertex out, final Vertex in) throws IOException {
    final Map<String, Object> props = GraphSONUtility.readProperties(json, true, this.hasEmbeddedTypes);

//    final Object edgeId = getTypedValueFromJsonNode(json.get(GraphSONTokens._ID));
    final Object edgeId = json.get(GraphSONTokens._ID);

    final Object nodeLabel = json.get(GraphSONTokens._LABEL);
    final String label = nodeLabel == null ? null : nodeLabel.toString();

    final Edge e = factory.createEdge(edgeId, out, in, label);

    for (Map.Entry<String, Object> entry : props.entrySet()) {
      // if (this.edgePropertyKeys == null || this.edgePropertyKeys.contains(entry.getKey())) {
      if (includeKey(entry.getKey(), edgePropertyKeys, this.edgePropertiesRule)) {
        e.setProperty(entry.getKey(), entry.getValue());
      }
    }

    return e;
  }

  /**
   * Creates a vertex from GraphSON using settings supplied in the constructor.
   */
  public Vertex vertexFromJson(final String json) throws ParseException {
    return this.vertexFromJson((JSONObject) parser.parse(json));
  }

  /**
   * Creates a vertex from GraphSON using settings supplied in the constructor.
   */
  public Vertex vertexFromJson(final JSONObject json) {
    final Map<String, Object> props = readProperties(json, true, this.hasEmbeddedTypes);

    //final Object vertexId = getTypedValueFromJsonNode((JSONObject)json.get(GraphSONTokens._ID));
    final Object vertexId = json.get(GraphSONTokens._ID);
    final Vertex v = factory.createVertex(vertexId);

    for (Map.Entry<String, Object> entry : props.entrySet()) {
      //if (this.vertexPropertyKeys == null || vertexPropertyKeys.contains(entry.getKey())) {
      if (includeKey(entry.getKey(), vertexPropertyKeys, this.vertexPropertiesRule)) {
        v.setProperty(entry.getKey(), entry.getValue());
      }
    }

    return v;
  }

  /**
   * Creates GraphSON for a single graph element.
   */
  public JSONObject jsonFromElement(final Element element) {
    final JSONObject objectNode = this.objectNodeFromElement(element);
    return objectNode;
  }

  /**
   * Creates GraphSON for a single graph element.
   */
  public org.json.simple.JSONObject objectNodeFromElement(final Element element) {
    final boolean isEdge = element instanceof Edge;
    final boolean showTypes = mode == GraphSONMode.EXTENDED;
    final Set<String> propertyKeys = isEdge ? this.edgePropertyKeys : this.vertexPropertyKeys;
    final ElementPropertiesRule elementPropertyConfig = isEdge ? this.edgePropertiesRule : this.vertexPropertiesRule;

    final org.json.simple.JSONObject jsonElement = createJSONMap(createPropertyMap(element, propertyKeys, elementPropertyConfig), propertyKeys, showTypes);

    if ((isEdge && this.includeReservedEdgeId) || (!isEdge && this.includeReservedVertexId)) {
      putObject(jsonElement, GraphSONTokens._ID, element.getId());
    }

    // it's important to keep the order of these straight.  check Edge first and then Vertex because there
    // are graph implementations that have Edge extend from Vertex
    if (element instanceof Edge) {
      final Edge edge = (Edge) element;

      if (this.includeReservedEdgeId) {
        putObject(jsonElement, GraphSONTokens._ID, element.getId());
      }

      if (this.includeReservedEdgeType) {
        jsonElement.put(GraphSONTokens._TYPE, GraphSONTokens.EDGE);
      }

      if (this.includeReservedEdgeOutV) {
        putObject(jsonElement, GraphSONTokens._OUT_V, edge.getVertex(Direction.OUT).getId());
      }

      if (this.includeReservedEdgeInV) {
        putObject(jsonElement, GraphSONTokens._IN_V, edge.getVertex(Direction.IN).getId());
      }

      if (this.includeReservedEdgeLabel) {
        jsonElement.put(GraphSONTokens._LABEL, edge.getLabel());
      }
    } else if (element instanceof Vertex) {
      if (this.includeReservedVertexId) {
        putObject(jsonElement, GraphSONTokens._ID, element.getId());
      }

      if (this.includeReservedVertexType) {
        jsonElement.put(GraphSONTokens._TYPE, GraphSONTokens.VERTEX);
      }
    }

    return jsonElement;
  }
}
