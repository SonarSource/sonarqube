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
package org.sonar.server.search;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.db.Dto;

import java.io.IOException;
import java.io.Serializable;

public abstract class BaseNormalizer<E extends Dto<K>, K extends Serializable> {

  public boolean canNormalize(Class<?> objectClass, Class<?> keyClass) {
    try {
      return this.getClass().getMethod("normalize", objectClass, keyClass) != null;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  public UpdateRequest normalizeOther(Object object, K key) throws Exception {
    return (UpdateRequest) this.getClass()
      .getMethod("normalize", object.getClass(), key.getClass())
      .invoke(this, object, key);
  }

  public abstract UpdateRequest normalize(K key) throws IOException;

  public abstract UpdateRequest normalize(E dto) throws IOException;

  private static final Logger LOG = LoggerFactory.getLogger(BaseNormalizer.class);

  protected void indexField(String field, Object value, XContentBuilder document) {
    try {
      document.field(field, value);
    } catch (IOException e) {
      LOG.error("Could not set {} to {} in ESDocument", field, value);
    }
  }


//  protected void indexField(Fields field, Object dto, XContentBuilder document) {
//    try {
//      document.field(field.key(), field.method.invoke(dto));
//    } catch (IllegalArgumentException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (IllegalAccessException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (InvocationTargetException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//  }
//
//
//
//private static Method getReadMethod(String method){
//  try {
//    return RuleDto.class.getDeclaredMethod(method);
//  } catch (SecurityException e) {
//    // TODO Auto-generated catch block
//    e.printStackTrace();
//  } catch (NoSuchMethodException e) {
//    // TODO Auto-generated catch block
//    e.printStackTrace();
//  }
//  return null;
//}


}
