/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.es;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public final class EsQueueDto {

  private String uuid;
  private String docType;
  private String docId;
  private String docIdType;
  private String docRouting;

  public String getUuid() {
    return uuid;
  }

  EsQueueDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getDocType() {
    return docType;
  }

  private EsQueueDto setDocType(String t) {
    this.docType = t;
    return this;
  }

  public String getDocId() {
    return docId;
  }

  private EsQueueDto setDocId(String s) {
    this.docId = s;
    return this;
  }

  @CheckForNull
  public String getDocIdType() {
    return docIdType;
  }

  private EsQueueDto setDocIdType(@Nullable String s) {
    this.docIdType = s;
    return this;
  }

  @CheckForNull
  public String getDocRouting() {
    return docRouting;
  }

  private EsQueueDto setDocRouting(@Nullable String s) {
    this.docRouting = s;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("EsQueueDto{");
    sb.append("uuid='").append(uuid).append('\'');
    sb.append(", docType=").append(docType);
    sb.append(", docId='").append(docId).append('\'');
    sb.append(", docIdType='").append(docIdType).append('\'');
    sb.append(", docRouting='").append(docRouting).append('\'');
    sb.append('}');
    return sb.toString();
  }

  public static EsQueueDto create(String docType, String docUuid) {
    return new EsQueueDto().setDocType(docType).setDocId(docUuid);
  }

  public static EsQueueDto create(String docType, String docId, @Nullable String docIdType, @Nullable String docRouting) {
    return new EsQueueDto().setDocType(docType)
      .setDocId(docId).setDocIdType(docIdType).setDocRouting(docRouting);
  }
}
