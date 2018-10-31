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
import { memoize } from 'lodash';
import { translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import {
  RawQuery,
  parseAsOptionalString,
  cleanQuery,
  serializeString
} from '../../../helpers/query';
import { isBitbucket, isGithub } from '../../../helpers/almIntegrations';

export const ORGANIZATION_IMPORT_REDIRECT_TO_PROJECT_TIMESTAMP =
  'sonarcloud.import_org.redirect_to_projects';

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
  (urlQuery: RawQuery = {}): Query => {
    return {
      almInstallId:
        parseAsOptionalString(urlQuery['installation_id']) ||
        parseAsOptionalString(urlQuery['clientKey']),
      almKey:
        (urlQuery['installation_id'] && 'github') ||
        (urlQuery['clientKey'] && 'bitbucket') ||
        undefined
    };
  }
);

export const serializeQuery = (query: Query): RawQuery =>
  cleanQuery({
    installation_id: isGithub(query.almKey) ? serializeString(query.almInstallId) : undefined,
    clientKey: isBitbucket(query.almKey) ? serializeString(query.almInstallId) : undefined
  });
