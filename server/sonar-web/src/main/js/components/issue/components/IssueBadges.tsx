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
import classNames from 'classnames';
import { Badge } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RuleStatus } from '../../../types/rules';
import DocumentationTooltip from '../../common/DocumentationTooltip';
import Link from '../../common/Link';
import Tooltip from '../../controls/Tooltip';
import SonarLintIcon from '../../icons/SonarLintIcon';

export interface IssueBadgesProps {
  quickFixAvailable?: boolean;
  ruleStatus?: RuleStatus;
}

export default function IssueBadges(props: IssueBadgesProps) {
  const { quickFixAvailable, ruleStatus } = props;

  return (
    <div className="sw-flex">
      <SonarLintBadge quickFixAvailable={quickFixAvailable} />
      <span className={classNames({ 'sw-ml-2': quickFixAvailable })}>
        <RuleBadge ruleStatus={ruleStatus} />
      </span>
    </div>
  );
}

export function RuleBadge({
  ruleStatus,
  className,
}: {
  ruleStatus?: RuleStatus;
  className?: string;
}) {
  if (ruleStatus === RuleStatus.Deprecated || ruleStatus === RuleStatus.Removed) {
    return (
      <DocumentationTooltip
        content={translate('rules.status', ruleStatus, 'help')}
        links={[
          {
            href: '/user-guide/rules/overview/',
            label: translateWithParameters('see_x', translate('rules')),
          },
        ]}
      >
        <Badge variant="deleted" className={className}>
          {translate('issue.resolution.badge', ruleStatus)}
        </Badge>
      </DocumentationTooltip>
    );
  }

  return null;
}

export function SonarLintBadge({ quickFixAvailable }: { quickFixAvailable?: boolean }) {
  if (quickFixAvailable) {
    return (
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
        <div className="sw-flex sw-items-center">
          <SonarLintIcon
            className="it__issues-sonarlint-quick-fix"
            size={15}
            description={translate('issue.quick_fix_available_with_sonarlint_no_link')}
          />
        </div>
      </Tooltip>
    );
  }

  return null;
}
