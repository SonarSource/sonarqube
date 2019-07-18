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
import { memoize } from 'lodash';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { cleanQuery, parseAsOptionalString, serializeString } from 'sonar-ui-common/helpers/query';
import { decodeJwt } from 'sonar-ui-common/helpers/strings';
import { isBitbucket, isGithub } from '../../../helpers/almIntegrations';

export const ORGANIZATION_IMPORT_BINDING_IN_PROGRESS_TIMESTAMP =
  'sonarcloud.import_org.binding_in_progress';

export const ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP =
  'sonarcloud.import_org.redirect_to_projects';

export const BIND_ORGANIZATION_KEY = 'sonarcloud.bind_org.key';

export const BIND_ORGANIZATION_REDIRECT_TO_ORG_TIMESTAMP = 'sonarcloud.bind_org.redirect_to_org';

export enum Step {
  OrganizationDetails,
  Plan
}

export function formatPrice(price?: number, noSign?: boolean) {
  const priceFormatted = formatMeasure(price, 'FLOAT')
    .replace(/[.|,]0$/, '')
    .replace(/([.|,]\d)$/, '$10');
  return noSign ? priceFormatted : translateWithParameters('billing.price_format', priceFormatted);
}

export interface Query {
  almInstallId?: string;
  almKey?: string;
}

export const parseQuery = memoize(
  (urlQuery: T.RawQuery = {}): Query => {
    let almInstallId = undefined;
    let almKey = undefined;

    if (urlQuery['installation_id']) {
      almKey = 'github';
      almInstallId = parseAsOptionalString(urlQuery['installation_id']);
    } else if (urlQuery['clientKey']) {
      almKey = 'bitbucket';
      almInstallId = parseAsOptionalString(urlQuery['clientKey']);
    } else if (urlQuery['jwt']) {
      const jwt = decodeJwt(urlQuery['jwt']);
      if (jwt && jwt.iss) {
        almKey = 'bitbucket';
        almInstallId = jwt.iss;
      }
    }
    return { almInstallId, almKey };
  }
);

export const serializeQuery = (query: Query): T.RawQuery =>
  cleanQuery({
    installation_id: isGithub(query.almKey) ? serializeString(query.almInstallId) : undefined,
    clientKey: isBitbucket(query.almKey) ? serializeString(query.almInstallId) : undefined
  });
