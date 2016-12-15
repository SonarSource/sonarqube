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
// @flow
import * as api from '../../api/projectActivity';
import {
  receiveProjectActivity,
  changeProjectActivityFilter,
  addEvent,
  deleteEvent,
  getPaging,
  getFilter
} from '../../store/projectActivity/duck';
import { onFail } from '../../store/rootActions';
import { getProjectActivity } from '../../store/rootReducer';

const rejectOnFail = (dispatch: Function) => (error: any) => {
  onFail(dispatch)(error);
  return Promise.reject();
};

export const fetchProjectActivity = (project: string) => (dispatch: Function, getState: Function): void => {
  const state = getState();
  const filter = getFilter(getProjectActivity(state), project);

  api.getProjectActivity(project, { category: filter }).then(
      ({ analyses, paging }) => dispatch(receiveProjectActivity(project, analyses, paging)),
      onFail(dispatch)
  );
};

export const fetchMoreProjectActivity = (project: string) => (dispatch: Function, getState: Function): void => {
  const projectActivity = getProjectActivity(getState());
  const filter = getFilter(projectActivity, project);
  const { pageIndex } = getPaging(projectActivity, project);

  api.getProjectActivity(project, { category: filter, pageIndex: pageIndex + 1 }).then(
      ({ analyses, paging }) => dispatch(receiveProjectActivity(project, analyses, paging)),
      onFail(dispatch)
  );
};

export const changeFilter = (project: string, filter: ?string) => (dispatch: Function): void => {
  dispatch(changeProjectActivityFilter(project, filter));
  dispatch(fetchProjectActivity(project));
};

export const addVersion = (project: string, analysis: string, version: string) => (dispatch: Function): Promise<*> => {
  return api.createEvent(analysis, version, 'VERSION').then(
      ({ analysis, ...event }) => dispatch(addEvent(project, analysis, event)),
      rejectOnFail(dispatch)
  );
};

export const removeVersion = (project: string, analysis: string, event: string) => (dispatch: Function): Promise<*> => {
  return api.deleteEvent(event).then(
      () => dispatch(deleteEvent(project, analysis, event)),
      rejectOnFail(dispatch)
  );
};
