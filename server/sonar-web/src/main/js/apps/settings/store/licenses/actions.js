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
import * as licenses from '../../../../api/licenses';
import { parseError } from '../../../code/utils';
import {
  addGlobalSuccessMessage,
  addGlobalErrorMessage
} from '../../../../store/globalMessages/duck';
import { translate } from '../../../../helpers/l10n';
import { isLicenseFromListInvalid, isLicenseInvalid } from '../../licenses/licenseUtils';

export const RECEIVE_LICENSES = 'RECEIVE_LICENSES';

const receiveLicenses = licenses => ({
  type: RECEIVE_LICENSES,
  licenses
});

const handleError = dispatch => error => {
  parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));
  return Promise.reject();
};

export const fetchLicenses = () => dispatch => {
  return licenses
    .getLicenses()
    .then(licenses => {
      dispatch(receiveLicenses(licenses));
      /* eslint import/namespace: 0 */
      const invalidLicenses = licenses.some(isLicenseInvalid);
      if (invalidLicenses) {
        dispatch(addGlobalErrorMessage(translate('licenses.there_are_invalid')));
      }
    })
    .catch(handleError(dispatch));
};

export const setLicense = (key, value) => dispatch => {
  const request = value ? licenses.setLicense(key, value) : licenses.resetLicense(key);

  return request
    .then(() =>
      licenses.getLicenses().then(licenses => {
        dispatch(receiveLicenses(licenses));
        if (isLicenseFromListInvalid(licenses, key)) {
          dispatch(addGlobalErrorMessage(translate('licenses.error_message')));
        } else {
          dispatch(addGlobalSuccessMessage(translate('licenses.success_message')));
        }
      })
    )
    .catch(handleError(dispatch));
};
