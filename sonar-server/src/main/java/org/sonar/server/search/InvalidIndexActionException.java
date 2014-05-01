package org.sonar.server.search;

/**
 * Created by gamars on 01/05/14.
 *
 * @since 4.4
 */
public class InvalidIndexActionException extends Throwable {
  public InvalidIndexActionException(String message) {
    super(message);
  }
}
