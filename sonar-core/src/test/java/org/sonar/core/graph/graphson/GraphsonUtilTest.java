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
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONTokens;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphsonUtilTest {
  private final String vertexJson1 = "{\"name\":\"marko\",\"age\":29,\"_id\":1,\"_type\":\"vertex\"}";
  private final String vertexJson2 = "{\"name\":\"vadas\",\"age\":27,\"_id\":2,\"_type\":\"vertex\"}";
  private final String edgeJsonLight = "{\"weight\":0.5,\"_outV\":1,\"_inV\":2}";
  private final String edgeJson = "{\"weight\":0.5,\"_id\":7,\"_type\":\"edge\",\"_outV\":1,\"_inV\":2,\"_label\":\"knows\"}";
  private TinkerGraph graph = new TinkerGraph();
  private InputStream inputStreamVertexJson1;
  private InputStream inputStreamEdgeJsonLight;

  @Before
  public void setUp() {
    this.graph.clear();

    this.inputStreamVertexJson1 = new ByteArrayInputStream(vertexJson1.getBytes());
    this.inputStreamEdgeJsonLight = new ByteArrayInputStream(edgeJsonLight.getBytes());
  }

  @Test
  public void jsonFromElementEdgeNoPropertiesNoKeysNoTypes() {
    Vertex v1 = this.graph.addVertex(1);
    Vertex v2 = this.graph.addVertex(2);

    Edge e = this.graph.addEdge(3, v1, v2, "test");
    e.setProperty("weight", 0.5f);

    JSONObject json = GraphsonUtil.jsonFromElement(e, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("3");
    assertThat(json.containsKey(GraphSONTokens._LABEL)).isTrue();
    assertThat(json.get(GraphSONTokens._LABEL)).isEqualTo("test");
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isTrue();
    assertThat(json.get(GraphSONTokens._TYPE)).isEqualTo("edge");
    assertThat(json.containsKey(GraphSONTokens._IN_V)).isTrue();
    assertThat(json.get(GraphSONTokens._IN_V)).isEqualTo("2");
    assertThat(json.containsKey(GraphSONTokens._OUT_V)).isTrue();
    assertThat(json.get(GraphSONTokens._OUT_V)).isEqualTo("1");
    assertThat(json.containsKey("weight")).isTrue();
    assertThat(json.get("weight")).isEqualTo(0.5f);
  }

  @Test
  public void jsonFromElementEdgeCompactIdOnlyAsInclude() {
    Vertex v1 = this.graph.addVertex(1);
    Vertex v2 = this.graph.addVertex(2);

    Edge e = this.graph.addEdge(3, v1, v2, "test");
    e.setProperty("weight", 0.5f);

    Set<String> propertiesToInclude = new HashSet<String>() {{
      add(GraphSONTokens._ID);
    }};

    JSONObject json = GraphsonUtil.jsonFromElement(e, propertiesToInclude, GraphsonMode.COMPACT);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._LABEL)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._IN_V)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._OUT_V)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.containsKey("weight")).isFalse();
  }

  @Test
  public void jsonFromElementEdgeCompactIdOnlyAsExclude() {
    ElementFactory factory = new ElementFactory(this.graph);
    Vertex v1 = this.graph.addVertex(1);
    Vertex v2 = this.graph.addVertex(2);

    Edge e = this.graph.addEdge(3, v1, v2, "test");
    e.setProperty("weight", 0.5f);
    e.setProperty("x", "y");

    Set<String> propertiesToExclude = new HashSet<String>() {{
      add(GraphSONTokens._TYPE);
      add(GraphSONTokens._LABEL);
      add(GraphSONTokens._IN_V);
      add(GraphSONTokens._OUT_V);
      add("weight");
    }};

    ElementPropertyConfig config = new ElementPropertyConfig(null, propertiesToExclude,
        ElementPropertyConfig.ElementPropertiesRule.INCLUDE,
        ElementPropertyConfig.ElementPropertiesRule.EXCLUDE);
    GraphsonUtil utility = new GraphsonUtil(GraphsonMode.COMPACT, factory, config);
    JSONObject json = utility.jsonFromElement(e);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._LABEL)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._IN_V)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._OUT_V)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.containsKey("weight")).isFalse();
    assertThat(json.containsKey("x")).isTrue();
    assertThat(json.get("x")).isEqualTo("y");
  }

  @Test
  public void jsonFromElementEdgeCompactAllKeys() {
    Vertex v1 = this.graph.addVertex(1);
    Vertex v2 = this.graph.addVertex(2);

    Edge e = this.graph.addEdge(3, v1, v2, "test");
    e.setProperty("weight", 0.5f);

    JSONObject json = GraphsonUtil.jsonFromElement(e, null, GraphsonMode.COMPACT);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isTrue();
    assertThat(json.containsKey(GraphSONTokens._LABEL)).isTrue();
    assertThat(json.containsKey(GraphSONTokens._IN_V)).isTrue();
    assertThat(json.containsKey(GraphSONTokens._OUT_V)).isTrue();
    assertThat(json.get("weight")).isEqualTo(0.5f);
  }

  @Test
  public void jsonFromElementVertexNoPropertiesNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("name", "marko");

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isTrue();
    assertThat(json.get(GraphSONTokens._TYPE)).isEqualTo("vertex");
    assertThat(json.get("name")).isEqualTo("marko");
  }

  @Test
  public void jsonFromElementVertexCompactIdOnlyAsInclude() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("name", "marko");

    Set<String> propertiesToInclude = new HashSet<String>() {{
      add(GraphSONTokens._ID);
    }};

    JSONObject json = GraphsonUtil.jsonFromElement(v, propertiesToInclude, GraphsonMode.COMPACT);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.containsKey("name")).isFalse();
  }

  @Test
  public void jsonFromElementVertexCompactIdNameOnlyAsExclude() {
    ElementFactory factory = new ElementFactory(this.graph);
    Vertex v = this.graph.addVertex(1);
    v.setProperty("name", "marko");

    Set<String> propertiesToExclude = new HashSet<String>() {{
      add(GraphSONTokens._TYPE);
    }};

    ElementPropertyConfig config = new ElementPropertyConfig(propertiesToExclude, null,
        ElementPropertyConfig.ElementPropertiesRule.EXCLUDE,
        ElementPropertyConfig.ElementPropertiesRule.EXCLUDE);

    GraphsonUtil utility = new GraphsonUtil(GraphsonMode.COMPACT, factory, config);
    JSONObject json = utility.jsonFromElement(v);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isFalse();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.containsKey("name")).isTrue();
  }

  @Test
  public void jsonFromElementVertexCompactAllOnly() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("name", "marko");

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.COMPACT);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._TYPE)).isTrue();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.containsKey("name")).isTrue();
  }

  @Test
  public void jsonFromElementVertexPrimitivePropertiesNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("keyString", "string");
    v.setProperty("keyLong", 1L);
    v.setProperty("keyInt", 2);
    v.setProperty("keyFloat", 3.3f);
    v.setProperty("keyExponentialDouble", 1312928167.626012);
    v.setProperty("keyDouble", 4.4);
    v.setProperty("keyBoolean", true);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.containsKey(GraphSONTokens._ID)).isTrue();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyString")).isTrue();
    assertThat(json.get("keyString")).isEqualTo("string");
    assertThat(json.containsKey("keyLong")).isTrue();
    assertThat(json.get("keyLong")).isEqualTo(1L);
    assertThat(json.containsKey("keyInt")).isTrue();
    assertThat(json.get("keyInt")).isEqualTo(2);
    assertThat(json.containsKey("keyFloat")).isTrue();
    assertThat(json.get("keyFloat")).isEqualTo(3.3f);
    assertThat(json.containsKey("keyExponentialDouble")).isTrue();
    assertThat(json.get("keyExponentialDouble")).isEqualTo(1312928167.626012);
    assertThat(json.containsKey("keyDouble")).isTrue();
    assertThat(json.get("keyDouble")).isEqualTo(4.4);
    assertThat(json.containsKey("keyBoolean")).isTrue();
    assertThat(json.get("keyBoolean")).isEqualTo(true);
  }

  @Test
  public void jsonFromElementVertexMapPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    Map map = new HashMap();
    map.put("this", "some");
    map.put("that", 1);

    v.setProperty("keyMap", map);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyMap")).isTrue();

    JSONObject mapAsJSON = (JSONObject) json.get("keyMap");
    assertThat(mapAsJSON).isNotNull();
    assertThat(mapAsJSON.containsKey("this")).isTrue();
    assertThat(mapAsJSON.get("this")).isEqualTo("some");
    assertThat(mapAsJSON.containsKey("that")).isTrue();
    assertThat(mapAsJSON.get("that")).isEqualTo(1);
  }

  @Test
  public void jsonFromElementVertexListPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    List<Object> list = new ArrayList<Object>();
    list.add("this");
    list.add("that");
    list.add("other");
    list.add(true);

    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyList")).isTrue();

    JSONArray listAsJSON = (JSONArray) json.get("keyList");
    assertThat(listAsJSON).isNotNull();
    assertThat(listAsJSON).hasSize(4);
  }

  @Test
  public void jsonFromElementVertexStringArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    String[] stringArray = new String[]{"this", "that", "other"};

    v.setProperty("keyStringArray", stringArray);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyStringArray")).isTrue();

    JSONArray stringArrayAsJSON = (JSONArray) json.get("keyStringArray");
    assertThat(stringArrayAsJSON).isNotNull();
    assertThat(stringArrayAsJSON).hasSize(3);
  }

  @Test
  public void jsonFromElementVertexDoubleArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    double[] doubleArray = new double[]{1.0, 2.0, 3.0};

    v.setProperty("keyDoubleArray", doubleArray);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyDoubleArray")).isTrue();

    JSONArray doubleArrayAsJSON = (JSONArray) json.get("keyDoubleArray");
    assertThat(doubleArrayAsJSON).isNotNull();
    assertThat(doubleArrayAsJSON).hasSize(3);
  }

  @Test
  public void jsonFromElementVertexIntArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    int[] intArray = new int[]{1, 2, 3};

    v.setProperty("keyIntArray", intArray);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyIntArray")).isTrue();

    JSONArray intArrayAsJSON = (JSONArray) json.get("keyIntArray");
    assertThat(intArrayAsJSON).isNotNull();
    assertThat(intArrayAsJSON).hasSize(3);
  }

  @Test
  public void jsonFromElementVertexLongArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    long[] longArray = new long[]{1l, 2l, 3l};

    v.setProperty("keyLongArray", longArray);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyLongArray")).isTrue();

    JSONArray longArrayAsJSON = (JSONArray) json.get("keyLongArray");
    assertThat(longArrayAsJSON).isNotNull();
    assertThat(longArrayAsJSON).hasSize(3);
  }

  @Test
  public void jsonFromElementFloatArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    float[] floatArray = new float[]{1.0f, 2.0f, 3.0f};

    v.setProperty("keyFloatArray", floatArray);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyFloatArray")).isTrue();

    JSONArray floatArrayAsJSON = (JSONArray) json.get("keyFloatArray");
    assertThat(floatArrayAsJSON).isNotNull();
    assertThat(floatArrayAsJSON).hasSize(3);
  }

  @Test
  public void jsonFromElementBooleanArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    boolean[] booleanArray = new boolean[]{true, false, true};

    v.setProperty("keyBooleanArray", booleanArray);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyBooleanArray")).isTrue();

    JSONArray booleanArrayAsJSON = (JSONArray) json.get("keyBooleanArray");
    assertThat(booleanArrayAsJSON).isNotNull();
    assertThat(booleanArrayAsJSON).hasSize(3);
  }

  @Test
  public void jsonFromElementVertexCatPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("mycat", new Cat("smithers"));

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("mycat")).isTrue();
    assertThat(json.get("mycat")).isEqualTo("smithers");
  }

  @Test
  public void jsonFromElementVertexCatPropertyNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("mycat", new Cat("smithers"));

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("mycat")).isTrue();

    JSONObject jsonObjectCat = (JSONObject) json.get("mycat");
    assertThat(jsonObjectCat.containsKey("value")).isTrue();
    assertThat(jsonObjectCat.get("value")).isEqualTo("smithers");
  }

  @Test
  public void jsonFromElementVertexCatArrayPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    List<Cat> cats = new ArrayList<Cat>();
    cats.add(new Cat("smithers"));
    cats.add(new Cat("mcallister"));

    v.setProperty("cats", cats);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("cats")).isTrue();

    JSONArray catListAsJson = (JSONArray) json.get("cats");
    assertThat(catListAsJson).hasSize(2);
  }

  @Test
  public void jsonFromElementCrazyPropertyNoKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    List mix = new ArrayList();
    mix.add(new Cat("smithers"));
    mix.add(true);

    List deepCats = new ArrayList();
    deepCats.add(new Cat("mcallister"));
    mix.add(deepCats);

    Map map = new HashMap();
    map.put("crazy", mix);

    int[] someInts = new int[]{1, 2, 3};
    map.put("ints", someInts);

    map.put("regular", "stuff");

    Map innerMap = new HashMap();
    innerMap.put("me", "you");

    map.put("inner", innerMap);

    v.setProperty("crazy-map", map);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("crazy-map")).isTrue();

    JSONObject mapAsJson = (JSONObject) json.get("crazy-map");
    assertThat(mapAsJson.containsKey("regular")).isTrue();
    assertThat(mapAsJson.get("regular")).isEqualTo("stuff");

    assertThat(mapAsJson.containsKey("ints")).isTrue();
    JSONArray intArrayAsJson = (JSONArray) mapAsJson.get("ints");
    assertThat(intArrayAsJson).hasSize(3);

    assertThat(mapAsJson.containsKey("crazy")).isTrue();
    JSONArray deepListAsJSON = (JSONArray) mapAsJson.get("crazy");
    assertThat(deepListAsJSON).hasSize(3);

    assertThat(mapAsJson.containsKey("inner")).isTrue();
    JSONObject mapInMapAsJSON = (JSONObject) mapAsJson.get("inner");
    assertThat(mapInMapAsJSON.containsKey("me")).isTrue();
    assertThat(mapInMapAsJSON.get("me")).isEqualTo("you");

  }

  @Test
  public void jsonFromElementVertexNoPropertiesWithKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("x", "X");
    v.setProperty("y", "Y");
    v.setProperty("z", "Z");

    Set<String> propertiesToInclude = new HashSet<String>();
    propertiesToInclude.add("y");
    JSONObject json = GraphsonUtil.jsonFromElement(v, propertiesToInclude, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.get(GraphSONTokens._TYPE)).isEqualTo("vertex");
    assertThat(json.containsKey("x")).isFalse();
    assertThat(json.containsKey("z")).isFalse();
    assertThat(json.containsKey("y")).isTrue();
  }

  @Test
  public void jsonFromElementVertexVertexPropertiesWithKeysNoTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("x", "X");
    v.setProperty("y", "Y");
    v.setProperty("z", "Z");

    Vertex innerV = this.graph.addVertex(2);
    innerV.setProperty("x", "X");
    innerV.setProperty("y", "Y");
    innerV.setProperty("z", "Z");

    v.setProperty("v", innerV);

    Set<String> propertiesToInclude = new HashSet<String>();
    propertiesToInclude.add("y");
    propertiesToInclude.add("v");

    JSONObject json = GraphsonUtil.jsonFromElement(v, propertiesToInclude, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.get(GraphSONTokens._TYPE)).isEqualTo("vertex");
    assertThat(json.containsKey("x")).isFalse();
    assertThat(json.containsKey("z")).isFalse();
    assertThat(json.containsKey("y")).isTrue();
    assertThat(json.containsKey("v")).isTrue();

    JSONObject innerJson = (JSONObject) json.get("v");
    assertThat(innerJson.containsKey("x")).isFalse();
    assertThat(innerJson.containsKey("z")).isFalse();
    assertThat(innerJson.containsKey("y")).isTrue();
    assertThat(innerJson.containsKey("v")).isFalse();
  }

  @Test
  public void jsonFromElementVertexPrimitivePropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    v.setProperty("keyString", "string");
    v.setProperty("keyLong", 1L);
    v.setProperty("keyInt", 2);
    v.setProperty("keyFloat", 3.3f);
    v.setProperty("keyDouble", 4.4);
    v.setProperty("keyBoolean", true);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyString")).isTrue();

    JSONObject valueAsJson = (JSONObject) json.get("keyString");
    assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_STRING);
    assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo("string");

    valueAsJson = (JSONObject) json.get("keyLong");
    assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LONG);
    assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(1L);

    valueAsJson = (JSONObject) json.get("keyInt");
    assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_INTEGER);
    assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(2);

    valueAsJson = (JSONObject) json.get("keyFloat");
    assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_FLOAT);
    assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(3.3f);

    valueAsJson = (JSONObject) json.get("keyDouble");
    assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_DOUBLE);
    assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(4.4);

    valueAsJson = (JSONObject) json.get("keyBoolean");
    assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_BOOLEAN);
    assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(true);
  }

  @Test
  public void jsonFromElementVertexListPropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    List<String> list = new ArrayList<String>();
    list.add("this");
    list.add("this");
    list.add("this");

    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyList")).isTrue();

    JSONObject listWithTypeAsJson = (JSONObject) json.get("keyList");
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(listWithTypeAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LIST);
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();

    JSONArray listAsJSON = (JSONArray) listWithTypeAsJson.get(GraphSONTokens.VALUE);
    assertThat(listAsJSON).isNotNull();
    assertThat(listAsJSON).hasSize(3);

    for (int ix = 0; ix < listAsJSON.size(); ix++) {
      JSONObject valueAsJson = (JSONObject) listAsJSON.get(ix);
      assertThat(valueAsJson).isNotNull();
      assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_STRING);
      assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo("this");
    }
  }

  @Test
  public void jsonFromElementVertexBooleanListPropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    List<Boolean> list = new ArrayList<Boolean>();
    list.add(true);
    list.add(true);
    list.add(true);

    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyList")).isTrue();

    JSONObject listWithTypeAsJson = (JSONObject) json.get("keyList");
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(listWithTypeAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LIST);
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();

    JSONArray listAsJSON = (JSONArray) listWithTypeAsJson.get(GraphSONTokens.VALUE);
    assertThat(listAsJSON).isNotNull();
    assertThat(listAsJSON).hasSize(3);

    for (int ix = 0; ix < listAsJSON.size(); ix++) {
      JSONObject valueAsJson = (JSONObject) listAsJSON.get(ix);
      assertThat(valueAsJson).isNotNull();
      assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_BOOLEAN);
      assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(true);
    }
  }

  @Test
  public void jsonFromElementVertexLongListPropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    List<Long> list = new ArrayList<Long>();
    list.add(1000L);
    list.add(1000L);
    list.add(1000L);

    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyList")).isTrue();

    JSONObject listWithTypeAsJson = (JSONObject) json.get("keyList");
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(listWithTypeAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LIST);
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();

    JSONArray listAsJSON = (JSONArray) listWithTypeAsJson.get(GraphSONTokens.VALUE);
    assertThat(listAsJSON).isNotNull();
    assertThat(listAsJSON).hasSize(3);

    for (int ix = 0; ix < listAsJSON.size(); ix++) {
      JSONObject valueAsJson = (JSONObject) listAsJSON.get(ix);
      assertThat(valueAsJson).isNotNull();
      assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LONG);
      assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(1000L);
    }
  }

  @Test
  public void jsonFromElementVertexIntListPropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    List<Integer> list = new ArrayList<Integer>();
    list.add(1);
    list.add(1);
    list.add(1);

    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyList")).isTrue();

    JSONObject listWithTypeAsJson = (JSONObject) json.get("keyList");
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(listWithTypeAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LIST);
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();

    JSONArray listAsJSON = (JSONArray) listWithTypeAsJson.get(GraphSONTokens.VALUE);
    assertThat(listAsJSON).isNotNull();
    assertThat(listAsJSON).hasSize(3);

    for (int ix = 0; ix < listAsJSON.size(); ix++) {
      JSONObject valueAsJson = (JSONObject) listAsJSON.get(ix);
      assertThat(valueAsJson).isNotNull();
      assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_INTEGER);
      assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(1);
    }
  }

  @Test
  public void jsonFromElementVertexListOfListPropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);
    List<Integer> list = new ArrayList<Integer>();
    list.add(1);
    list.add(1);
    list.add(1);

    List<List<Integer>> listList = new ArrayList<List<Integer>>();
    listList.add(list);

    v.setProperty("keyList", listList);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyList")).isTrue();

    JSONObject listWithTypeAsJson = (JSONObject) json.get("keyList");
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(listWithTypeAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_LIST);
    assertThat(listWithTypeAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();

    JSONArray array = (JSONArray) listWithTypeAsJson.get(GraphSONTokens.VALUE);
    JSONObject obj = (JSONObject) array.get(0);
    JSONArray listAsJSON = (JSONArray) obj.get(GraphSONTokens.VALUE);
    assertThat(listAsJSON).isNotNull();
    assertThat(listAsJSON).hasSize(3);

    for (int ix = 0; ix < listAsJSON.size(); ix++) {
      JSONObject valueAsJson = (JSONObject) listAsJSON.get(ix);
      assertThat(valueAsJson).isNotNull();
      assertThat(valueAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_INTEGER);
      assertThat(valueAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
      assertThat(valueAsJson.get(GraphSONTokens.VALUE)).isEqualTo(1);
    }
  }

  @Test
  public void jsonFromElementVertexMapPropertiesNoKeysWithTypes() {
    Vertex v = this.graph.addVertex(1);

    Map map = new HashMap();
    map.put("this", "some");
    map.put("that", 1);

    v.setProperty("keyMap", map);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);

    assertThat(json).isNotNull();
    assertThat(json.get(GraphSONTokens._ID)).isEqualTo("1");
    assertThat(json.containsKey("keyMap")).isTrue();

    JSONObject mapWithTypeAsJSON = (JSONObject) json.get("keyMap");
    assertThat(mapWithTypeAsJSON).isNotNull();
    assertThat(mapWithTypeAsJSON.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(mapWithTypeAsJSON.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_MAP);

    assertThat(mapWithTypeAsJSON.containsKey(GraphSONTokens.VALUE)).isTrue();
    JSONObject mapAsJSON = (JSONObject) mapWithTypeAsJSON.get(GraphSONTokens.VALUE);

    assertThat(mapAsJSON.containsKey("this")).isTrue();
    JSONObject thisAsJson = (JSONObject) mapAsJSON.get("this");
    assertThat(thisAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(thisAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_STRING);
    assertThat(thisAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(thisAsJson.get(GraphSONTokens.VALUE)).isEqualTo("some");

    assertThat(mapAsJSON.containsKey("that")).isTrue();
    JSONObject thatAsJson = (JSONObject) mapAsJSON.get("that");
    assertThat(thatAsJson.containsKey(GraphSONTokens.TYPE)).isTrue();
    assertThat(thatAsJson.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_INTEGER);
    assertThat(thatAsJson.containsKey(GraphSONTokens.VALUE)).isTrue();
    assertThat(thatAsJson.get(GraphSONTokens.VALUE)).isEqualTo(1);
  }

  @Test
  public void jsonFromElementNullsNoKeysNoTypes() {
    Graph g = new TinkerGraph();
    Vertex v = g.addVertex(1);
    v.setProperty("key", null);

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("innerkey", null);

    List<String> innerList = new ArrayList<String>();
    innerList.add(null);
    innerList.add("innerstring");
    map.put("list", innerList);

    v.setProperty("keyMap", map);

    List<String> list = new ArrayList<String>();
    list.add(null);
    list.add("string");
    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.NORMAL);

    assertThat(json).isNotNull();
    assertThat(json.get("key")).isNull();
    ;

    JSONObject jsonMap = (JSONObject) json.get("keyMap");
    assertThat(jsonMap).isNotNull();
    assertThat(jsonMap.get("innerkey")).isNull();

    JSONArray jsonInnerArray = (JSONArray) jsonMap.get("list");
    assertThat(jsonInnerArray).isNotNull();
    assertThat(jsonInnerArray.get(0)).isNull();
    assertThat(jsonInnerArray.get(1)).isEqualTo("innerstring");

    JSONArray jsonArray = (JSONArray) json.get("keyList");
    assertThat(jsonArray).isNotNull();
    assertThat(jsonArray.get(0)).isNull();
    assertThat(jsonArray.get(1)).isEqualTo("string");
  }

  @Test
  public void jsonFromElementNullsNoKeysWithTypes() {
    Graph g = new TinkerGraph();
    Vertex v = g.addVertex(1);
    v.setProperty("key", null);

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("innerkey", null);

    List<String> innerList = new ArrayList<String>();
    innerList.add(null);
    innerList.add("innerstring");
    map.put("list", innerList);

    v.setProperty("keyMap", map);

    List<String> list = new ArrayList<String>();
    list.add(null);
    list.add("string");
    v.setProperty("keyList", list);

    JSONObject json = GraphsonUtil.jsonFromElement(v, null, GraphsonMode.EXTENDED);


    assertThat(json).isNotNull();
    JSONObject jsonObjectKey = (JSONObject) json.get("key");
    assertThat(jsonObjectKey.get(GraphSONTokens.VALUE)).isNull();
    assertThat(jsonObjectKey.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_UNKNOWN);

    JSONObject keyMap = (JSONObject) json.get("keyMap");
    JSONObject jsonMap = (JSONObject) keyMap.get(GraphSONTokens.VALUE);
    assertThat(jsonMap).isNotNull();
    JSONObject jsonObjectMap = (JSONObject) jsonMap.get("innerkey");
    assertThat(jsonObjectMap.get(GraphSONTokens.VALUE)).isNull();
    assertThat(jsonObjectMap.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_UNKNOWN);

    JSONObject listJson = (JSONObject) jsonMap.get("list");
    JSONArray jsonInnerArray = (JSONArray) listJson.get(GraphSONTokens.VALUE);
    assertThat(jsonInnerArray).isNotNull();
    JSONObject jsonObjectInnerListFirst = (JSONObject) jsonInnerArray.get(0);
    assertThat(jsonObjectInnerListFirst.get(GraphSONTokens.VALUE)).isNull();
    assertThat(jsonObjectInnerListFirst.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_UNKNOWN);

    JSONObject keyList = (JSONObject) json.get("keyList");
    JSONArray jsonArray = (JSONArray) keyList.get(GraphSONTokens.VALUE);
    assertThat(jsonArray).isNotNull();
    JSONObject jsonObjectListFirst = (JSONObject) jsonArray.get(0);
    assertThat(jsonObjectListFirst.get(GraphSONTokens.VALUE)).isNull();
    assertThat(jsonObjectListFirst.get(GraphSONTokens.TYPE)).isEqualTo(GraphSONTokens.TYPE_UNKNOWN);
  }

  @Test
  public void vertexFromJsonValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1), factory, GraphsonMode.NORMAL, null);

    assertThat(v).isSameAs(g.getVertex(1));

    // tinkergraph converts id to string
    assertThat(v.getId()).isEqualTo("1");
    assertThat(v.getProperty("name")).isEqualTo("marko");
    assertThat(v.getProperty("age")).isEqualTo(29L);
  }

  @Test
  public void vertexFromJsonStringValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v = GraphsonUtil.vertexFromJson(vertexJson1, factory, GraphsonMode.NORMAL, null);

    assertThat(v).isSameAs(g.getVertex(1));

    // tinkergraph converts id to string
    assertThat(v.getId()).isEqualTo("1");
    assertThat(v.getProperty("name")).isEqualTo("marko");
    assertThat(v.getProperty("age")).isEqualTo(29L);
  }

  @Test
  public void vertexFromJsonStringValidExtended() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    String vertexJson = "{\"person\":{\"value\":\"marko\",\"type\":\"string\"},\"_id\":1,\"_type\":\"vertex\"}";
    Vertex v = GraphsonUtil.vertexFromJson(vertexJson, factory, GraphsonMode.EXTENDED, null);

    Assert.assertSame(v, g.getVertex(1));

    // tinkergraph converts id to string
    Assert.assertEquals("1", v.getId());
    Assert.assertEquals("marko", v.getProperty("person"));
  }

  @Test
  public void vertexFromJsonInputStreamValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v = GraphsonUtil.vertexFromJson(inputStreamVertexJson1, factory, GraphsonMode.NORMAL, null);

    Assert.assertSame(v, g.getVertex(1));

    // tinkergraph converts id to string
    Assert.assertEquals("1", v.getId());
    Assert.assertEquals("marko", v.getProperty("name"));
    Assert.assertEquals(29L, v.getProperty("age"));
  }

  @Test
  public void vertexFromJsonIgnoreKeyValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Set<String> ignoreAge = new HashSet<String>();
    ignoreAge.add("age");
    ElementPropertyConfig config = ElementPropertyConfig.excludeProperties(ignoreAge, null);
    GraphsonUtil utility = new GraphsonUtil(GraphsonMode.NORMAL, factory, config);
    Vertex v = utility.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1));

    Assert.assertSame(v, g.getVertex(1));

    // tinkergraph converts id to string
    Assert.assertEquals("1", v.getId());
    Assert.assertEquals("marko", v.getProperty("name"));
    Assert.assertNull(v.getProperty("age"));
  }

  @Test
  public void edgeFromJsonValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v1 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1), factory, GraphsonMode.NORMAL, null);
    Vertex v2 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson2), factory, GraphsonMode.NORMAL, null);
    Edge e = GraphsonUtil.edgeFromJson((JSONObject) JSONValue.parse(edgeJson), v1, v2, factory, GraphsonMode.NORMAL, null);

    Assert.assertSame(v1, g.getVertex(1));
    Assert.assertSame(v2, g.getVertex(2));
    Assert.assertSame(e, g.getEdge(7));

    // tinkergraph converts id to string
    Assert.assertEquals("7", e.getId());
    Assert.assertEquals(0.5d, e.getProperty("weight"));
    Assert.assertEquals("knows", e.getLabel());
    Assert.assertEquals(v1, e.getVertex(Direction.OUT));
    Assert.assertEquals(v2, e.getVertex(Direction.IN));
  }

  @Test
  public void edgeFromJsonStringValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v1 = GraphsonUtil.vertexFromJson(vertexJson1, factory, GraphsonMode.NORMAL, null);
    Vertex v2 = GraphsonUtil.vertexFromJson(vertexJson2, factory, GraphsonMode.NORMAL, null);
    Edge e = GraphsonUtil.edgeFromJson(edgeJson, v1, v2, factory, GraphsonMode.NORMAL, null);

    Assert.assertSame(v1, g.getVertex(1));
    Assert.assertSame(v2, g.getVertex(2));
    Assert.assertSame(e, g.getEdge(7));

    // tinkergraph converts id to string
    Assert.assertEquals("7", e.getId());
    Assert.assertEquals(0.5d, e.getProperty("weight"));
    Assert.assertEquals("knows", e.getLabel());
    Assert.assertEquals(v1, e.getVertex(Direction.OUT));
    Assert.assertEquals(v2, e.getVertex(Direction.IN));
  }

  @Test
  public void edgeFromJsonIgnoreWeightValid() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v1 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1), factory, GraphsonMode.NORMAL, null);
    Vertex v2 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson2), factory, GraphsonMode.NORMAL, null);

    Set<String> ignoreWeight = new HashSet<String>();
    ignoreWeight.add("weight");
    ElementPropertyConfig config = ElementPropertyConfig.excludeProperties(null, ignoreWeight);
    GraphsonUtil utility = new GraphsonUtil(GraphsonMode.NORMAL, factory, config);
    Edge e = utility.edgeFromJson((JSONObject) JSONValue.parse(edgeJson), v1, v2);

    Assert.assertSame(v1, g.getVertex(1));
    Assert.assertSame(v2, g.getVertex(2));
    Assert.assertSame(e, g.getEdge(7));

    // tinkergraph converts id to string
    Assert.assertEquals("7", e.getId());
    Assert.assertNull(e.getProperty("weight"));
    Assert.assertEquals("knows", e.getLabel());
    Assert.assertEquals(v1, e.getVertex(Direction.OUT));
    Assert.assertEquals(v2, e.getVertex(Direction.IN));
  }

  @Test
  public void edgeFromJsonNormalLabelOrIdOnEdge() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v1 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1), factory, GraphsonMode.NORMAL, null);
    Vertex v2 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson2), factory, GraphsonMode.NORMAL, null);
    Edge e = GraphsonUtil.edgeFromJson((JSONObject) JSONValue.parse(edgeJsonLight), v1, v2, factory, GraphsonMode.NORMAL, null);

    Assert.assertSame(v1, g.getVertex(1));
    Assert.assertSame(v2, g.getVertex(2));
    Assert.assertSame(e, g.getEdge(0));
  }

  @Test
  public void edgeFromJsonInputStreamCompactLabelOrIdOnEdge() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Vertex v1 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1), factory, GraphsonMode.COMPACT, null);
    Vertex v2 = GraphsonUtil.vertexFromJson((JSONObject) JSONValue.parse(vertexJson2), factory, GraphsonMode.COMPACT, null);
    Edge e = GraphsonUtil.edgeFromJson(inputStreamEdgeJsonLight, v1, v2, factory, GraphsonMode.COMPACT, null);

    Assert.assertSame(v1, g.getVertex(1));
    Assert.assertSame(v2, g.getVertex(2));
    Assert.assertSame(e, g.getEdge(0));
  }

  @Test
  public void edgeFromJsonInputStreamCompactNoIdOnEdge() throws Exception {
    Graph g = new TinkerGraph();
    ElementFactory factory = new ElementFactory(g);

    Set<String> vertexKeys = new HashSet<String>() {{
      add(GraphSONTokens._ID);
    }};

    Set<String> edgeKeys = new HashSet<String>() {{
      add(GraphSONTokens._IN_V);
    }};

    GraphsonUtil graphson = new GraphsonUtil(GraphsonMode.COMPACT, factory, vertexKeys, edgeKeys);

    Vertex v1 = graphson.vertexFromJson((JSONObject) JSONValue.parse(vertexJson1));
    Vertex v2 = graphson.vertexFromJson((JSONObject) JSONValue.parse(vertexJson2));
    Edge e = graphson.edgeFromJson(inputStreamEdgeJsonLight, v1, v2);

    Assert.assertSame(v1, g.getVertex(1));
    Assert.assertSame(v2, g.getVertex(2));
    Assert.assertSame(e, g.getEdge(0));
  }

  private class Cat {
    private String name;

    public Cat(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
