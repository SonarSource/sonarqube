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
import { receiveValues } from './values/actions';
import { receiveDefinitions } from './definitions/actions';
import { startLoading, stopLoading } from './settingsPage/loading/actions';
import { passValidation, failValidation } from './settingsPage/validationMessages/actions';
import { cancelChange } from './settingsPage/changedValues/actions';
import {
  getDefinitions,
  getValues,
  setSettingValue,
  resetSettingValue
} from '../../../api/settings';
import { parseError } from '../../../helpers/request';
import { addGlobalErrorMessage, closeAllGlobalMessages } from '../../../store/globalMessages/duck';
import { isEmptyValue } from '../utils';
import { translate } from '../../../helpers/l10n';
import { getSettingsAppDefinition, getSettingsAppChangedValue } from '../../../store/rootReducer';

export const fetchSettings = componentKey => dispatch => {
  return getDefinitions(componentKey).then(
    definitions => {
      const filtered = definitions
        .filter(definition => definition.type !== 'LICENSE')
        // do not display this setting on project level
        .filter(
          definition =>
            componentKey == null || definition.key !== 'sonar.branch.longLivedBranches.regex'
        );
      dispatch(receiveDefinitions(filtered));
    },
    e => parseError(e).then(message => dispatch(addGlobalErrorMessage(message)))
  );
};

export const fetchValues = (keys, componentKey) => dispatch =>
  getValues(keys, componentKey).then(
    settings => {
      dispatch(receiveValues(settings, componentKey));
      dispatch(closeAllGlobalMessages());
    },
    () => {}
  );

export const saveValue = (key, componentKey) => (dispatch, getState) => {
  dispatch(startLoading(key));

  const state = getState();
  const definition = getSettingsAppDefinition(state, key);
  const value = getSettingsAppChangedValue(state, key);

  if (isEmptyValue(definition, value)) {
    dispatch(failValidation(key, translate('settings.state.value_cant_be_empty')));
    dispatch(stopLoading(key));
    return Promise.reject();
  }

  return setSettingValue(definition, value, componentKey)
    .then(() => getValues(key, componentKey))
    .then(values => {
      dispatch(receiveValues(values, componentKey));
      dispatch(cancelChange(key));
      dispatch(passValidation(key));
      dispatch(stopLoading(key));
    })
    .catch(e => {
      dispatch(stopLoading(key));
      parseError(e).then(message => dispatch(failValidation(key, message)));
      return Promise.reject();
    });
};

export const resetValue = (key, componentKey) => dispatch => {
  dispatch(startLoading(key));

  return resetSettingValue(key, componentKey)
    .then(() => getValues(key, componentKey))
    .then(values => {
      if (values.length > 0) {
        dispatch(receiveValues(values, componentKey));
      } else {
        dispatch(receiveValues([{ key }], componentKey));
      }
      dispatch(passValidation(key));
      dispatch(stopLoading(key));
    })
    .catch(e => {
      dispatch(stopLoading(key));
      parseError(e).then(message => dispatch(failValidation(key, message)));
      return Promise.reject();
    });
};
