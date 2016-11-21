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
import { getDefinitions, getValues, setSettingValue, resetSettingValue } from '../../../api/settings';
import { receiveValues } from './values/actions';
import { receiveDefinitions } from './definitions/actions';
import { startLoading, stopLoading } from './settingsPage/loading/actions';
import { parseError } from '../../code/utils';
import { addGlobalErrorMessage, closeAllGlobalMessages } from '../../../components/store/globalMessages';
import { passValidation, failValidation } from './settingsPage/validationMessages/actions';
import { cancelChange } from './settingsPage/changedValues/actions';
import { isEmptyValue } from '../utils';
import { translate } from '../../../helpers/l10n';
import { getSettingsAppDefinition, getSettingsAppChangedValue } from '../../../app/store/rootReducer';

export const fetchSettings = componentKey => dispatch => {
  return getDefinitions(componentKey)
      .then(definitions => {
        const withoutLicenses = definitions.filter(definition => definition.type !== 'LICENSE');
        dispatch(receiveDefinitions(withoutLicenses));
        const keys = withoutLicenses.map(definition => definition.key).join();
        return getValues(keys, componentKey);
      })
      .then(settings => {
        dispatch(receiveValues(settings));
        dispatch(closeAllGlobalMessages());
      })
      .catch(e => parseError(e).then(message => dispatch(addGlobalErrorMessage(message))));
};

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
        dispatch(receiveValues(values));
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
          dispatch(receiveValues(values));
        } else {
          dispatch(receiveValues([{ key }]));
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
