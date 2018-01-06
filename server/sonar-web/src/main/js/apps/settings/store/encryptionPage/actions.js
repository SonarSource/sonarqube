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
import * as api from '../../../../api/settings';
import { parseError } from '../../../../helpers/request';
import {
  addGlobalErrorMessage,
  closeAllGlobalMessages
} from '../../../../store/globalMessages/duck';

export const UPDATE_ENCRYPTION = 'UPDATE_ENCRYPTION';

const updateEncryption = changes => ({
  type: UPDATE_ENCRYPTION,
  changes
});

const startLoading = dispatch => {
  dispatch(updateEncryption({ loading: true }));
  dispatch(closeAllGlobalMessages());
};

const handleError = dispatch => error => {
  parseError(error).then(message => {
    dispatch(addGlobalErrorMessage(message));
    dispatch(updateEncryption({ loading: false }));
  });
};

export const checkSecretKey = () => dispatch => {
  startLoading(dispatch);
  api
    .checkSecretKey()
    .then(data => dispatch(updateEncryption({ ...data, loading: false })))
    .catch(handleError(dispatch));
};

export const generateSecretKey = () => dispatch => {
  startLoading(dispatch);
  api
    .generateSecretKey()
    .then(data =>
      dispatch(
        updateEncryption({
          ...data,
          secretKeyAvailable: false,
          loading: false
        })
      )
    )
    .catch(handleError(dispatch));
};

export const encryptValue = value => dispatch => {
  startLoading(dispatch);
  api
    .encryptValue(value)
    .then(data => dispatch(updateEncryption({ ...data, loading: false })))
    .catch(handleError(dispatch));
};

export const startGeneration = () => dispatch => {
  dispatch(updateEncryption({ secretKeyAvailable: false, secretKey: undefined }));
};
