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
import { getJSON } from '../helpers/request.js';


export function getMeasures (componentKey, metrics) {
  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(',') };
  return getJSON(url, data).then(r => {
    const msr = r[0].msr || [];
    const measures = {};
    msr.forEach(measure => {
      measures[measure.key] = measure.val || measure.data;
    });
    return measures;
  });
}


export function getMeasuresAndVariations (componentKey, metrics) {
  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(','), includetrends: 'true' };
  return getJSON(url, data).then(r => {
    const msr = r[0].msr || [];
    const measures = {};
    msr.forEach(measure => {
      measures[measure.key] = {
        value: measure.val != null ? measure.val : measure.data,
        var1: measure.var1,
        var2: measure.var2,
        var3: measure.var3
      };
    });
    return measures;
  });
}
