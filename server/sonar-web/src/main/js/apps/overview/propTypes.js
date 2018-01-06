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
import { shape, arrayOf, array, string, number, object } from 'prop-types';

export const ComponentType = shape({
  id: string.isRequired
});

export const MetricType = shape({
  key: string.isRequired,
  name: string.isRequired,
  type: string.isRequired
});

export const MeasureType = shape({
  metric: MetricType.isRequired,
  value: string,
  periods: array
});

export const MeasuresListType = arrayOf(MeasureType);

export const ConditionType = shape({
  metric: string.isRequired
});

export const EnhancedConditionType = shape({
  measure: MeasureType.isRequired
});

export const ConditionsListType = arrayOf(ConditionType);

export const EnhancedConditionsListType = arrayOf(EnhancedConditionType);

export const PeriodType = shape({
  index: number.isRequired,
  date: string.isRequired,
  mode: string.isRequired,
  parameter: string
});

export const PeriodsListType = arrayOf(PeriodType);

export const EventType = shape({
  id: string.isRequired,
  date: object.isRequired,
  type: string.isRequired,
  name: string.isRequired,
  text: string
});
