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
import type { Action, ReceiveProjectActivityAction, DeleteAnalysisAction } from './duck';

export type State = {
  [key: string]: Array<string>
};

const receiveProjectActivity = (state: State, action: ReceiveProjectActivityAction): State => {
  const analyses = state[action.project] || [];
  const newAnalyses = action.analyses.map(analysis => analysis.key);
  return {
    ...state,
    [action.project]: action.paging.pageIndex === 1 ? newAnalyses : [...analyses, ...newAnalyses]
  };
};

const deleteAnalysis = (state: State, action: DeleteAnalysisAction): State => {
  const analyses = state[action.project];
  return {
    ...state,
    [action.project]: analyses.filter(key => key !== action.analysis)
  };
};

export default (state: State = {}, action: Action): State => {
  switch (action.type) {
    case 'RECEIVE_PROJECT_ACTIVITY':
      return receiveProjectActivity(state, action);
    case 'DELETE_PROJECT_ACTIVITY_ANALYSIS':
      return deleteAnalysis(state, action);
    default:
      return state;
  }
};
