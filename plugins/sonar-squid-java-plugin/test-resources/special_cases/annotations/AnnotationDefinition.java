package org.sonar.plugins.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Nice javadoc
 * with 2 lines
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationDefinition {
  String value() default "";
  
  /**
   * This is a javadoc annotation
   * @return
   */
  String value2() default "";
}
