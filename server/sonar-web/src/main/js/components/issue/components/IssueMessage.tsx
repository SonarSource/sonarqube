/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import * as React from 'react';
import { ButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { RuleStatus } from '../../../types/rules';
import { WorkspaceContext } from '../../workspace/context';
import IssueMessageTags from './IssueMessageTags';

export interface IssueMessageProps {
  engine?: string;
  quickFixAvailable?: boolean;
  displayWhyIsThisAnIssue?: boolean;
  message: string;
  ruleKey: string;
  ruleStatus?: RuleStatus;
}

export default function IssueMessage(props: IssueMessageProps) {
  const {
    engine,
    quickFixAvailable,
    message,
    ruleKey,
    ruleStatus,
    displayWhyIsThisAnIssue
  } = props;

  const { openRule } = React.useContext(WorkspaceContext);

  return (
    <>
      <div className="display-inline-flex-center issue-message break-word">
        <span className="spacer-right">{message}</span>
        <IssueMessageTags
          engine={engine}
          quickFixAvailable={quickFixAvailable}
          ruleStatus={ruleStatus}
        />
      </div>
      {displayWhyIsThisAnIssue && (
        <ButtonLink
          aria-label={translate('issue.why_this_issue.long')}
          className="issue-see-rule spacer-right text-baseline"
          onClick={() =>
            openRule({
              key: ruleKey
            })
          }>
          {translate('issue.why_this_issue')}
        </ButtonLink>
      )}
    </>
  );
}
