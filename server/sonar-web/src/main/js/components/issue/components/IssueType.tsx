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

import { DisabledText, Tooltip } from 'design-system';
import * as React from 'react';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import IssueTypeIcon from '../../icon-mappers/IssueTypeIcon';
import { DeprecatedFieldTooltip } from './DeprecatedFieldTooltip';

interface Props {
  issue: Pick<Issue, 'type'>;
}

export default function IssueType({ issue }: Props) {
  const docUrl = useDocUrl('/');

  return (
    <Tooltip overlay={<DeprecatedFieldTooltip field="type" docUrl={docUrl} />}>
      <DisabledText className="sw-flex sw-items-center sw-gap-1 sw-cursor-not-allowed">
        <IssueTypeIcon fill="iconTypeDisabled" type={issue.type} aria-hidden />
        {translate('issue.type', issue.type)}
      </DisabledText>
    </Tooltip>
  );
}
