/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// Author: Christoffer Niska <christofferniska@gmail.com>
// https://gist.github.com/crisu83/42ecffccad9d04c74605fbc75c9dc9d1
import { uniq } from 'lodash';

/**
 * Creates a reducer that manages a single value.
 *
 * @param {function(state, action)} shouldUpdate
 * @param {function(state, action)} shouldReset
 * @param {function(state, action)} getValue
 * @param {*} defaultValue
 * @returns {function(state, action)}
 */
export const createValue = (
  shouldUpdate = () => true,
  shouldReset = () => false,
  getValue = (state, action) => action.payload,
  defaultValue = null
) => (state = defaultValue, action = {}) => {
  if (shouldReset(state, action)) {
    return defaultValue;
  }
  if (shouldUpdate(state, action)) {
    return getValue(state, action);
  }
  return state;
};

/**
 * Creates a reducer that manages a map.
 *
 * @param {function(state, action)} shouldUpdate
 * @param {function(state, action)} shouldReset
 * @param {function(state, action)} getValues
 * @returns {function(state, action)}
 */
export const createMap = (
  shouldUpdate = () => true,
  shouldReset = () => false,
  getValues = (state, action) => action.payload
) =>
  createValue(
    shouldUpdate,
    shouldReset,
    (state, action) => ({ ...state, ...getValues(state, action) }),
    {}
  );

/**
 * Creates a reducer that manages a set.
 *
 * @param {function(state, action)} shouldUpdate
 * @param {function(state, action)} shouldReset
 * @param {function(state, action)} getValues
 * @returns {function(state, action)}
 */
export const createSet = (
  shouldUpdate = () => true,
  shouldReset = () => false,
  getValues = (state, action) => action.payload
) =>
  createValue(
    shouldUpdate,
    shouldReset,
    (state, action) => uniq([...state, ...getValues(state, action)]),
    []
  );

/**
 * Creates a reducer that manages a flag.
 *
 * @param {function(state, action)} shouldTurnOn
 * @param {function(state, action)} shouldTurnOff
 * @param {bool} defaultValue
 * @returns {function(state, action)}
 */
export const createFlag = (shouldTurnOn, shouldTurnOff, defaultValue = false) => (
  state = defaultValue,
  action = {}
) => {
  if (shouldTurnOn(state, action)) {
    return true;
  }
  if (shouldTurnOff(state, action)) {
    return false;
  }
  return state;
};
