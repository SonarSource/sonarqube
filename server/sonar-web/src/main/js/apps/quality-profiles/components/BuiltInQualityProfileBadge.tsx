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
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
  tooltip?: boolean;
}

export default function BuiltInQualityProfileBadge({ className, tooltip = true }: Props) {
  const badge = (
    <Badge variant="default" className={className}>
      {translate('quality_profiles.built_in')}
    </Badge>
  );

  if (tooltip) {
    return <Tooltip content={translate('quality_profiles.built_in.description')}>{badge}</Tooltip>;
  }

  return badge;
}
