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
export function getNextRating(rating: number): number | undefined {
  return rating > 1 ? rating - 1 : undefined;
}

function getWorstSeverity(data: string): { severity: string; count: number } | undefined {
  const SEVERITY_ORDER = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'];

  const severities: { [key: string]: number } = {};
  data.split(';').forEach(equality => {
    const [key, count] = equality.split('=');
    severities[key] = Number(count);
  });

  for (let i = 0; i < SEVERITY_ORDER.length; i++) {
    const count = severities[SEVERITY_ORDER[i]];
    if (count > 0) {
      return { severity: SEVERITY_ORDER[i], count };
    }
  }

  return undefined;
}

export function getEffortToNextRating(
  measures: Array<{ metric: { key: string }; value: string }>,
  metricKey: string
) {
  const measure = measures.find(measure => measure.metric.key === metricKey);
  if (!measure) {
    return undefined;
  }
  return getWorstSeverity(measure.value);
}
