/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.exceptions.BadRequestException;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static org.junit.Assert.*;

public class SearchDataLoaderTest {

  @Rule
  public ExpectedException thrown= ExpectedException.none();

  @Test
  public void name_and_default_query_is_valid() throws Exception {
    SearchWsRequest request = new SearchWsRequest()
            .setProfileName("bla")
            .setDefaults(true);

    SearchDataLoader.validateRequest(request);
  }

  @Test
  public void name_and_component_query_is_valid() throws Exception {
    SearchWsRequest request = new SearchWsRequest()
            .setProfileName("bla")
            .setProjectKey("blubb");

    SearchDataLoader.validateRequest(request);
  }

  @Test
  public void name_requires_either_component_or_defaults() throws Exception {
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The name parameter requires either projectKey or defaults to be set.");

    SearchWsRequest request = new SearchWsRequest()
            .setProfileName("bla");

    SearchDataLoader.validateRequest(request);
  }

  @Test
  public void default_and_component_cannot_be_set_at_same_time() throws Exception {
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The default parameter cannot be provided at the same time than the component key.");

    SearchWsRequest request = new SearchWsRequest()
            .setDefaults(true)
            .setProjectKey("blubb");

    SearchDataLoader.validateRequest(request);
  }

  @Test
  public void language_and_component_cannot_be_set_at_same_time() throws Exception {
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The language parameter cannot be provided at the same time than the component key or profile name.");

    SearchWsRequest request = new SearchWsRequest()
            .setLanguage("xoo")
            .setProjectKey("bla");

    SearchDataLoader.validateRequest(request);
  }

  @Test
  public void language_and_name_cannot_be_set_at_same_time() throws Exception {
    thrown.expect(BadRequestException.class);
    thrown.expectMessage("The language parameter cannot be provided at the same time than the component key or profile name.");

    SearchWsRequest request = new SearchWsRequest()
            .setLanguage("xoo")
            .setProfileName("bla");

    SearchDataLoader.validateRequest(request);
  }

}