/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
export const RECEIVE_COMPONENT_MEASURE = 'RECEIVE_COMPONENT_MEASURE';

export const receiveComponentMeasure = (componentKey, metricKey, value) => ({
  type: RECEIVE_COMPONENT_MEASURE,
  componentKey,
  metricKey,
  value
});

export const RECEIVE_COMPONENT_MEASURES = 'RECEIVE_COMPONENT_MEASURES';

export const receiveComponentMeasures = (componentKey, measures) => ({
  type: RECEIVE_COMPONENT_MEASURES,
  componentKey,
  measures
});

export const RECEIVE_COMPONENTS_MEASURES = 'RECEIVE_COMPONENTS_MEASURES';

export const receiveComponentsMeasures = componentsWithMeasures => ({
  type: RECEIVE_COMPONENTS_MEASURES,
  componentsWithMeasures
});
