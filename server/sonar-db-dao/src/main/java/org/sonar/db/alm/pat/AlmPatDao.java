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
package org.sonar.db.alm.pat;

import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PersonalAccessTokenNewValue;
import org.sonar.db.user.UserDto;

import static org.sonar.api.CoreProperties.ENCRYPTION_SECRET_KEY_PATH;

/**
 * Personal access tokens are stored encrypted whenever an encryption secret key is configured, and decrypted on read.
 * Tokens written by an instance without a secret key, or before encryption was introduced, are stored as clear text
 * and read back unchanged.
 * <p>
 * A token that cannot be decrypted, typically because the secret key was lost or replaced, is reported as absent
 * instead of failing the read. Callers then behave as if the user never entered a token, which prompts them to enter
 * it again. Replacing a token that is stored encrypted is refused while no secret key is configured, so that entering
 * it again cannot turn an encrypted token into a clear text one.
 */
public class AlmPatDao implements Dao {

  private static final Logger LOG = LoggerFactory.getLogger(AlmPatDao.class);
  private static final String AES_GCM_PREFIX = "{aes-gcm}";
  private static final String AES_ECB_PREFIX = "{aes}";

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;
  private final Encryption encryption;

  public AlmPatDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister, Settings settings) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
    this.encryption = settings.getEncryption();
  }

  private static AlmPatMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmPatMapper.class);
  }

  public Optional<AlmPatDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(getMapper(dbSession).selectByUuid(uuid)).flatMap(this::decryptPersonalAccessToken);
  }

  public Optional<AlmPatDto> selectByUserAndAlmSetting(DbSession dbSession, String userUuid, AlmSettingDto almSettingDto) {
    return Optional.ofNullable(getMapper(dbSession).selectByUserAndAlmSetting(userUuid, almSettingDto.getUuid()))
      .flatMap(this::decryptPersonalAccessToken);
  }

  /**
   * Stores the token of a user for a DevOps platform configuration, replacing the token already stored for that pair.
   * The stored row is looked up without decrypting it, so that a token which can no longer be decrypted is replaced
   * rather than inserted a second time, which {@code UNIQ_ALM_PATS} forbids.
   *
   * @throws IllegalStateException if the stored token is encrypted while no secret key is configured to encrypt its
   *                               replacement with
   */
  public void save(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    AlmPatDto storedAlmPatDto = getMapper(dbSession).selectByUserAndAlmSetting(almPatDto.getUserUuid(), almPatDto.getAlmSettingUuid());
    if (storedAlmPatDto == null) {
      insert(dbSession, almPatDto, userLogin, almSettingKey);
      return;
    }
    failIfItWouldReplaceAnEncryptedTokenWithClearText(storedAlmPatDto);
    almPatDto.setUuid(storedAlmPatDto.getUuid());
    update(dbSession, almPatDto, userLogin, almSettingKey);
  }

  public void insert(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    almPatDto.setUuid(uuid);
    almPatDto.setCreatedAt(now);
    almPatDto.setUpdatedAt(now);
    getMapper(dbSession).insert(almPatDto, toStoredPersonalAccessToken(almPatDto));

    auditPersister.addPersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almPatDto, userLogin, almSettingKey));
  }

  public void update(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    long now = system2.now();
    almPatDto.setUpdatedAt(now);
    getMapper(dbSession).update(almPatDto, toStoredPersonalAccessToken(almPatDto));
    auditPersister.updatePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almPatDto, userLogin, almSettingKey));
  }

  public void delete(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    int deletedRows = getMapper(dbSession).deleteByUuid(almPatDto.getUuid());
    if (deletedRows > 0) {
      auditPersister.deletePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almPatDto, userLogin, almSettingKey));
    }
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    int deletedRows = getMapper(dbSession).deleteByUser(user.getUuid());
    if (deletedRows > 0) {
      auditPersister.deletePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(user));
    }
  }

  public void deleteByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    int deletedRows = getMapper(dbSession).deleteByAlmSetting(almSetting.getUuid());
    if (deletedRows > 0) {
      auditPersister.deletePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almSetting));
    }
  }

  /**
   * A token is only ever stored encrypted while a secret key is configured, so a key that is gone is a
   * misconfiguration. Overwriting the token would store its replacement as clear text, which loses the encryption the
   * instance was set up with and cannot be undone by putting the key back.
   */
  private void failIfItWouldReplaceAnEncryptedTokenWithClearText(AlmPatDto storedAlmPatDto) {
    if (needsSecretKeyToBeRead(storedAlmPatDto.getPersonalAccessToken()) && !encryption.hasSecretKey()) {
      throw new IllegalStateException("The personal access token of alm_pats entry '" + storedAlmPatDto.getUuid()
        + "' is stored encrypted, and no secret key is configured to encrypt its replacement with");
    }
  }

  /**
   * Whether reading the stored value back needs the secret key, which is true of the ciphers this class encrypts with
   * and of nothing else. {@link Encryption#isEncrypted(String)} cannot answer that, since it only tests the shape of
   * the value: a clear text token such as {@code {a}b} passes it, and one starting with {@code {b64}} would be run
   * through a cipher that decodes anything without ever failing, which turns the token into unusable bytes instead of
   * reporting it as unreadable.
   */
  private static boolean needsSecretKeyToBeRead(String storedPersonalAccessToken) {
    String lowerCase = storedPersonalAccessToken.toLowerCase(Locale.ENGLISH);
    return lowerCase.startsWith(AES_GCM_PREFIX) || lowerCase.startsWith(AES_ECB_PREFIX);
  }

  /**
   * Returned separately from the DTO so that the caller keeps holding the clear text token.
   */
  private String toStoredPersonalAccessToken(AlmPatDto almPatDto) {
    String personalAccessToken = almPatDto.getPersonalAccessToken();
    return encryption.hasSecretKey() ? encryption.encrypt(personalAccessToken) : personalAccessToken;
  }

  private Optional<AlmPatDto> decryptPersonalAccessToken(AlmPatDto almPatDto) {
    String storedPersonalAccessToken = almPatDto.getPersonalAccessToken();
    if (!needsSecretKeyToBeRead(storedPersonalAccessToken)) {
      return Optional.of(almPatDto);
    }
    // entering the token again is refused while the key is missing, so the key is what has to be put back
    if (!encryption.hasSecretKey()) {
      LOG.warn("The personal access token of alm_pats entry '{}' is stored encrypted and no secret key is configured, "
        + "so it is ignored. Configure the secret key that was used to encrypt it to make it readable again.",
        almPatDto.getUuid());
      return Optional.empty();
    }
    try {
      almPatDto.setPersonalAccessToken(encryption.decrypt(storedPersonalAccessToken));
      return Optional.of(almPatDto);
    } catch (RuntimeException e) {
      LOG.warn("The personal access token of alm_pats entry '{}' cannot be decrypted and is ignored. Check that the "
        + "secret key configured in '{}' can be read and is the one the token was encrypted with. Entering the token "
        + "again only replaces it once that key works.", almPatDto.getUuid(), ENCRYPTION_SECRET_KEY_PATH, e);
      return Optional.empty();
    }
  }
}
