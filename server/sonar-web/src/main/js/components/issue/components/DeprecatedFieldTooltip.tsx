/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';

export interface DeprecatedTooltipProps {
  docUrl: string;
  field: 'type' | 'severity';
}

export function DeprecatedFieldTooltip({ field, docUrl }: DeprecatedTooltipProps) {
  return (
    <>
      <p className="sw-mb-4">{translate('issue', field, 'deprecation.title')}</p>
      <p>{translate('issue', field, 'deprecation.filter_by')}</p>
      <ul className="sw-list-disc sw-ml-6">
        <li>{translate('issue.clean_code_attributes')}</li>
        <li>{translate('issue.software_qualities')}</li>
      </ul>
      <hr className="sw-w-full sw-mx-0 sw-my-4" />
      <FormattedMessage
        defaultMessage={translate('learn_more_x')}
        id="learn_more_x"
        values={{
          link: (
            <Link isExternal to={docUrl}>
              {translate('issue', field, 'deprecation.documentation')}
            </Link>
          ),
        }}
      />
    </>
  );
}
