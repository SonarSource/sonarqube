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

import { Badge } from '~design-system';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseVersion } from '../utils';

export default function DeprecatedBadge({ since }: { since?: string }) {
  const version = since && parseVersion(since);
  const overlay = version
    ? translateWithParameters('api_documentation.will_be_removed_in_x', `${version.major + 1}.0`)
    : translate('api_documentation.deprecation_tooltip');
  const label = since
    ? translateWithParameters('api_documentation.deprecated_since_x', since)
    : translate('api_documentation.deprecated');
  return (
    <Tooltip content={overlay}>
      <span>
        <Badge variant="default">{label}</Badge>
      </span>
    </Tooltip>
  );
}
