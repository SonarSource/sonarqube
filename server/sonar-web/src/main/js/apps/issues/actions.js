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
/*:: import type { State } from './components/App'; */

export const enableLocationsNavigator = (state /*: State */) => {
  const { openIssue } = state;
  if (openIssue && (openIssue.secondaryLocations.length > 0 || openIssue.flows.length > 0)) {
    return {
      locationsNavigator: true,
      selectedFlowIndex: state.selectedFlowIndex || (openIssue.flows.length > 0 ? 0 : null),
      selectedLocationIndex: state.selectedLocationIndex || 0
    };
  }
};

export const disableLocationsNavigator = () => ({
  locationsNavigator: false
});

export const selectLocation = (nextIndex /*: ?number */) => (state /*: State */) => {
  const { selectedLocationIndex: index, openIssue } = state;
  if (openIssue) {
    if (!state.locationsNavigator) {
      if (nextIndex != null) {
        return { locationsNavigator: true, selectedLocationIndex: nextIndex };
      }
    } else if (index != null) {
      // disable locations when selecting (clicking) the same location
      return {
        locationsNavigator: nextIndex !== index,
        selectedLocationIndex: nextIndex
      };
    }
  }
};

export const selectNextLocation = (state /*: State */) => {
  const { selectedFlowIndex, selectedLocationIndex: index, openIssue } = state;
  if (openIssue) {
    const locations =
      selectedFlowIndex != null ? openIssue.flows[selectedFlowIndex] : openIssue.secondaryLocations;
    return {
      selectedLocationIndex: index != null && locations.length > index + 1 ? index + 1 : index
    };
  }
};

export const selectPreviousLocation = (state /*: State */) => {
  const { selectedLocationIndex: index, openIssue } = state;
  if (openIssue) {
    return { selectedLocationIndex: index != null && index > 0 ? index - 1 : index };
  }
};

export const selectFlow = (nextIndex /*: ?number */) => () => {
  return { selectedFlowIndex: nextIndex, selectedLocationIndex: 0 };
};

export const selectNextFlow = (state /*: State */) => {
  const { openIssue, selectedFlowIndex } = state;
  if (openIssue && selectedFlowIndex != null && openIssue.flows.length > selectedFlowIndex + 1) {
    return { selectedFlowIndex: selectedFlowIndex + 1, selectedLocationIndex: 0 };
  }
};

export const selectPreviousFlow = (state /*: State */) => {
  const { openIssue, selectedFlowIndex } = state;
  if (openIssue && selectedFlowIndex != null && selectedFlowIndex > 0) {
    return { selectedFlowIndex: selectedFlowIndex - 1, selectedLocationIndex: 0 };
  }
};
