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
export function checkIfDefault(qualityGate, list) {
  const finding = list.find(candidate => candidate.id === qualityGate.id);

  return finding ? finding.isDefault : false;
}

export function addCondition(qualityGate, metric) {
  const condition = {
    metric,
    op: 'LT',
    warning: '',
    error: ''
  };
  const oldConditions = qualityGate.conditions || [];
  const conditions = [...oldConditions, condition];

  return { ...qualityGate, conditions };
}

export function deleteCondition(qualityGate, condition) {
  const conditions = qualityGate.conditions.filter(candidate => candidate !== condition);

  return { ...qualityGate, conditions };
}

export function replaceCondition(qualityGate, oldCondition, newCondition) {
  const conditions = qualityGate.conditions.map(candidate => {
    return candidate === oldCondition ? newCondition : candidate;
  });
  return { ...qualityGate, conditions };
}
