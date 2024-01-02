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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RuleStatus } from '../../../types/rules';
import DocumentationTooltip from '../../common/DocumentationTooltip';
import Link from '../../common/Link';
import SonarLintIcon from '../../icons/SonarLintIcon';
import { WorkspaceContext } from '../../workspace/context';

export interface IssueMessageTagsProps {
  engine?: string;
  quickFixAvailable?: boolean;
  ruleStatus?: RuleStatus;
}

export default function IssueMessageTags(props: IssueMessageTagsProps) {
  const { engine, quickFixAvailable, ruleStatus } = props;

  const { externalRulesRepoNames } = React.useContext(WorkspaceContext);
  const ruleEngine = (engine && externalRulesRepoNames && externalRulesRepoNames[engine]) || engine;

  return (
    <>
      {quickFixAvailable && (
        <Tooltip
          overlay={
            <FormattedMessage
              id="issue.quick_fix_available_with_sonarlint"
              defaultMessage={translate('issue.quick_fix_available_with_sonarlint')}
              values={{
                link: (
                  <Link
                    to="https://www.sonarqube.org/sonarlint/?referrer=sonarqube-quick-fix"
                    target="_blank"
                  >
                    SonarLint
                  </Link>
                ),
              }}
            />
          }
          mouseLeaveDelay={0.5}
        >
          <SonarLintIcon
            className="it__issues-sonarlint-quick-fix spacer-right"
            size={15}
            ariaLabel="sonar-lint-icon"
          />
        </Tooltip>
      )}
      {ruleStatus &&
        (ruleStatus === RuleStatus.Deprecated || ruleStatus === RuleStatus.Removed) && (
          <DocumentationTooltip
            className="spacer-left"
            content={translate('rules.status', ruleStatus, 'help')}
            links={[
              {
                href: '/user-guide/rules/overview/',
                label: translateWithParameters('see_x', translate('rules')),
              },
            ]}
          >
            <span className="spacer-right badge badge-error">
              {translate('issue.resolution.badge', ruleStatus)}
            </span>
          </DocumentationTooltip>
        )}
      {ruleEngine && (
        <Tooltip overlay={translateWithParameters('issue.from_external_rule_engine', ruleEngine)}>
          <div className="badge spacer-right text-baseline">{ruleEngine}</div>
        </Tooltip>
      )}
    </>
  );
}
