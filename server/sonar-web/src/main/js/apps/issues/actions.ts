/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { State } from './components/IssuesApp';
import { getLocations } from './utils';

export function enableLocationsNavigator(state: State) {
  const { openIssue, selectedLocationIndex } = state;
  if (openIssue && (openIssue.secondaryLocations.length > 0 || openIssue.flows.length > 0)) {
    const selectedFlowIndex =
      state.selectedFlowIndex || (openIssue.flows.length > 0 ? 0 : undefined);

    return {
      locationsNavigator: true,
      selectedFlowIndex,
      // Also reset index = -1 to 0, we don't want to start on the issue when enabling the location navigator
      selectedLocationIndex:
        selectedLocationIndex && selectedLocationIndex < 0 ? 0 : selectedLocationIndex,
    };
  }
  return null;
}

export function disableLocationsNavigator() {
  return { locationsNavigator: false };
}

export function selectNextLocation(
  state: Pick<State, 'selectedFlowIndex' | 'selectedLocationIndex' | 'openIssue'>,
) {
  const { selectedFlowIndex, selectedLocationIndex: index = -1, openIssue } = state;
  if (openIssue) {
    const locations = getLocations(openIssue, selectedFlowIndex);

    const lastLocationIdx = locations.length - 1;
    if (index === lastLocationIdx) {
      // -1 to jump back to the issue itself
      return { selectedLocationIndex: -1, locationsNavigator: true };
    }
    return {
      selectedLocationIndex: index !== undefined && index < lastLocationIdx ? index + 1 : index,
      locationsNavigator: true,
    };
  }
  return null;
}

export function selectPreviousLocation(state: State) {
  const { selectedFlowIndex, selectedLocationIndex: index, openIssue } = state;
  if (openIssue) {
    if (index === -1) {
      const locations = getLocations(openIssue, selectedFlowIndex);
      const lastLocationIdx = locations.length - 1;
      return { selectedLocationIndex: lastLocationIdx, locationsNavigator: true };
    }
    return {
      selectedLocationIndex: index !== undefined && index > 0 ? index - 1 : index,
      locationsNavigator: true,
    };
  }
  return null;
}

export function selectFlow(nextIndex?: number) {
  return () => {
    return {
      locationsNavigator: true,
      selectedFlowIndex: nextIndex,
      selectedLocationIndex: undefined,
    };
  };
}

export function selectNextFlow(state: State) {
  const { openIssue, selectedFlowIndex } = state;

  if (
    openIssue &&
    selectedFlowIndex !== undefined &&
    (openIssue.flows.length > selectedFlowIndex + 1 ||
      openIssue.flowsWithType.length > selectedFlowIndex + 1)
  ) {
    return {
      selectedFlowIndex: selectedFlowIndex + 1,
      selectedLocationIndex: 0,
      locationsNavigator: true,
    };
  } else if (
    openIssue &&
    selectedFlowIndex === undefined &&
    (openIssue.flows.length > 0 || openIssue.flowsWithType.length > 0)
  ) {
    return { selectedFlowIndex: 0, selectedLocationIndex: 0, locationsNavigator: true };
  }
  return null;
}

export function selectPreviousFlow(state: State) {
  const { openIssue, selectedFlowIndex } = state;
  if (openIssue && selectedFlowIndex !== undefined && selectedFlowIndex > 0) {
    return {
      selectedFlowIndex: selectedFlowIndex - 1,
      selectedLocationIndex: 0,
      locationsNavigator: true,
    };
  }
  return null;
}
