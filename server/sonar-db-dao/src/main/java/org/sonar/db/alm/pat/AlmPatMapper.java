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

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface AlmPatMapper {

  @CheckForNull
  AlmPatDto selectByUuid(@Param("uuid") String uuid);

  @CheckForNull
  AlmPatDto selectByUserAndAlmSetting(@Param("userUuid") String userUuid, @Param("almSettingUuid") String almSettingUuid);

  List<AlmPatDto> selectAll();

  /**
   * Counts the tokens that are stored as clear text, without reading any of them. Both cipher prefixes are tested
   * because {@code {aes-gcm}} does not start with {@code {aes}}, and neither is widened to an opening brace on its
   * own: that would take a clear text token which merely starts with one for an encrypted token, and never report
   * it. See {@link AlmPatDao#countNotEncryptedPersonalAccessTokens}.
   */
  int countNotEncrypted();

  void insert(@Param("dto") AlmPatDto almPatDto, @Param("pat") String pat);

  void update(@Param("dto") AlmPatDto almPatDto, @Param("pat") String pat);

  /**
   * Replaces the stored value of a token without touching any other column, used when only the storage format changes.
   * The replacement only happens while the row still holds {@code expectedPat}, so that a token stored meanwhile by
   * another node is not overwritten. Returns how many rows were updated, which is 0 when the value has changed.
   */
  int updatePat(@Param("uuid") String uuid, @Param("pat") String pat, @Param("expectedPat") String expectedPat);

  int deleteByUuid(@Param("uuid") String uuid);

  int deleteByUser(@Param("userUuid") String userUuid);

  int deleteByAlmSetting(@Param("almSettingUuid") String almSettingUuid);
}
