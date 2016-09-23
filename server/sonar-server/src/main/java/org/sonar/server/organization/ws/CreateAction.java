/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.organization.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations.CreateWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;
import static org.sonar.core.util.Slug.slugify;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements OrganizationsAction {
  private static final String ACTION = "create";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_KEY = "key";
  private static final String PARAM_DESCRIPTION = "description";
  private static final String PARAM_URL = "url";
  private static final String PARAM_AVATAR_URL = "avatar";
  private static final int NAME_MIN_LENGTH = 2;
  private static final int NAME_MAX_LENGTH = 64;
  private static final int KEY_MIN_LENGTH = 2;
  private static final int KEY_MAX_LENGTH = 32;

  private final UserSession userSession;
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system2;
  private final OrganizationsWsSupport wsSupport;

  public CreateAction(UserSession userSession, DbClient dbClient, UuidFactory uuidFactory, System2 system2, OrganizationsWsSupport wsSupport) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Create an organization.<br /> Requires 'Administer System' permission.")
      .setResponseExample(getClass().getResource("example-create.json"))
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setDescription("Name of the organization. <br />" +
        "It must be between 2 and 64 chars longs.")
      .setExampleValue("Foo Company");

    action.createParam(PARAM_KEY)
      .setRequired(false)
      .setDescription("Key of the organization. <br />" +
        "The key is unique to the whole SonarQube. <br/>" +
        "When not specified, the key is computed from the name. <br />" +
        "Otherwise, it must be between 2 and 32 chars long. All chars must be lower-case letters (a to z), digits or dash (but dash can neither be trailing nor heading)")
      .setExampleValue("foo-company");

    action.createParam(PARAM_DESCRIPTION)
      .setRequired(false)
      .setDescription("Description of the organization.<br/> It must be less than 256 chars long.")
      .setExampleValue("The Foo company produces quality software for Bar.");

    action.createParam(PARAM_URL)
      .setRequired(false)
      .setDescription("URL of the organization.<br/> It must be less than 256 chars long.")
      .setExampleValue("https://www.foo.com");

    action.createParam(PARAM_AVATAR_URL)
      .setRequired(false)
      .setDescription("URL of the organization avatar.<br/> It must be less than 256 chars long.")
      .setExampleValue("https://www.foo.com/foo.png");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);

    String name = getAndCheckName(request);
    String requestKey = getAndCheckKey(request);
    String key = useOrGenerateKey(requestKey, name);
    checkParamMaxLength(request, PARAM_DESCRIPTION, 256);
    checkParamMaxLength(request, PARAM_URL, 256);
    checkParamMaxLength(request, PARAM_AVATAR_URL, 256);

    try (DbSession dbSession = dbClient.openSession(false)) {
      checkKeyIsNotUsed(dbSession, key, requestKey, name);

      OrganizationDto dto = createOrganizationDto(request, name, key, system2.now());
      dbClient.organizationDao().insert(dbSession, dto);
      dbSession.commit();

      writeResponse(request, response, dto);
    }
  }

  private static void checkParamMaxLength(Request request, String key, int maxLength) {
    String value = request.param(key);
    if (value != null) {
      checkArgument(value.length() <= maxLength, "%s '%s' must be at most %s chars long", key, value, maxLength);
    }
  }

  private static String getAndCheckName(Request request) {
    String name = request.mandatoryParam(PARAM_NAME);
    checkArgument(name.length() >= NAME_MIN_LENGTH, "Name '%s' must be at least %s chars long", name, NAME_MIN_LENGTH);
    checkArgument(name.length() <= NAME_MAX_LENGTH, "Name '%s' must be at most %s chars long", name, NAME_MAX_LENGTH);
    return name;
  }

  @CheckForNull
  private static String getAndCheckKey(Request request) {
    String rqstKey = request.param(PARAM_KEY);
    if (rqstKey != null) {
      checkArgument(rqstKey.length() >= KEY_MIN_LENGTH, "Key '%s' must be at least %s chars long", rqstKey, KEY_MIN_LENGTH);
      checkArgument(rqstKey.length() <= KEY_MAX_LENGTH, "Key '%s' must be at most %s chars long", rqstKey, KEY_MAX_LENGTH);
      checkArgument(slugify(rqstKey).equals(rqstKey), "Key '%s' contains at least one invalid char", rqstKey);
    }
    return rqstKey;
  }

  private static String useOrGenerateKey(@Nullable String key, String name) {
    if (key == null) {
      return slugify(name.substring(0, min(name.length(), KEY_MAX_LENGTH)));
    }
    return key;
  }

  private void checkKeyIsNotUsed(DbSession dbSession, String key, @Nullable String requestKey, String name) {
    boolean isUsed = checkKeyIsUsed(dbSession, key);
    checkArgument(requestKey == null || !isUsed, "Key '%s' is already used. Specify another one.", key);
    checkArgument(requestKey != null || !isUsed, "Key '%s' generated from name '%s' is already used. Specify one.", key, name);
  }

  private boolean checkKeyIsUsed(DbSession dbSession, String key) {
    return dbClient.organizationDao().selectByKey(dbSession, key).isPresent();
  }

  private OrganizationDto createOrganizationDto(Request request, String name, String key, long now) {
    return new OrganizationDto()
      .setUuid(uuidFactory.create())
      .setName(name)
      .setKey(key)
      .setDescription(request.param(PARAM_DESCRIPTION))
      .setUrl(request.param(PARAM_URL))
      .setAvatarUrl(request.param(PARAM_AVATAR_URL))
      .setCreatedAt(now)
      .setUpdatedAt(now);
  }

  private void writeResponse(Request request, Response response, OrganizationDto dto) {
    writeProtobuf(
      CreateWsResponse.newBuilder().setOrganization(wsSupport.toOrganization(dto)).build(),
      request,
      response);
  }

}
