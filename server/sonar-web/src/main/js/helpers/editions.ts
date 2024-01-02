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
import { omitNil } from '../helpers/request';
import { Edition, EditionKey } from '../types/editions';
import { SystemUpgrade } from '../types/system';

const EDITIONS: { [x in EditionKey]: Edition } = {
  community: {
    key: EditionKey.community,
    name: 'Community Edition',
    homeUrl: 'https://www.sonarsource.com/open-source-editions/',
    downloadProperty: 'downloadUrl',
  },
  developer: {
    key: EditionKey.developer,
    name: 'Developer Edition',
    homeUrl: 'https://www.sonarsource.com/plans-and-pricing/developer/',
    downloadProperty: 'downloadDeveloperUrl',
  },
  enterprise: {
    key: EditionKey.enterprise,
    name: 'Enterprise Edition',
    homeUrl: 'https://www.sonarsource.com/plans-and-pricing/enterprise/',
    downloadProperty: 'downloadEnterpriseUrl',
  },
  datacenter: {
    key: EditionKey.datacenter,
    name: 'Data Center Edition',
    homeUrl: 'https://www.sonarsource.com/plans-and-pricing/data-center/',
    downloadProperty: 'downloadDatacenterUrl',
  },
};

export function getEdition(editionKey: EditionKey) {
  return EDITIONS[editionKey];
}

export function getAllEditionsAbove(currentEdition?: EditionKey) {
  const editions = Object.values(EDITIONS);
  const currentEditionIdx = editions.findIndex((edition) => edition.key === currentEdition);
  return editions.slice(currentEditionIdx + 1);
}

export function getEditionUrl(
  edition: Edition,
  data: { serverId?: string; ncloc?: number; sourceEdition?: EditionKey }
) {
  let url = edition.homeUrl;
  const query = new URLSearchParams(omitNil(data)).toString();
  if (query) {
    url += '?' + query;
  }
  return url;
}

export function getEditionDownloadUrl(edition: Edition, lastUpgrade: SystemUpgrade) {
  return lastUpgrade[edition.downloadProperty] || lastUpgrade.downloadUrl;
}

export function getEditionDownloadFilename(url: string) {
  return url.replace(/^.+\/(sonarqube-[\w\-.]+\.zip)$/, '$1');
}
