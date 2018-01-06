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
// @flow
/*:: import type { State } from './components/App'; */

export const enableLocationsNavigator = (state /*: State */) => {
  const { openIssue, selectedLocationIndex } = state;
  if (openIssue && (openIssue.secondaryLocations.length > 0 || openIssue.flows.length > 0)) {
    return {
      locationsNavigator: true,
      selectedFlowIndex: state.selectedFlowIndex || (openIssue.flows.length > 0 ? 0 : null),
      // Also reset index = -1 to 0, we don't want to start on the issue when enabling the location navigator
      selectedLocationIndex:
        !selectedLocationIndex || selectedLocationIndex < 0 ? 0 : selectedLocationIndex
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
    const lastLocationIdx = locations.length - 1;
    if (index === lastLocationIdx) {
      // -1 to jump back to the issue itself
      return { selectedLocationIndex: -1 };
    }
    return {
      selectedLocationIndex: index != null && index < lastLocationIdx ? index + 1 : index
    };
  }
};

export const selectPreviousLocation = (state /*: State */) => {
  const { selectedFlowIndex, selectedLocationIndex: index, openIssue } = state;
  if (openIssue) {
    if (index === -1) {
      const locations =
        selectedFlowIndex != null
          ? openIssue.flows[selectedFlowIndex]
          : openIssue.secondaryLocations;
      const lastLocationIdx = locations.length - 1;
      return { selectedLocationIndex: lastLocationIdx };
    }
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
