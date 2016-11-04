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
import { getMetrics } from '../../../api/metrics';

/*
 * Actions
 */

export const DISPLAY_HOME = 'measuresApp/app/DISPLAY_HOME';
export const RECEIVE_METRICS = 'measuresApp/app/RECEIVE_METRICS';
export const SET_COMPONENT = 'measuresApp/app/SET_COMPONENT';

/*
 * Action Creators
 */

export function displayHome () {
  return { type: DISPLAY_HOME };
}

function receiveMetrics (metrics) {
  return { type: RECEIVE_METRICS, metrics };
}

export function setComponent (component) {
  return { type: SET_COMPONENT, component };
}

/*
 * Workflow
 */

export function fetchMetrics () {
  return dispatch => {
    getMetrics().then(metrics => {
      dispatch(receiveMetrics(metrics));
    });
  };
}
