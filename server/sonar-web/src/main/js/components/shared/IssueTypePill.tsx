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

import { Tooltip } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { Pill, PillVariant } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { IssueSeverity, IssueType } from '../../types/issues';
import IssueTypeIcon from '../icon-mappers/IssueTypeIcon';
import SoftwareImpactSeverityIcon from '../icon-mappers/SoftwareImpactSeverityIcon';

export interface Props {
  className?: string;
  issueType: string;
  severity: IssueSeverity;
}

export default function IssueTypePill(props: Readonly<Props>) {
  const { className, severity, issueType } = props;
  const intl = useIntl();

  const variant = {
    [IssueSeverity.Blocker]: PillVariant.Critical,
    [IssueSeverity.Critical]: PillVariant.Danger,
    [IssueSeverity.Major]: PillVariant.Warning,
    [IssueSeverity.Minor]: PillVariant.Caution,
    [IssueSeverity.Info]: PillVariant.Info,
  }[severity];

  return (
    <Tooltip
      content={
        issueType === IssueType.SecurityHotspot
          ? ''
          : intl.formatMessage(
              {
                id: `issue.type.tooltip`,
              },
              {
                severity: intl.formatMessage({ id: `severity.${severity}` }),
                type: (
                  <span className="sw-lowercase">
                    {intl.formatMessage({ id: `issue.type.${issueType}` })}
                  </span>
                ),
              },
            )
      }
    >
      <Pill
        notClickable
        className={classNames('sw-flex sw-gap-1 sw-items-center', className)}
        variant={issueType !== IssueType.SecurityHotspot ? variant : PillVariant.Accent}
      >
        <IssueTypeIcon type={issueType} />
        {intl.formatMessage({ id: `issue.type.${issueType}` })}
        {issueType !== IssueType.SecurityHotspot && (
          <SoftwareImpactSeverityIcon
            width={14}
            height={14}
            severity={severity}
            data-guiding-id="issue-3"
          />
        )}
      </Pill>
    </Tooltip>
  );
}
