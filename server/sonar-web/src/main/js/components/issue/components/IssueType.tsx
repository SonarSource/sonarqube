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
import DocHelpTooltip from '../../../sonar-aligned/components/controls/DocHelpTooltip';
import { Issue } from '../../../types/types';
import IssueTypeIcon from '../../icon-mappers/IssueTypeIcon';
import { DeprecatedFieldTooltip } from './DeprecatedFieldTooltip';

interface Props extends IconProps {
  issue: Pick<Issue, 'type'>;
}

export default function IssueType({ issue, ...iconProps }: Readonly<Props>) {
  return (
    <DocHelpTooltip
      content={<DeprecatedFieldTooltip field="type" />}
      links={[
        {
          href: '/user-guide/issues',
          label: translate('learn_more'),
        },
      ]}
    >
      <TextSubdued className="sw-flex sw-items-center sw-gap-1/2">
        <IssueTypeIcon fill="iconTypeDisabled" type={issue.type} aria-hidden {...iconProps} />
        {translate('issue.type', issue.type)}
      </TextSubdued>
    </DocHelpTooltip>
  );
}
