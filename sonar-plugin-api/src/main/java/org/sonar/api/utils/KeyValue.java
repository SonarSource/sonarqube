/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.utils;

/**
 * A utility class to store a key / value couple of generic types
 *
 * @since 1.10
 */
public class KeyValue<K, V> {

  private K key;
  private V value;

  /**
   * Creates a key / value object
   */
  public KeyValue(K key, V value) {
    super();
    this.key = key;
    this.value = value;
  }

  /**
   * @return the key of the couple
   */
  public K getKey() {
    return key;
  }

  /**
   * Sets the key of the couple
   *
   * @param key the key
   */
  public void setKey(K key) {
    this.key = key;
  }

  /**
   *
   * @return the value of the couple
   */
  public V getValue() {
    return value;
  }

  /**
   * Sets the value of the couple
   */
  public void setValue(V value) {
    this.value = value;
  }

}
