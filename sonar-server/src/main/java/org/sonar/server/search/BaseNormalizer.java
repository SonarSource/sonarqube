package org.sonar.server.search;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.db.Dto;

import java.io.IOException;
import java.io.Serializable;

public abstract class BaseNormalizer<E extends Dto<K>, K extends Serializable> {

  public abstract XContentBuilder normalize(K key) throws IOException;

  public abstract XContentBuilder normalize(E dto) throws IOException;

  private static final Logger LOG = LoggerFactory.getLogger(BaseNormalizer.class);

  protected void indexField(String field, Object value, XContentBuilder document){
    try {
      document.field(field,value);
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
