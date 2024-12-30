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

import { DropdownMenu, DropdownMenuAlign, Spinner, Tooltip } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { useState } from 'react';
import { useIntl } from 'react-intl';
import { Pill, PillVariant } from '~design-system';
import { IssueSeverity, IssueType } from '../../types/issues';
import IssueTypeIcon from '../icon-mappers/IssueTypeIcon';
import SoftwareImpactSeverityIcon from '../icon-mappers/SoftwareImpactSeverityIcon';

export interface Props {
  className?: string;
  issueType: string;
  onSetSeverity?: (severity: IssueSeverity) => Promise<void>;
  severity: IssueSeverity;
}

export default function IssueTypePill(props: Readonly<Props>) {
  const { className, severity, issueType, onSetSeverity } = props;
  const intl = useIntl();
  const [updatingSeverity, setUpdatingSeverity] = useState(false);
  const variant = {
    [IssueSeverity.Blocker]: PillVariant.Critical,
    [IssueSeverity.Critical]: PillVariant.Danger,
    [IssueSeverity.Major]: PillVariant.Warning,
    [IssueSeverity.Minor]: PillVariant.Caution,
    [IssueSeverity.Info]: PillVariant.Info,
  }[severity];

  const renderPill = (notClickable = false) => (
    <Pill
      notClickable={notClickable}
      className={classNames('sw-flex sw-gap-1 sw-items-center', className)}
      variant={issueType !== IssueType.SecurityHotspot ? variant : PillVariant.Accent}
    >
      <IssueTypeIcon type={issueType} />
      {intl.formatMessage({ id: `issue.type.${issueType}` })}
      <Spinner isLoading={updatingSeverity} className="sw-ml-1/2">
        {issueType !== IssueType.SecurityHotspot && (
          <SoftwareImpactSeverityIcon
            width={14}
            height={14}
            severity={severity}
            data-guiding-id="issue-3"
          />
        )}
      </Spinner>
    </Pill>
  );

  const handleSetSeverity = async (severity: IssueSeverity) => {
    setUpdatingSeverity(true);
    await onSetSeverity?.(severity);
    setUpdatingSeverity(false);
  };

  if (onSetSeverity) {
    return (
      <DropdownMenu.Root
        align={DropdownMenuAlign.Start}
        items={Object.values(IssueSeverity).map((severityItem) => (
          <DropdownMenu.ItemButtonCheckable
            key={severityItem}
            isDisabled={severityItem === severity}
            isChecked={severityItem === severity}
            onClick={() => handleSetSeverity(severityItem)}
          >
            <div className="sw-flex sw-items-center sw-gap-2">
              <SoftwareImpactSeverityIcon width={14} height={14} severity={severityItem} />
              {intl.formatMessage({ id: `severity.${severityItem}` })}
            </div>
          </DropdownMenu.ItemButtonCheckable>
        ))}
      >
        <Tooltip
          content={intl.formatMessage(
            {
              id: `issue.type.tooltip_with_change`,
            },
            {
              severity: intl.formatMessage({ id: `severity.${severity}` }),
            },
          )}
        >
          {renderPill()}
        </Tooltip>
      </DropdownMenu.Root>
    );
  }

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
              },
            )
      }
    >
      {renderPill(true)}
    </Tooltip>
  );
}
