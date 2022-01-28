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
import { Link } from 'react-router';
import Tooltip from '../../../components/controls/Tooltip';
import LinkIcon from '../../../components/icons/LinkIcon';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { RuleStatus } from '../../../types/rules';
import { Issue } from '../../../types/types';
import LocationIndex from '../../common/LocationIndex';
import { WorkspaceContext } from '../../workspace/context';
import IssueChangelog from './IssueChangelog';
import IssueMessage from './IssueMessage';
import SimilarIssuesFilter from './SimilarIssuesFilter';

export interface IssueTitleBarProps {
  branchLike?: BranchLike;
  currentPopup?: string;
  displayLocationsCount?: boolean;
  displayLocationsLink?: boolean;
  issue: Issue;
  onFilter?: (property: string, issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default function IssueTitleBar(props: IssueTitleBarProps) {
  const { issue } = props;
  const hasSimilarIssuesFilter = props.onFilter != null;

  const locationsCount =
    issue.secondaryLocations.length +
    issue.flows.reduce((sum, locations) => sum + locations.length, 0);

  const locationsBadge = (
    <Tooltip
      overlay={translateWithParameters(
        'issue.this_issue_involves_x_code_locations',
        formatMeasure(locationsCount, 'INT')
      )}>
      <LocationIndex>{locationsCount}</LocationIndex>
    </Tooltip>
  );

  const displayLocations = props.displayLocationsCount && locationsCount > 0;

  const issueUrl = getComponentIssuesUrl(issue.project, {
    ...getBranchLikeQuery(props.branchLike),
    issues: issue.key,
    open: issue.key,
    types: issue.type === 'SECURITY_HOTSPOT' ? issue.type : undefined
  });

  return (
    <div className="issue-row">
      <WorkspaceContext.Consumer>
        {({ externalRulesRepoNames, openRule }) => (
          <IssueMessage
            engine={issue.externalRuleEngine}
            engineName={
              issue.externalRuleEngine &&
              externalRulesRepoNames &&
              externalRulesRepoNames[issue.externalRuleEngine]
            }
            quickFixAvailable={issue.quickFixAvailable}
            manualVulnerability={issue.fromHotspot && issue.type === 'VULNERABILITY'}
            message={issue.message}
            onOpenRule={openRule}
            ruleKey={issue.rule}
            ruleStatus={issue.ruleStatus as RuleStatus | undefined}
          />
        )}
      </WorkspaceContext.Consumer>

      <div className="issue-row-meta">
        <div className="issue-meta-list">
          <div className="issue-meta">
            <IssueChangelog
              creationDate={issue.creationDate}
              isOpen={props.currentPopup === 'changelog'}
              issue={issue}
              togglePopup={props.togglePopup}
            />
          </div>
          {issue.textRange != null && (
            <div className="issue-meta">
              <span className="issue-meta-label" title={translate('line_number')}>
                L{issue.textRange.endLine}
              </span>
            </div>
          )}
          {displayLocations && (
            <div className="issue-meta">
              {props.displayLocationsLink ? (
                <Link target="_blank" to={issueUrl}>
                  {locationsBadge}
                </Link>
              ) : (
                locationsBadge
              )}
            </div>
          )}
          <div className="issue-meta">
            <Link
              className="js-issue-permalink link-no-underline"
              target="_blank"
              title={translate('permalink')}
              to={issueUrl}>
              <LinkIcon />
            </Link>
          </div>
          {hasSimilarIssuesFilter && (
            <div className="issue-meta">
              <SimilarIssuesFilter
                isOpen={props.currentPopup === 'similarIssues'}
                issue={issue}
                onFilter={props.onFilter}
                togglePopup={props.togglePopup}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
