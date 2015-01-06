/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.graph.graphson;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.io.Charsets;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.sonar.core.graph.graphson.ElementPropertyConfig.ElementPropertiesRule;

/**
 * Helps write individual graph elements to TinkerPop JSON format known as GraphSON.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphsonUtil {

  private final GraphsonMode mode;
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
  GraphsonUtil(GraphsonMode mode, ElementFactory factory) {
    this(mode, factory, ElementPropertyConfig.AllProperties);
  }

  /**
   * A GraphSONUtility that includes the specified properties.
   */
  GraphsonUtil(GraphsonMode mode, ElementFactory factory,
    Set<String> vertexPropertyKeys, Set<String> edgePropertyKeys) {
    this(mode, factory, ElementPropertyConfig.includeProperties(vertexPropertyKeys, edgePropertyKeys));
  }

  GraphsonUtil(GraphsonMode mode, ElementFactory factory,
    ElementPropertyConfig config) {
    this.vertexPropertyKeys = config.getVertexPropertyKeys();
    this.edgePropertyKeys = config.getEdgePropertyKeys();
    this.vertexPropertiesRule = config.getVertexPropertiesRule();
    this.edgePropertiesRule = config.getEdgePropertiesRule();

    this.mode = mode;
    this.factory = factory;
    this.hasEmbeddedTypes = mode == GraphsonMode.EXTENDED;

    this.includeReservedVertexId = includeReservedKey(mode, GraphsonTokens._ID, vertexPropertyKeys, this.vertexPropertiesRule);
    this.includeReservedEdgeId = includeReservedKey(mode, GraphsonTokens._ID, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedVertexType = includeReservedKey(mode, GraphsonTokens._TYPE, vertexPropertyKeys, this.vertexPropertiesRule);
    this.includeReservedEdgeType = includeReservedKey(mode, GraphsonTokens._TYPE, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedEdgeLabel = includeReservedKey(mode, GraphsonTokens._LABEL, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedEdgeOutV = includeReservedKey(mode, GraphsonTokens._OUT_V, edgePropertyKeys, this.edgePropertiesRule);
    this.includeReservedEdgeInV = includeReservedKey(mode, GraphsonTokens._IN_V, edgePropertyKeys, this.edgePropertiesRule);
  }

  /**
   * Creates a JSONObject from a graph element.
   *
   * @param element      the graph element to convert to JSON.
   * @param propertyKeys The property keys at the root of the element to serialize.  If null, then all keys are serialized.
   * @param mode         the type of GraphSON to be generated.
   */
  static JSONObject jsonFromElement(Element element, @Nullable Set<String> propertyKeys, GraphsonMode mode) {
    GraphsonUtil graphson = element instanceof Edge ? new GraphsonUtil(mode, null, null, propertyKeys)
      : new GraphsonUtil(mode, null, propertyKeys, null);
    return graphson.jsonFromElement(element);
  }

  /**
   * Reads an individual Vertex from JSON.  The vertex must match the accepted GraphSON format.
   *
   * @param json         a single vertex in GraphSON format as JSONObject
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include on reading of element properties
   */
  static Vertex vertexFromJson(JSONObject json, ElementFactory factory, GraphsonMode mode,
    Set<String> propertyKeys) throws IOException {
    GraphsonUtil graphson = new GraphsonUtil(mode, factory, propertyKeys, null);
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
  static Vertex vertexFromJson(String json, ElementFactory factory, GraphsonMode mode,
    Set<String> propertyKeys) throws ParseException {
    GraphsonUtil graphson = new GraphsonUtil(mode, factory, propertyKeys, null);
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
  static Vertex vertexFromJson(InputStream json, ElementFactory factory, GraphsonMode mode,
    Set<String> propertyKeys) throws IOException, ParseException {
    GraphsonUtil graphson = new GraphsonUtil(mode, factory, propertyKeys, null);
    return graphson.vertexFromJson(json);
  }

  private static boolean includeReservedKey(GraphsonMode mode, String key,
    Set<String> propertyKeys,
    ElementPropertiesRule rule) {
    // the key is always included in modes other than compact. if it is compact, then validate that the
    // key is in the property key list
    return mode != GraphsonMode.COMPACT || includeKey(key, propertyKeys, rule);
  }

  private static boolean includeKey(String key, Set<String> propertyKeys,
    ElementPropertiesRule rule) {
    if (propertyKeys == null) {
      // when null always include the key and shortcut this piece
      return true;
    }

    // default the key situation. if it's included then it should be explicitly defined in the
    // property keys list to be included or the reverse otherwise
    boolean keySituation = rule == ElementPropertiesRule.INCLUDE;

    if (rule == ElementPropertiesRule.INCLUDE) {
      keySituation = propertyKeys.contains(key);
    } else if (rule == ElementPropertiesRule.EXCLUDE) {
      keySituation = !propertyKeys.contains(key);
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
  static Edge edgeFromJson(String json, Vertex out, Vertex in,
    ElementFactory factory, GraphsonMode mode,
    Set<String> propertyKeys) throws IOException, ParseException {
    GraphsonUtil graphson = new GraphsonUtil(mode, factory, null, propertyKeys);
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
  static Edge edgeFromJson(InputStream json, Vertex out, Vertex in,
    ElementFactory factory, GraphsonMode mode,
    Set<String> propertyKeys) throws IOException, ParseException {
    GraphsonUtil graphson = new GraphsonUtil(mode, factory, null, propertyKeys);
    return graphson.edgeFromJson(json, out, in);
  }

  /**
   * Reads an individual Edge from JSON.  The edge must match the accepted GraphSON format.
   *
   * @param json         a single edge in GraphSON format as a JSONObject
   * @param factory      the factory responsible for constructing graph elements
   * @param mode         the mode of the GraphSON
   * @param propertyKeys a list of keys to include when reading of element properties
   */
  static Edge edgeFromJson(JSONObject json, Vertex out, Vertex in,
    ElementFactory factory, GraphsonMode mode,
    Set<String> propertyKeys) throws IOException {
    GraphsonUtil graphson = new GraphsonUtil(mode, factory, null, propertyKeys);
    return graphson.edgeFromJson(json, out, in);
  }

  static Map<String, Object> readProperties(JSONObject node, boolean ignoreReservedKeys, boolean hasEmbeddedTypes) {
    Map<String, Object> map = new HashMap<String, Object>();

    for (Object objKey : node.keySet()) {
      String key = (String) objKey;
      Object value = node.get(key);

      if (!ignoreReservedKeys || !isReservedKey(key)) {
        map.put(key, readProperty(value, hasEmbeddedTypes));
      }
    }

    return map;
  }

  private static boolean isReservedKey(String key) {
    return key.equals(GraphsonTokens._ID) || key.equals(GraphsonTokens._TYPE) || key.equals(GraphsonTokens._LABEL)
      || key.equals(GraphsonTokens._OUT_V) || key.equals(GraphsonTokens._IN_V);
  }

  private static JSONArray createJSONList(List list, Set<String> propertyKeys, boolean showTypes) {
    JSONArray jsonList = new JSONArray();
    for (Object item : list) {
      if (item instanceof Element) {
        jsonList.add(jsonFromElement((Element) item, propertyKeys,
          showTypes ? GraphsonMode.EXTENDED : GraphsonMode.NORMAL));
      } else if (item instanceof List) {
        jsonList.add(createJSONList((List) item, propertyKeys, showTypes));
      } else if (item instanceof Map) {
        jsonList.add(createJSONMap((Map) item, propertyKeys, showTypes));
      } else if (item != null && item.getClass().isArray()) {
        jsonList.add(createJSONList(convertArrayToList(item), propertyKeys, showTypes));
      } else if (item instanceof Set) {
        throw new UnsupportedOperationException("Set property is not supported");
      } else {
        addObject(jsonList, item);
      }
    }
    return jsonList;
  }

  //
  private static JSONObject createJSONMap(Map<Object, Object> map, Set<String> propertyKeys, boolean showTypes) {
    JSONObject jsonMap = new JSONObject();
    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      Object value = entry.getValue();
      if (value != null) {
        if (value instanceof List) {
          value = createJSONList((List) value, propertyKeys, showTypes);
        } else if (value instanceof Map) {
          value = createJSONMap((Map) value, propertyKeys, showTypes);
        } else if (value instanceof Element) {
          value = jsonFromElement((Element) value, propertyKeys,
            showTypes ? GraphsonMode.EXTENDED : GraphsonMode.NORMAL);
        } else if (value.getClass().isArray()) {
          value = createJSONList(convertArrayToList(value), propertyKeys, showTypes);
        }
      }

      putObject(jsonMap, entry.getKey().toString(), getValue(value, showTypes));
    }
    return jsonMap;

  }

  private static Object readProperty(Object node, boolean hasEmbeddedTypes) {
    Object propertyValue;

    if (hasEmbeddedTypes) {
      JSONObject json = (JSONObject) node;
      if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_UNKNOWN)) {
        propertyValue = null;
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_BOOLEAN)) {
        propertyValue = json.get(GraphsonTokens.VALUE);
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_FLOAT)) {
        propertyValue = ((Double) json.get(GraphsonTokens.VALUE)).floatValue();
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_DOUBLE)) {
        propertyValue = json.get(GraphsonTokens.VALUE);
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_INTEGER)) {
        propertyValue = ((Long) json.get(GraphsonTokens.VALUE)).intValue();
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_LONG)) {
        propertyValue = json.get(GraphsonTokens.VALUE);
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_STRING)) {
        propertyValue = json.get(GraphsonTokens.VALUE);
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_LIST)) {
        propertyValue = readProperties(((JSONArray) json.get(GraphsonTokens.VALUE)).iterator(), hasEmbeddedTypes);
      } else if (json.get(GraphsonTokens.TYPE).equals(GraphsonTokens.TYPE_MAP)) {
        propertyValue = readProperties((JSONObject) json.get(GraphsonTokens.VALUE), false, hasEmbeddedTypes);
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

  private static void putObject(JSONObject jsonMap, String key, Object value) {
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

  private static List readProperties(Iterator<JSONObject> listOfNodes, boolean hasEmbeddedTypes) {
    List array = new ArrayList();

    while (listOfNodes.hasNext()) {
      array.add(readProperty(listOfNodes.next(), hasEmbeddedTypes));
    }

    return array;
  }

  private static void addObject(JSONArray jsonList, Object value) {
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

  private static Map createPropertyMap(Element element, Set<String> propertyKeys, ElementPropertiesRule rule) {
    Map map = new HashMap<String, Object>();

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

  private static Object getValue(Object value, boolean includeType) {

    Object returnValue = value;

    // if the includeType is set to true then show the data types of the properties
    if (includeType) {

      // type will be one of: map, list, string, long, int, double, float.
      // in the event of a complex object it will call a toString and store as a
      // string
      String type = determineType(value);

      JSONObject valueAndType = new JSONObject();
      valueAndType.put(GraphsonTokens.TYPE, type);

      if (type.equals(GraphsonTokens.TYPE_LIST)) {

        // values of lists must be accumulated as ObjectNode objects under the value key.
        // will return as a ArrayNode. called recursively to traverse the entire
        // object graph of each item in the array.
        JSONArray list = (JSONArray) value;

        // there is a set of values that must be accumulated as an array under a key
        JSONArray valueArray = new JSONArray();
        valueAndType.put(GraphsonTokens.VALUE, valueArray);
        for (int ix = 0; ix < list.size(); ix++) {
          // the value of each item in the array is a node object from an ArrayNode...must
          // get the value of it.
          addObject(valueArray, getValue(list.get(ix), includeType));
        }

      } else if (type.equals(GraphsonTokens.TYPE_MAP)) {

        // maps are converted to a ObjectNode. called recursively to traverse
        // the entire object graph within the map.
        JSONObject convertedMap = new JSONObject();
        JSONObject jsonObject = (JSONObject) value;

        Map<Object, Object> jsonObjectMap = jsonObject;
        for (Map.Entry<Object, Object> entry : jsonObjectMap.entrySet()) {

          // no need to getValue() here as this is already a ObjectNode and should have type info
          convertedMap.put(entry.getKey(), entry.getValue());
        }

        valueAndType.put(GraphsonTokens.VALUE, convertedMap);

      } else {

        // this must be a primitive value or a complex object. if a complex
        // object it will be handled by a call to toString and stored as a
        // string value
        putObject(valueAndType, GraphsonTokens.VALUE, value);
      }

      // this goes back as a JSONObject with data type and value
      returnValue = valueAndType;
    }

    return returnValue;
  }

  private static List convertArrayToList(Object value) {

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

  private static String determineType(Object value) {
    String type = GraphsonTokens.TYPE_STRING;
    if (value == null) {
      type = "unknown";
    } else if (value instanceof Double) {
      type = GraphsonTokens.TYPE_DOUBLE;
    } else if (value instanceof Float) {
      type = GraphsonTokens.TYPE_FLOAT;
    } else if (value instanceof Integer) {
      type = GraphsonTokens.TYPE_INTEGER;
    } else if (value instanceof Long) {
      type = GraphsonTokens.TYPE_LONG;
    } else if (value instanceof Boolean) {
      type = GraphsonTokens.TYPE_BOOLEAN;
    } else if (value instanceof JSONArray) {
      type = GraphsonTokens.TYPE_LIST;
    } else if (value instanceof JSONObject) {
      type = GraphsonTokens.TYPE_MAP;
    }

    return type;
  }

  /**
   * Creates a vertex from GraphSON using settings supplied in the constructor.
   */
  Vertex vertexFromJson(InputStream json) throws ParseException, IOException {
    return this.vertexFromJson((JSONObject) parser.parse(new InputStreamReader(json, Charsets.UTF_8)));
  }

  /**
   * Creates an edge from GraphSON using settings supplied in the constructor.
   */
  Edge edgeFromJson(String json, Vertex out, Vertex in) throws IOException, ParseException {
    return this.edgeFromJson((JSONObject) parser.parse(json), out, in);
  }

  /**
   * Creates an edge from GraphSON using settings supplied in the constructor.
   */
  Edge edgeFromJson(InputStream json, Vertex out, Vertex in) throws IOException, ParseException {
    return this.edgeFromJson((JSONObject) parser.parse(new InputStreamReader(json, Charsets.UTF_8)), out, in);
  }

  /**
   * Creates an edge from GraphSON using settings supplied in the constructor.
   */
  Edge edgeFromJson(JSONObject json, Vertex out, Vertex in) throws IOException {
    Map<String, Object> props = GraphsonUtil.readProperties(json, true, this.hasEmbeddedTypes);

    Object edgeId = json.get(GraphsonTokens._ID);

    Object nodeLabel = json.get(GraphsonTokens._LABEL);
    String label = nodeLabel == null ? null : nodeLabel.toString();

    Edge e = factory.createEdge(edgeId, out, in, label);

    for (Map.Entry<String, Object> entry : props.entrySet()) {
      if (includeKey(entry.getKey(), edgePropertyKeys, this.edgePropertiesRule)) {
        e.setProperty(entry.getKey(), entry.getValue());
      }
    }

    return e;
  }

  /**
   * Creates a vertex from GraphSON using settings supplied in the constructor.
   */
  Vertex vertexFromJson(String json) throws ParseException {
    return this.vertexFromJson((JSONObject) parser.parse(json));
  }

  /**
   * Creates a vertex from GraphSON using settings supplied in the constructor.
   */
  Vertex vertexFromJson(JSONObject json) {
    Map<String, Object> props = readProperties(json, true, this.hasEmbeddedTypes);

    Object vertexId = json.get(GraphsonTokens._ID);
    Vertex v = factory.createVertex(vertexId);

    for (Map.Entry<String, Object> entry : props.entrySet()) {
      if (includeKey(entry.getKey(), vertexPropertyKeys, this.vertexPropertiesRule)) {
        v.setProperty(entry.getKey(), entry.getValue());
      }
    }

    return v;
  }

  /**
   * Creates GraphSON for a single graph element.
   */
  JSONObject jsonFromElement(Element element) {
    boolean isEdge = element instanceof Edge;
    boolean showTypes = mode == GraphsonMode.EXTENDED;
    Set<String> propertyKeys = isEdge ? this.edgePropertyKeys : this.vertexPropertyKeys;
    ElementPropertiesRule elementPropertyConfig = isEdge ? this.edgePropertiesRule : this.vertexPropertiesRule;

    JSONObject jsonElement = createJSONMap(createPropertyMap(element, propertyKeys, elementPropertyConfig), propertyKeys, showTypes);

    if ((isEdge && this.includeReservedEdgeId) || (!isEdge && this.includeReservedVertexId)) {
      putObject(jsonElement, GraphsonTokens._ID, element.getId());
    }

    // it's important to keep the order of these straight. check Edge first and then Vertex because there
    // are graph implementations that have Edge extend from Vertex
    if (element instanceof Edge) {
      Edge edge = (Edge) element;

      if (this.includeReservedEdgeId) {
        putObject(jsonElement, GraphsonTokens._ID, element.getId());
      }

      if (this.includeReservedEdgeType) {
        jsonElement.put(GraphsonTokens._TYPE, GraphsonTokens.EDGE);
      }

      if (this.includeReservedEdgeOutV) {
        putObject(jsonElement, GraphsonTokens._OUT_V, edge.getVertex(Direction.OUT).getId());
      }

      if (this.includeReservedEdgeInV) {
        putObject(jsonElement, GraphsonTokens._IN_V, edge.getVertex(Direction.IN).getId());
      }

      if (this.includeReservedEdgeLabel) {
        jsonElement.put(GraphsonTokens._LABEL, edge.getLabel());
      }
    } else if (element instanceof Vertex) {
      if (this.includeReservedVertexId) {
        putObject(jsonElement, GraphsonTokens._ID, element.getId());
      }

      if (this.includeReservedVertexType) {
        jsonElement.put(GraphsonTokens._TYPE, GraphsonTokens.VERTEX);
      }
    }

    return jsonElement;
  }
}
