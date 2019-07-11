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
import * as React from 'react';
import { getCookie } from 'sonar-ui-common/helpers/cookies';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';

export default function Unauthorized() {
  const message = decodeURIComponent(getCookie('AUTHENTICATION-ERROR') || '');
  return (
    <div className="page-wrapper-simple" id="bd">
      <div className="page-simple" id="nonav">
        <div className="text-center">
          <p id="unauthorized">{translate('unauthorized.message')}</p>

          {Boolean(message) && (
            <p className="spacer-top">
              {translate('unauthorized.reason')} {message}
            </p>
          )}

          <div className="big-spacer-top">
            <a href={getBaseUrl() + '/'}>{translate('layout.home')}</a>
          </div>
        </div>
      </div>
    </div>
  );
}
