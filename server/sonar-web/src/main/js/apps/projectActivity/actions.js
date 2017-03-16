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
// @flow
import * as api from '../../api/projectActivity';
import {
  receiveProjectActivity,
  addEvent,
  deleteEvent as deleteEventAction,
  changeEvent as changeEventAction,
  deleteAnalysis as deleteAnalysisAction,
  getPaging
} from '../../store/projectActivity/duck';
import { onFail } from '../../store/rootActions';
import { getProjectActivity } from '../../store/rootReducer';

const rejectOnFail = (dispatch: Function) =>
  (error: Object) => {
    onFail(dispatch)(error);
    return Promise.reject();
  };

export const fetchProjectActivity = (project: string, filter: ?string) =>
  (dispatch: Function): void => {
    api
      .getProjectActivity(project, { category: filter })
      .then(
        ({ analyses, paging }) => dispatch(receiveProjectActivity(project, analyses, paging)),
        onFail(dispatch)
      );
  };

export const fetchMoreProjectActivity = (project: string, filter: ?string) =>
  (dispatch: Function, getState: Function): void => {
    const projectActivity = getProjectActivity(getState());
    const { pageIndex } = getPaging(projectActivity, project);

    api
      .getProjectActivity(project, { category: filter, pageIndex: pageIndex + 1 })
      .then(
        ({ analyses, paging }) => dispatch(receiveProjectActivity(project, analyses, paging)),
        onFail(dispatch)
      );
  };

export const addCustomEvent = (analysis: string, name: string, category?: string) =>
  (dispatch: Function): Promise<*> => {
    return api
      .createEvent(analysis, name, category)
      .then(
        ({ analysis, ...event }) => dispatch(addEvent(analysis, event)),
        rejectOnFail(dispatch)
      );
  };

export const deleteEvent = (analysis: string, event: string) =>
  (dispatch: Function): Promise<*> => {
    return api
      .deleteEvent(event)
      .then(() => dispatch(deleteEventAction(analysis, event)), rejectOnFail(dispatch));
  };

export const addVersion = (analysis: string, version: string) =>
  (dispatch: Function): Promise<*> => {
    return dispatch(addCustomEvent(analysis, version, 'VERSION'));
  };

export const changeEvent = (event: string, name: string) =>
  (dispatch: Function): Promise<*> => {
    return api
      .changeEvent(event, name)
      .then(() => dispatch(changeEventAction(event, { name })), rejectOnFail(dispatch));
  };

export const deleteAnalysis = (project: string, analysis: string) =>
  (dispatch: Function): Promise<*> => {
    return api
      .deleteAnalysis(analysis)
      .then(() => dispatch(deleteAnalysisAction(project, analysis)), rejectOnFail(dispatch));
  };
