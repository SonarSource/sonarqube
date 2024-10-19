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
import {
  IconProps,
  SeverityBlockerIcon,
  SeverityCriticalIcon,
  SeverityInfoIcon,
  SeverityMajorIcon,
  SeverityMinorIcon,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import { isDefined } from '../../helpers/types';
import { Dict } from '../../types/types';

interface Props extends IconProps {
  severity: string | null | undefined;
}

const severityIcons: Dict<(props: IconProps) => React.ReactElement> = {
  blocker: SeverityBlockerIcon,
  critical: SeverityCriticalIcon,
  major: SeverityMajorIcon,
  minor: SeverityMinorIcon,
  info: SeverityInfoIcon,
};

export default function SeverityIcon({ severity, ...iconProps }: Omit<Props, 'label'>) {
  if (!isDefined(severity)) {
    return null;
  }

  const DesiredIcon = severityIcons[severity.toLowerCase()];
  return DesiredIcon ? (
    <DesiredIcon {...iconProps} aria-label={translate('severity', severity)} />
  ) : null;
}
