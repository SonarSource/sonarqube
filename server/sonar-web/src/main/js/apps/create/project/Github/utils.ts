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

import { isWebUri } from 'valid-url';
import { getGithubClientId } from '../../../../api/alm-integrations';
import { getHostUrl } from '../../../../helpers/urls';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';

export async function redirectToGithub(params: {
  isMonorepoSetup: boolean;
  selectedDopSetting?: DopSetting;
}) {
  const { isMonorepoSetup, selectedDopSetting } = params;

  if (selectedDopSetting?.url === undefined) {
    return;
  }

  const { clientId } = await getGithubClientId(selectedDopSetting.key);

  if (clientId === undefined) {
    throw new Error('Received no GitHub client id');
  }
  const queryParams = [
    { param: 'client_id', value: clientId },
    {
      param: 'redirect_uri',
      value: encodeURIComponent(
        `${getHostUrl()}/projects/create?mode=${AlmKeys.GitHub}&dopSetting=${
          selectedDopSetting.id
        }${isMonorepoSetup ? '&mono=true' : ''}`,
      ),
    },
  ]
    .map(({ param, value }) => `${param}=${value}`)
    .join('&');

  let instanceRootUrl;
  // Strip the api section from the url, since we're not hitting the api here.
  if (selectedDopSetting.url.includes('/api/v3')) {
    // GitHub Enterprise
    instanceRootUrl = selectedDopSetting.url.replace('/api/v3', '');
  } else {
    // github.com
    instanceRootUrl = selectedDopSetting.url.replace('api.', '');
  }

  // strip the trailing /
  instanceRootUrl = instanceRootUrl.replace(/\/$/, '');
  if (isWebUri(instanceRootUrl) === undefined) {
    throw new Error('Invalid GitHub URL');
  } else {
    window.location.replace(`${instanceRootUrl}/login/oauth/authorize?${queryParams}`);
  }
}
