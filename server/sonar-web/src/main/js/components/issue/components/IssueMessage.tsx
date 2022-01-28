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
import { FormattedMessage } from 'react-intl';
import { ButtonLink } from '../../../components/controls/buttons';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RuleStatus } from '../../../types/rules';
import DocumentationTooltip from '../../common/DocumentationTooltip';
import SonarLintIcon from '../../icons/SonarLintIcon';
import { WorkspaceContextShape } from '../../workspace/context';

export interface IssueMessageProps {
  engine?: string;
  engineName?: string;
  quickFixAvailable?: boolean;
  manualVulnerability: boolean;
  message: string;
  onOpenRule: WorkspaceContextShape['openRule'];
  ruleKey: string;
  ruleStatus?: RuleStatus;
}

export default function IssueMessage(props: IssueMessageProps) {
  const {
    engine,
    engineName,
    quickFixAvailable,
    manualVulnerability,
    message,
    ruleKey,
    ruleStatus
  } = props;
  const ruleEngine = engineName ? engineName : engine;

  return (
    <div className="display-inline-flex-center issue-message break-word">
      <span className="spacer-right">{message}</span>
      {quickFixAvailable && (
        <Tooltip
          overlay={
            <FormattedMessage
              id="issue.quick_fix_available_with_sonarlint"
              defaultMessage={translate('issue.quick_fix_available_with_sonarlint')}
              values={{
                link: (
                  <a
                    href="https://www.sonarqube.org/sonarlint/?referrer=sonarqube-quick-fix"
                    rel="noopener noreferrer"
                    target="_blank">
                    SonarLint
                  </a>
                )
              }}
            />
          }
          mouseLeaveDelay={0.5}>
          <SonarLintIcon className="it__issues-sonarlint-quick-fix spacer-right" size={15} />
        </Tooltip>
      )}
      <ButtonLink
        aria-label={translate('issue.why_this_issue.long')}
        className="issue-see-rule spacer-right text-baseline"
        onClick={() =>
          props.onOpenRule({
            key: ruleKey
          })
        }>
        {translate('issue.why_this_issue')}
      </ButtonLink>
      {ruleStatus && (ruleStatus === RuleStatus.Deprecated || ruleStatus === RuleStatus.Removed) && (
        <DocumentationTooltip
          className="spacer-left"
          content={translate('rules.status', ruleStatus, 'help')}
          links={[
            {
              href: '/documentation/user-guide/rules/',
              label: translateWithParameters('see_x', translate('rules'))
            }
          ]}>
          <span className="spacer-right badge badge-error">
            {translate('rules.status', ruleStatus)}
          </span>
        </DocumentationTooltip>
      )}
      {ruleEngine && (
        <Tooltip overlay={translateWithParameters('issue.from_external_rule_engine', ruleEngine)}>
          <div className="badge spacer-right text-baseline">{ruleEngine}</div>
        </Tooltip>
      )}
      {manualVulnerability && (
        <Tooltip overlay={translate('issue.manual_vulnerability.description')}>
          <div className="badge spacer-right text-baseline">
            {translate('issue.manual_vulnerability')}
          </div>
        </Tooltip>
      )}
    </div>
  );
}
