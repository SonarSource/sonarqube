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

import { translate } from '../../../helpers/l10n';

export interface DeprecatedTooltipProps {
  field: 'type' | 'severity';
}

const FILTERS_LIST = {
  type: ['issue.clean_code_attribute', 'software_quality'],
  severity: ['software_quality', 'issue.severity.new'],
};

export function DeprecatedFieldTooltip({ field }: DeprecatedTooltipProps) {
  return (
    <>
      <p className="sw-mb-4">{translate('issue', field, 'deprecation.title')}</p>
      <p>{translate('issue', field, 'deprecation.filter_by')}</p>
      <ul className="sw-list-disc sw-ml-6">
        {FILTERS_LIST[field].map((key) => (
          <li key={key}>{translate(key)}</li>
        ))}
      </ul>
    </>
  );
}
