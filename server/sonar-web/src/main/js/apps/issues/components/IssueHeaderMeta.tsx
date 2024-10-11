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
import { LightLabel, Note, SeparatorCircleIcon } from 'design-system';
import React from 'react';
import DateFromNow from '../../../components/intl/DateFromNow';
import IssuePrioritized from '../../../components/issue/components/IssuePrioritized';
import { translate } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';

interface Props {
  issue: Issue;
}

export default function IssueHeaderMeta({ issue }: Readonly<Props>) {
  return (
    <Note className="sw-flex sw-flex-wrap sw-items-center sw-gap-2 sw-text-xs">
      {typeof issue.line === 'number' && (
        <>
          <div className="sw-flex sw-gap-1">
            <span>{translate('issue.line_affected')}</span>
            <span className="sw-font-semibold">L{issue.line}</span>
          </div>
          <SeparatorCircleIcon />
        </>
      )}

      {issue.effort && (
        <>
          <div className="sw-flex sw-gap-1">
            <span>{translate('issue.effort')}</span>
            <span className="sw-font-semibold">{issue.effort}</span>
          </div>
          <SeparatorCircleIcon />
        </>
      )}

      <div className="sw-flex sw-gap-1">
        <span>{translate('issue.introduced')}</span>
        <span className="sw-font-semibold">
          <LightLabel>
            <DateFromNow date={issue.creationDate} />
          </LightLabel>
        </span>
      </div>

      {(issue.codeVariants?.length ?? 0) > 0 && (
        <>
          <SeparatorCircleIcon />
          <div className="sw-flex sw-gap-1">
            <span>{translate('issue.code_variants')}</span>
            <Tooltip content={issue.codeVariants?.join(', ')}>
              <span className="sw-font-semibold">
                <LightLabel>{issue.codeVariants?.join(', ')}</LightLabel>
              </span>
            </Tooltip>
          </div>
          <SeparatorCircleIcon />
        </>
      )}

      {issue.prioritizedRule && (
        <>
          <SeparatorCircleIcon />
          <IssuePrioritized />
        </>
      )}
    </Note>
  );
}
