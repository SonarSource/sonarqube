/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Dispatch } from 'redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { parseError } from 'sonar-ui-common/helpers/request';
import {
  getDefinitions,
  getValues,
  resetSettingValue,
  setSettingValue
} from '../../../api/settings';
import { closeAllGlobalMessages } from '../../../store/globalMessages';
import {
  getSettingsAppChangedValue,
  getSettingsAppDefinition,
  Store
} from '../../../store/rootReducer';
import { isEmptyValue } from '../utils';
import { receiveDefinitions } from './definitions';
import {
  cancelChange,
  failValidation,
  passValidation,
  startLoading,
  stopLoading
} from './settingsPage';
import { receiveValues } from './values';

export function fetchSettings(component?: string) {
  return (dispatch: Dispatch) => {
    return getDefinitions(component).then(definitions => {
      const filtered = definitions
        .filter(definition => definition.type !== 'LICENSE')
        // do not display this setting on project level
        .filter(
          definition => !component || definition.key !== 'sonar.branch.longLivedBranches.regex'
        );
      dispatch(receiveDefinitions(filtered));
    });
  };
}

export function fetchValues(keys: string, component?: string) {
  return (dispatch: Dispatch) =>
    getValues({ keys, component }).then(settings => {
      dispatch(receiveValues(settings, component));
      dispatch(closeAllGlobalMessages());
    });
}

export function checkValue(key: string) {
  return (dispatch: Dispatch, getState: () => Store) => {
    const state = getState();
    const definition = getSettingsAppDefinition(state, key);
    const value = getSettingsAppChangedValue(state, key);

    if (isEmptyValue(definition, value)) {
      if (definition.defaultValue === undefined) {
        dispatch(failValidation(key, translate('settings.state.value_cant_be_empty_no_default')));
      } else {
        dispatch(failValidation(key, translate('settings.state.value_cant_be_empty')));
      }
      return false;
    }

    dispatch(passValidation(key));
    return true;
  };
}

export function saveValue(key: string, component?: string) {
  return (dispatch: Dispatch, getState: () => Store) => {
    dispatch(startLoading(key));
    const state = getState();
    const definition = getSettingsAppDefinition(state, key);
    const value = getSettingsAppChangedValue(state, key);

    if (isEmptyValue(definition, value)) {
      dispatch(failValidation(key, translate('settings.state.value_cant_be_empty')));
      dispatch(stopLoading(key));
      return Promise.reject();
    }

    return setSettingValue(definition, value, component)
      .then(() => getValues({ keys: key, component }))
      .then(values => {
        dispatch(receiveValues(values, component));
        dispatch(cancelChange(key));
        dispatch(passValidation(key));
        dispatch(stopLoading(key));
      })
      .catch(handleError(key, dispatch));
  };
}

export function resetValue(key: string, component?: string) {
  return (dispatch: Dispatch) => {
    dispatch(startLoading(key));

    return resetSettingValue({ keys: key, component })
      .then(() => getValues({ keys: key, component }))
      .then(values => {
        if (values.length > 0) {
          dispatch(receiveValues(values, component));
        } else {
          dispatch(receiveValues([{ key }], component));
        }
        dispatch(passValidation(key));
        dispatch(stopLoading(key));
      })
      .catch(handleError(key, dispatch));
  };
}

function handleError(key: string, dispatch: Dispatch) {
  return (response: Response) => {
    dispatch(stopLoading(key));
    return parseError(response).then(message => {
      dispatch(failValidation(key, message));
      return Promise.reject();
    });
  };
}
