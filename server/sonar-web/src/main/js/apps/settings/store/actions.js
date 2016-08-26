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
import { getDefinitions, getValues, setSetting } from '../../../api/settings';
import { receiveValues } from './values/actions';
import { receiveDefinitions } from './definitions/actions';
import { startLoading, failValidation, passValidation } from './settingsPage/actions';
import { parseError } from '../../code/utils';
import { addGlobalErrorMessage, closeAllGlobalMessages } from '../../../components/store/globalMessages';

export const fetchSettings = componentKey => dispatch => {
  return getDefinitions(componentKey)
      .then(definitions => {
        dispatch(receiveDefinitions(definitions));
        const keys = definitions.map(definition => definition.key).join();
        return getValues(keys, componentKey);
      })
      .then(settings => {
        dispatch(receiveValues(settings));
        dispatch(closeAllGlobalMessages());
      })
      .catch(e => parseError(e).then(message => dispatch(addGlobalErrorMessage(message))));
};

export const setValue = (key, value, componentKey) => dispatch => {
  dispatch(startLoading(key));

  return setSetting(key, value, componentKey)
      .then(() => getValues(key, componentKey))
      .then(values => {
        dispatch(receiveValues(values));
        dispatch(passValidation(key));
      })
      .catch(e => parseError(e).then(message => dispatch(failValidation(key, message))));
};
