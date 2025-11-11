/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.es.textsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.util.Set;
import org.junit.Test;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.createQueryV2;
import static org.sonar.test.JsonAssert.assertJson;

public class ComponentTextSearchQueryFactoryTest {


  @Test
  public void createQuery_whenComponentTextSearchFeature_isKey() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.KEY);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool" : {
          "must" : [{
            "bool" : {
              "should" : [{
                "match" : {
                  "key.sortable_analyzer" : {
                    "query" : "SonarQube",
                    "boost" : 50.0
                  }
                }
              }]
            }
          }]
        }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeature_isPartial() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.PARTIAL);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "bool": {
                      "boost": 0.5,
                      "must": [
                        {
                          "match": {
                            "name.search_grams_analyzer": {
                              "query": "SonarQube"
                            }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeature_isPrefixIgnoreCase() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.PREFIX_IGNORE_CASE);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "bool": {
                      "boost": 2.0,
                          "must": [
                            {
                              "match": {
                                "name.search_prefix_case_insensitive_analyzer": {
                                  "query": "sonarqube"
                            }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeature_isKeyAndExactIgnoreCase() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.KEY, ComponentTextSearchFeatureRepertoire.EXACT_IGNORE_CASE);

    assertJson(toJson(result)).isSimilarTo("""
      {
       "bool": {
         "must": [
           {
             "bool": {
               "should": [
                 {
                   "match": {
                     "key.sortable_analyzer": {
                       "boost": 50.0,
                       "query": "SonarQube"
                     }
                   }
                 }
               ]
             }
           }
         ],
         "should": [
           {
             "bool": {
               "should": [
                 {
                   "match": {
                     "name.sortable_analyzer": {
                       "boost": 2.5,
                       "query": "SonarQube"
                     }
                   }
                 }
               ]
             }
           }
         ]
       }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeature_isKeyAndPrefix() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.KEY, ComponentTextSearchFeatureRepertoire.PREFIX);

    assertJson(toJson(result)).isSimilarTo("""
      {
       "bool": {
         "must": [
           {
             "bool": {
               "should": [
                 {
                   "match": {
                     "key.sortable_analyzer": {
                       "boost": 50.0,
                       "query": "SonarQube"
                     }
                   }
                 }
               ]
             }
           }
         ],
         "should": [
           {
             "bool": {
               "should": [
                 {
                   "bool": {
                     "boost": 3.0,
                     "must": [
                       {
                         "match": {
                           "name.search_prefix_analyzer": {
                             "query": "SonarQube"
                           }
                         }
                       }
                     ]
                   }
                 }
               ]
             }
           }
         ]
       }
     }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeature_isKeyAndRecentlyBrowsed() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").setRecentlyBrowsedKeys(Set.of("key1", "key2")).build(),
      ComponentTextSearchFeatureRepertoire.KEY, ComponentTextSearchFeatureRepertoire.RECENTLY_BROWSED);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "key.sortable_analyzer": {
                        "boost": 50.0,
                        "query": "SonarQube"
                      }
                    }
                  }
                ]
              }
            }
          ],
          "should": [
            {
              "bool": {
                "should": [
                  {
                    "terms": {
                      "key": [
                        "key2",
                        "key1"
                      ],
                      "boost": 100.0
                    }
                  }
                ]
              }
            }
          ]
        }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeatureHasNoRecentlyBrowser_isKeyAndRecentlyBrowsed() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.KEY, ComponentTextSearchFeatureRepertoire.RECENTLY_BROWSED);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "key.sortable_analyzer": {
                        "boost": 50.0,
                        "query": "SonarQube"
                      }
                    }
                  }
                ]
              }
            }
          ],
        }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeature_isKeyAndFavorite() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").setFavoriteKeys(Set.of("key1", "key2")).build(),
      ComponentTextSearchFeatureRepertoire.KEY, ComponentTextSearchFeatureRepertoire.FAVORITE);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "key.sortable_analyzer": {
                        "boost": 50.0,
                        "query": "SonarQube"
                      }
                    }
                  }
                ]
              }
            }
          ],
          "should": [
            {
              "bool": {
                "should": [
                  {
                    "terms": {
                      "key": [
                        "key2",
                        "key1"
                      ],
                      "boost": 1000.0
                    }
                  }
                ]
              }
            }
          ]
        }
      }""");
  }

  @Test
  public void createQuery_whenComponentTextSearchFeatureHasNoFavoriteKeys_isKeyAndFavorite() {
    Query result = createQueryV2(ComponentTextSearchQuery.builder()
        .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build(),
      ComponentTextSearchFeatureRepertoire.KEY, ComponentTextSearchFeatureRepertoire.FAVORITE);

    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "key.sortable_analyzer": {
                        "boost": 50.0,
                        "query": "SonarQube"
                      }
                    }
                  }
                ]
              }
            }
          ],
        }
      }""");
  }

  @Test
  public void fail_to_create_query_when_no_feature() {
    var componentTextSearchQuery = ComponentTextSearchQuery.builder()
      .setQueryText("SonarQube").setFieldKey("key").setFieldName("name").build();
    assertThatThrownBy(() -> createQueryV2(componentTextSearchQuery))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("features cannot be empty");
  }

  @Test
  public void fail_to_create_query_when_no_query_text() {
    assertThatThrownBy(() -> ComponentTextSearchQuery.builder().setFieldKey("key").setFieldName("name").build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("query text cannot be null");
  }

  @Test
  public void fail_to_create_query_when_no_field_key() {
    assertThatThrownBy(() -> ComponentTextSearchQuery.builder().setQueryText("SonarQube").setFieldName("name").build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("field key cannot be null");
  }

  @Test
  public void fail_to_create_query_when_no_field_name() {
    assertThatThrownBy(() -> ComponentTextSearchQuery.builder().setQueryText("SonarQube").setFieldKey("key").build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("field name cannot be null");
  }

  @Test
  public void createQuery_whenQueryTextIsEmpty_withGenerateResultsFeature_shouldFailEarly() {
    // Empty string produces no tokens
    var query = ComponentTextSearchQuery.builder()
      .setQueryText("")
      .setFieldKey("key")
      .setFieldName("name")
      .build();

    // When using GENERATE_RESULTS features (PREFIX, PREFIX_IGNORE_CASE, PARTIAL),
    // they return Stream.empty() when tokens are empty, causing the factory to fail
    // with IllegalStateException BEFORE the tokens.isEmpty() check could return Stream.empty()
    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PREFIX))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PREFIX]");

    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PREFIX_IGNORE_CASE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PREFIX_IGNORE_CASE]");

    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PARTIAL))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PARTIAL]");
  }

  @Test
  public void createQuery_whenQueryTextIsOnlyShortTokens_withGenerateResultsFeature_shouldFailEarly() {
    // Query with only single-character tokens (below MINIMUM_NGRAM_LENGTH of 2)
    var query = ComponentTextSearchQuery.builder()
      .setQueryText("a b c")
      .setFieldKey("key")
      .setFieldName("name")
      .build();

    // Same behavior as empty string - tokens.isEmpty() is true but we fail earlier
    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PREFIX))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PREFIX]");

    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PREFIX_IGNORE_CASE))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PREFIX_IGNORE_CASE]");

    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PARTIAL))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PARTIAL]");
  }

  @Test
  public void createQuery_whenQueryTextIsOnlyWhitespace_withGenerateResultsFeature_shouldFailEarly() {
    // Query with only whitespace produces no tokens
    var query = ComponentTextSearchQuery.builder()
      .setQueryText("   ")
      .setFieldKey("key")
      .setFieldName("name")
      .build();

    // Same behavior - tokens.isEmpty() is true but we fail earlier
    assertThatThrownBy(() -> createQueryV2(query, ComponentTextSearchFeatureRepertoire.PREFIX))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No text search features found to generate search results. Features: [PREFIX]");
  }

  @Test
  public void createQuery_whenQueryTextIsEmpty_withChangeOrderFeature_tokensEmptyCheckIsRedundant() {
    // Empty string produces no tokens
    var query = ComponentTextSearchQuery.builder()
      .setQueryText("")
      .setFieldKey("key")
      .setFieldName("name")
      .build();

    // With CHANGE_ORDER_OF_RESULTS features combined with a GENERATE_RESULTS feature,
    // the CHANGE_ORDER features that check tokens.isEmpty() just return Stream.empty(),
    // which is redundant because they would return Stream.empty() anyway when used alone
    // This proves the tokens.isEmpty() check is defensive but unnecessary
    Query result = createQueryV2(query,
      ComponentTextSearchFeatureRepertoire.KEY,  // GENERATE_RESULTS - doesn't check tokens
      ComponentTextSearchFeatureRepertoire.PREFIX);  // CHANGE_ORDER - checks tokens.isEmpty()

    // The query succeeds because KEY feature doesn't depend on tokens
    // PREFIX returns Stream.empty() due to tokens.isEmpty() check, but that's redundant -
    // it would return Stream.empty() naturally when there are no tokens to process
    assertJson(toJson(result)).isSimilarTo("""
      {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "key.sortable_analyzer": {
                        "boost": 50.0,
                        "query": ""
                      }
                    }
                  }
                ]
              }
            }
          ]
        }
      }""");
  }

  private static String toJson(JsonpSerializable serializable) {
    StringWriter writer = new StringWriter();
    JsonProvider provider = JsonProvider.provider();
    JsonGenerator generator = provider.createGenerator(writer);
    JacksonJsonpMapper mapper = new JacksonJsonpMapper();
    serializable.serialize(generator, mapper);
    generator.close();
    return writer.toString();
  }

}
