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
import { getBaseUrl } from '../../../../helpers/urls';
import './LoginButtons.css';

export default function LoginButtons() {
  return (
    <div>
      <a className="sc-login-button" href={`${getBaseUrl()}/sessions/init/github`}>
        <img alt="" height="25" src={`${getBaseUrl()}/images/sonarcloud/github.svg`} />
        GitHub
      </a>
      <a className="sc-login-button" href={`${getBaseUrl()}/sessions/init/bitbucket`}>
        <img alt="" height="25" src={`${getBaseUrl()}/images/sonarcloud/bitbucket.svg`} />
        Bitbucket
      </a>
      <a className="sc-login-button" href={`${getBaseUrl()}/sessions/init/microsoft`}>
        <img alt="" height="25" src={`${getBaseUrl()}/images/sonarcloud/azure.svg`} />
        Azure DevOps
      </a>
    </div>
  );
}
