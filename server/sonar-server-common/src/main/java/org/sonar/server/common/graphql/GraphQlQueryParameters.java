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
package org.sonar.server.common.graphql;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public interface GraphQlQueryParameters {

  record QueryWithPagination<T, U>(String appUrl,
                                   String accessToken,
                                   String queryString,
                                   Map<String, String> queryVariables,
                                   Function<GsonGraphQlAnswer<T>, List<U>> extractAndMapResultsFunction,
                                   Function<GsonGraphQlAnswer<T>, String> extractCursorFunction,
                                   Predicate<GsonGraphQlAnswer<T>> hasNextPage, Type answerDataType) {}
}
