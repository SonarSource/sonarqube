/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
export const SET_STATE = 'SET_STATE';
export function setState (nextState) {
  return {
    type: SET_STATE,
    nextState
  };
}

export const ADD = 'ADD';
export function addQualityGate (qualityGate) {
  return {
    type: 'ADD',
    qualityGate
  };
}

export const DELETE = 'DELETE';
export function deleteQualityGate (qualityGate) {
  return {
    type: 'DELETE',
    qualityGate
  };
}

export const SHOW = 'SHOW';
export function showQualityGate (qualityGate) {
  return {
    type: 'SHOW',
    qualityGate
  };
}

export const RENAME = 'RENAME';
export function renameQualityGate (qualityGate, newName) {
  return {
    type: 'RENAME',
    qualityGate,
    newName
  };
}

export const COPY = 'COPY';
export function copyQualityGate (qualityGate) {
  return {
    type: COPY,
    qualityGate
  };
}

export const SET_AS_DEFAULT = 'SET_AS_DEFAULT';
export function setQualityGateAsDefault (qualityGate) {
  return {
    type: SET_AS_DEFAULT,
    qualityGate
  };
}

export const UNSET_AS_DEFAULT = 'UNSET_AS_DEFAULT';
export function unsetQualityGateAsDefault (qualityGate) {
  return {
    type: UNSET_AS_DEFAULT,
    qualityGate
  };
}

export const ADD_CONDITION = 'ADD_CONDITION';
export function addCondition (metric) {
  return {
    type: ADD_CONDITION,
    metric
  };
}

export const SAVE_CONDITION = 'SAVE_CONDITION';
export function saveCondition (oldCondition, newCondition) {
  return {
    type: SAVE_CONDITION,
    oldCondition,
    newCondition
  };
}

export const DELETE_CONDITION = 'DELETE_CONDITION';
export function deleteCondition (condition) {
  return {
    type: DELETE_CONDITION,
    condition
  };
}
