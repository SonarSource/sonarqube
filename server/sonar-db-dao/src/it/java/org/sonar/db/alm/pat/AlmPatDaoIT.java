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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

class AlmPatDaoIT {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  private static final String A_NEW_PAT = "a new pat";
  private static final String AES_GCM_PREFIX = "{aes-gcm}";
  // passes Encryption.isEncrypted, which only tests the shape of the value, but needs no secret key to be read
  private static final String A_TOKEN_THAT_ONLY_LOOKS_ENCRYPTED = "{a}b";
  // passes it too, and would be Base64 decoded to "token" rather than left alone
  private static final String A_TOKEN_THAT_LOOKS_BASE64_ENCODED = "{b64}dG9rZW4=";
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  @TempDir
  private Path tempDir;

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final AlmSettingDao almSettingDao = new AlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  private final AlmPatDao underTest = newAlmPatDaoWithoutSecretKey();

  @Test
  void selectByUuid() {
    when(uuidFactory.create()).thenReturn(A_UUID);

    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto, null, null);

    assertThat(underTest.selectByUuid(dbSession, A_UUID).get())
      .extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken,
        AlmPatDto::getUserUuid, AlmPatDto::getAlmSettingUuid,
        AlmPatDto::getUpdatedAt, AlmPatDto::getCreatedAt)
      .containsExactly(A_UUID, almPatDto.getPersonalAccessToken(),
        almPatDto.getUserUuid(), almPatDto.getAlmSettingUuid(),
        NOW, NOW);

    assertThat(underTest.selectByUuid(dbSession, "foo")).isNotPresent();
  }

  @Test
  void selectByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);

    AlmSettingDto almSetting = newGithubAlmSettingDto();
    almSettingDao.insert(dbSession, almSetting);
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());

    String userUuid = secure().nextAlphanumeric(40);
    almPatDto.setUserUuid(userUuid);
    underTest.insert(dbSession, almPatDto, null, null);

    assertThat(underTest.selectByUserAndAlmSetting(dbSession, userUuid, almSetting).get())
      .extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken,
        AlmPatDto::getUserUuid, AlmPatDto::getAlmSettingUuid,
        AlmPatDto::getCreatedAt, AlmPatDto::getUpdatedAt)
      .containsExactly(A_UUID, almPatDto.getPersonalAccessToken(),
        userUuid, almSetting.getUuid(), NOW, NOW);

    assertThat(underTest.selectByUserAndAlmSetting(dbSession, secure().nextAlphanumeric(40), newGithubAlmSettingDto())).isNotPresent();
  }

  @Test
  void update() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto, null, null);

    String updated_pat = "updated pat";
    almPatDto.setPersonalAccessToken(updated_pat);

    system2.setNow(NOW + 1);
    underTest.update(dbSession, almPatDto, null, null);

    AlmPatDto result = underTest.selectByUuid(dbSession, A_UUID).get();
    assertThat(result)
      .extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken,
        AlmPatDto::getUserUuid, AlmPatDto::getAlmSettingUuid,
        AlmPatDto::getCreatedAt, AlmPatDto::getUpdatedAt)
      .containsExactly(A_UUID, updated_pat, almPatDto.getUserUuid(),
        almPatDto.getAlmSettingUuid(),
        NOW, NOW + 1);
  }

  @Test
  void delete() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDto almPat = newAlmPatDto();
    underTest.insert(dbSession, almPat, null, null);

    underTest.delete(dbSession, almPat, null, null);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid())).isNotPresent();
  }

  @Test
  void deleteByUser() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    UserDto userDto = db.users().insertUser();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setUserUuid(userDto.getUuid());
    underTest.insert(dbSession, almPat, userDto.getLogin(), null);

    underTest.deleteByUser(dbSession, userDto);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid())).isNotPresent();
  }

  @Test
  void deleteByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setAlmSettingUuid(almSettingDto.getUuid());
    underTest.insert(dbSession, almPat, null, null);

    underTest.deleteByAlmSetting(dbSession, almSettingDto);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid())).isNotPresent();
  }

  @Test
  void insert_whenNoSecretKeyIsConfigured_shouldStoreClearTextToken() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDto almPatDto = newAlmPatDto();

    underTest.insert(dbSession, almPatDto, null, null);

    assertThat(storedPersonalAccessToken()).isEqualTo(almPatDto.getPersonalAccessToken());
  }

  @Test
  void insert_whenSecretKeyIsConfigured_shouldEncryptTokenAndDecryptItOnRead() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDao daoWithSecretKey = newAlmPatDaoWithSecretKey();
    AlmSettingDto almSetting = newGithubAlmSettingDto();
    almSettingDao.insert(dbSession, almSetting);
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    String clearTextToken = almPatDto.getPersonalAccessToken();

    daoWithSecretKey.insert(dbSession, almPatDto, null, null);

    assertThat(storedPersonalAccessToken()).startsWith(AES_GCM_PREFIX).doesNotContain(clearTextToken);
    assertThat(almPatDto.getPersonalAccessToken()).isEqualTo(clearTextToken);
    assertThat(daoWithSecretKey.selectByUuid(dbSession, A_UUID))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(clearTextToken);
    assertThat(daoWithSecretKey.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(clearTextToken);
  }

  @Test
  void update_whenSecretKeyIsConfigured_shouldEncryptTokenAndDecryptItOnRead() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDao daoWithSecretKey = newAlmPatDaoWithSecretKey();
    AlmPatDto almPatDto = newAlmPatDto();
    daoWithSecretKey.insert(dbSession, almPatDto, null, null);
    String updatedToken = "updated pat";

    almPatDto.setPersonalAccessToken(updatedToken);
    daoWithSecretKey.update(dbSession, almPatDto, null, null);

    assertThat(storedPersonalAccessToken()).startsWith(AES_GCM_PREFIX).doesNotContain(updatedToken);
    assertThat(daoWithSecretKey.selectByUuid(dbSession, A_UUID))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(updatedToken);
  }

  @Test
  void selectByUuid_whenSecretKeyIsConfiguredAndTokenWasStoredAsClearText_shouldReturnItUnchanged() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto, null, null);

    assertThat(newAlmPatDaoWithSecretKey().selectByUuid(dbSession, A_UUID))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(almPatDto.getPersonalAccessToken());
  }

  @Test
  void selectByUuid_whenTokenCannotBeDecrypted_shouldReturnEmpty() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    newAlmPatDaoWithSecretKey("original-secret.txt").insert(dbSession, newAlmPatDto(), null, null);

    AlmPatDao daoWithAnotherSecretKey = newAlmPatDaoWithSecretKey("another-secret.txt");

    assertThat(daoWithAnotherSecretKey.selectByUuid(dbSession, A_UUID)).isEmpty();

    // this key is readable, it is simply not the one the token was encrypted with, so entering it again does replace it
    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("cannot be decrypted and is ignored")
        && log.contains("Entering the token again only replaces it once that key works"));
  }

  @Test
  void selectByUserAndAlmSetting_whenTokenCannotBeDecrypted_shouldReturnEmpty() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    newAlmPatDaoWithSecretKey("original-secret.txt").insert(dbSession, almPatDto, null, null);

    AlmPatDao daoWithAnotherSecretKey = newAlmPatDaoWithSecretKey("another-secret.txt");

    assertThat(daoWithAnotherSecretKey.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting)).isEmpty();
  }

  @Test
  void selectByUuid_whenNoSecretKeyIsConfiguredAndTokenIsEncrypted_shouldReturnEmpty() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    newAlmPatDaoWithSecretKey().insert(dbSession, newAlmPatDto(), null, null);

    assertThat(underTest.selectByUuid(dbSession, A_UUID)).isEmpty();

    // save() refuses to replace the token while the key is missing, so asking for it to be entered again is a dead end
    assertThat(logTester.logs(Level.WARN))
      .anyMatch(log -> log.contains("Configure the secret key that was used to encrypt it"))
      .noneMatch(log -> log.contains("Entering the token again"));
  }

  @Test
  void selectByUuid_whenStoredTokenOnlyLooksEncrypted_shouldReturnItUnchanged() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    underTest.insert(dbSession, newAlmPatDto().setPersonalAccessToken(A_TOKEN_THAT_ONLY_LOOKS_ENCRYPTED), null, null);

    assertThat(underTest.selectByUuid(dbSession, A_UUID))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(A_TOKEN_THAT_ONLY_LOOKS_ENCRYPTED);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void selectByUuid_whenStoredTokenLooksBase64Encoded_shouldReturnItUnchanged() {
    // decrypting this one would not fail, it would hand out a token nobody stored
    when(uuidFactory.create()).thenReturn(A_UUID);
    underTest.insert(dbSession, newAlmPatDto().setPersonalAccessToken(A_TOKEN_THAT_LOOKS_BASE64_ENCODED), null, null);

    assertThat(underTest.selectByUuid(dbSession, A_UUID))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(A_TOKEN_THAT_LOOKS_BASE64_ENCODED);
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  void selectByUserAndAlmSetting_whenNoSecretKeyIsConfiguredAndTokenIsEncrypted_shouldReturnEmpty() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    newAlmPatDaoWithSecretKey().insert(dbSession, almPatDto, null, null);

    assertThat(underTest.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting)).isEmpty();
  }

  @Test
  void save_whenNoTokenIsStoredForUserAndAlmSetting_shouldInsertIt() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());

    underTest.save(dbSession, almPatDto, null, null);

    assertThat(db.countRowsOfTable(dbSession, "alm_pats")).isOne();
    assertThat(underTest.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting))
      .get().extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken)
      .containsExactly(A_UUID, almPatDto.getPersonalAccessToken());
  }

  @Test
  void save_whenATokenIsAlreadyStoredForUserAndAlmSetting_shouldReplaceIt() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    underTest.save(dbSession, almPatDto, null, null);

    underTest.save(dbSession, newAlmPatDtoFor(almSetting, almPatDto.getUserUuid()), null, null);

    assertThat(db.countRowsOfTable(dbSession, "alm_pats")).isOne();
    assertThat(underTest.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting))
      .get().extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken)
      .containsExactly(A_UUID, A_NEW_PAT);
  }

  @Test
  void save_whenStoredTokenCannotBeDecrypted_shouldReplaceItInsteadOfFailing() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    newAlmPatDaoWithSecretKey("original-secret.txt").insert(dbSession, almPatDto, null, null);
    AlmPatDao daoWithAnotherSecretKey = newAlmPatDaoWithSecretKey("another-secret.txt");

    daoWithAnotherSecretKey.save(dbSession, newAlmPatDtoFor(almSetting, almPatDto.getUserUuid()), null, null);

    assertThat(db.countRowsOfTable(dbSession, "alm_pats")).isOne();
    assertThat(daoWithAnotherSecretKey.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting))
      .get().extracting(AlmPatDto::getPersonalAccessToken).isEqualTo(A_NEW_PAT);
  }

  @Test
  void save_whenNoSecretKeyIsConfiguredAndStoredTokenIsEncrypted_shouldFailRatherThanStoreItAsClearText() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    newAlmPatDaoWithSecretKey().insert(dbSession, almPatDto, null, null);
    String encryptedToken = storedPersonalAccessToken();
    AlmPatDto replacement = newAlmPatDtoFor(almSetting, almPatDto.getUserUuid());

    assertThatThrownBy(() -> underTest.save(dbSession, replacement, null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("no secret key is configured to encrypt its replacement with");

    assertThat(storedPersonalAccessToken()).isEqualTo(encryptedToken).startsWith(AES_GCM_PREFIX);
  }

  @Test
  void save_whenStoredTokenOnlyLooksEncryptedAndNoSecretKeyIsConfigured_shouldStillReplaceIt() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto()
      .setPersonalAccessToken(A_TOKEN_THAT_ONLY_LOOKS_ENCRYPTED)
      .setAlmSettingUuid(almSetting.getUuid());
    underTest.insert(dbSession, almPatDto, null, null);

    // nothing is encrypted, so there is no encryption to lose by replacing it
    underTest.save(dbSession, newAlmPatDtoFor(almSetting, almPatDto.getUserUuid()), null, null);

    assertThat(db.countRowsOfTable(dbSession, "alm_pats")).isOne();
    assertThat(storedPersonalAccessToken()).isEqualTo(A_NEW_PAT);
  }

  @Test
  void save_whenTheSecretKeyFileIsBlankAndStoredTokenIsEncrypted_shouldFailRatherThanStoreItAsClearText() throws IOException {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSetting = insertGithubAlmSetting();
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());
    newAlmPatDaoWithSecretKey().insert(dbSession, almPatDto, null, null);
    String encryptedToken = storedPersonalAccessToken();
    // a blank key file gets past hasSecretKey(), which only checks that the file exists
    AlmPatDao daoWithBlankSecretKey = newAlmPatDaoWithBlankSecretKey();
    AlmPatDto replacement = newAlmPatDtoFor(almSetting, almPatDto.getUserUuid());

    assertThat(daoWithBlankSecretKey.selectByUserAndAlmSetting(dbSession, almPatDto.getUserUuid(), almSetting)).isEmpty();
    assertThatThrownBy(() -> daoWithBlankSecretKey.save(dbSession, replacement, null, null))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No secret key in the file");

    assertThat(storedPersonalAccessToken()).isEqualTo(encryptedToken).startsWith(AES_GCM_PREFIX);
  }

  private AlmSettingDto insertGithubAlmSetting() {
    AlmSettingDto almSetting = newGithubAlmSettingDto();
    almSettingDao.insert(dbSession, almSetting);
    return almSetting;
  }

  private static AlmPatDto newAlmPatDtoFor(AlmSettingDto almSetting, String userUuid) {
    return new AlmPatDto()
      .setPersonalAccessToken(A_NEW_PAT)
      .setAlmSettingUuid(almSetting.getUuid())
      .setUserUuid(userUuid);
  }

  private AlmPatDao newAlmPatDaoWithoutSecretKey() {
    // an unset path falls back to ~/.sonar/sonar-secret.txt, which would make this
    // non-deterministic on machines that happen to have a key there
    return newAlmPatDao("target/no-such-sonar-secret.txt");
  }

  private AlmPatDao newAlmPatDaoWithSecretKey() throws IOException {
    return newAlmPatDaoWithSecretKey("sonar-secret.txt");
  }

  private AlmPatDao newAlmPatDaoWithSecretKey(String secretKeyFileName) throws IOException {
    Path secretKeyFile = tempDir.resolve(secretKeyFileName);
    Files.writeString(secretKeyFile, new Encryption(null).generateRandomSecretKey());
    return newAlmPatDao(secretKeyFile.toString());
  }

  private AlmPatDao newAlmPatDaoWithBlankSecretKey() throws IOException {
    Path secretKeyFile = tempDir.resolve("blank-secret.txt");
    Files.writeString(secretKeyFile, "");
    return newAlmPatDao(secretKeyFile.toString());
  }

  private AlmPatDao newAlmPatDao(String pathToSecretKey) {
    Settings settings = new MapSettings();
    settings.getEncryption().setPathToSecretKey(pathToSecretKey);
    return new AlmPatDao(system2, uuidFactory, new NoOpAuditPersister(), settings);
  }

  private String storedPersonalAccessToken() {
    return (String) db.selectFirst(dbSession, "select pat as \"pat\" from alm_pats").get("pat");
  }

}
