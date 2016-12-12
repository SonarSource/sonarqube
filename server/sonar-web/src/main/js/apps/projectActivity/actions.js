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
import { receiveProjectActivity, getPaging } from '../../store/projectActivity/duck';
import { onFail } from '../../store/rootActions';
import { getProjectActivity } from '../../store/rootReducer';

export const fetchProjectActivity = (project: string) => (dispatch: Function) => (
    api.getProjectActivity(project).then(
        ({ analyses, paging }) => dispatch(receiveProjectActivity(project, analyses, paging)),
        onFail(dispatch)
    )
);

export const fetchMoreProjectActivity = (project: string) => (dispatch: Function, getState: Function) => {
  const state = getState();
  const { pageIndex } = getPaging(getProjectActivity(state), project);

  api.getProjectActivity(project, pageIndex + 1).then(
      ({ analyses, paging }) => dispatch(receiveProjectActivity(project, analyses, paging)),
      onFail(dispatch)
  );
};
