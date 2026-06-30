/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.test.tags;

/**
 * JUnit 4 category marker for tests that require a live Elasticsearch cluster (see EsTester).
 * Used via {@code @Category(ElasticsearchTest.class)} on the test class. The JUnit Vintage engine
 * exposes this category as the JUnit Platform tag "org.sonar.test.tags.ElasticsearchTest", which
 * the CI uses to route tests to the right job. JUnit 5 tests use {@code @Tag("elasticsearch")} instead.
 *
 * <p>To start Elasticsearch locally for these tests:
 * <pre>
 * docker run -d --name sonar-es-test -p 9200:9200 \
 *   -e "discovery.type=single-node" \
 *   -e "xpack.security.enabled=false" \
 *   -e "action.auto_create_index=false" \
 *   -e "action.destructive_requires_name=false" \
 *   -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
 *   docker.elastic.co/elasticsearch/elasticsearch:8.19.17
 * </pre>
 */
public interface ElasticsearchTest {
}
