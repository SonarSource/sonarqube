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

import { IconProps, TextSubdued } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { IssueSeverity as IssueSeverityType } from '../../../types/issues';
import { Issue } from '../../../types/types';
import DocumentationTooltip from '../../common/DocumentationTooltip';
import IssueSeverityIcon from '../../icon-mappers/IssueSeverityIcon';
import { DeprecatedFieldTooltip } from './DeprecatedFieldTooltip';

interface Props extends IconProps {
  issue: Pick<Issue, 'severity'>;
}

export default function IssueSeverity({ issue, ...iconProps }: Readonly<Props>) {
  return (
    <DocumentationTooltip
      content={<DeprecatedFieldTooltip field="severity" />}
      links={[
        {
          href: '/user-guide/issues',
          label: translate('learn_more'),
        },
      ]}
    >
      <TextSubdued className="sw-flex sw-items-center sw-gap-1/2">
        <IssueSeverityIcon
          fill="iconSeverityDisabled"
          severity={issue.severity as IssueSeverityType}
          aria-hidden
          {...iconProps}
        />
        {translate('severity', issue.severity)}
      </TextSubdued>
    </DocumentationTooltip>
  );
}
