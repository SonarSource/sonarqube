/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { Link } from 'react-router';
import IssueChangelog from './IssueChangelog';
import IssueMessage from './IssueMessage';
import SimilarIssuesFilter from './SimilarIssuesFilter';
import LocationIndex from '../../common/LocationIndex';
import Tooltip from '../../controls/Tooltip';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { formatMeasure } from '../../../helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import type { Issue } from '../types';

type Props = {|
  issue: Issue,
  currentPopup: string,
  onFail: Error => void,
  onFilter?: (property: string, issue: Issue) => void,
  togglePopup: string => void
|};

const stopPropagation = (event: Event) => event.stopPropagation();

export default function IssueTitleBar(props: Props) {
  const { issue } = props;
  const hasSimilarIssuesFilter = props.onFilter != null;

  const locationsCount =
    issue.secondaryLocations.length +
    issue.flows.reduce((sum, locations) => sum + locations.length, 0);

  const locationsBadge = (
    <Tooltip
      overlay={translateWithParameters(
        'issue.this_issue_involves_x_code_locations',
        formatMeasure(locationsCount)
      )}
      placement="left">
      <LocationIndex>{locationsCount}</LocationIndex>
    </Tooltip>
  );

  // dirty trick :(
  const onIssuesPage = document.getElementById('issues-page') != null;

  const issueUrl = getComponentIssuesUrl(issue.project, { issues: issue.key, open: issue.key });

  return (
    <table className="issue-table">
      <tbody>
        <tr>
          <td>
            <IssueMessage
              message={issue.message}
              rule={issue.rule}
              organization={issue.organization}
            />
          </td>
          <td className="issue-table-meta-cell issue-table-meta-cell-first">
            <ul className="list-inline issue-meta-list">
              <li className="issue-meta">
                <IssueChangelog
                  creationDate={issue.creationDate}
                  isOpen={props.currentPopup === 'changelog'}
                  issue={issue}
                  togglePopup={props.togglePopup}
                  onFail={props.onFail}
                />
              </li>
              {issue.textRange != null &&
                <li className="issue-meta">
                  <span className="issue-meta-label" title={translate('line_number')}>
                    L{issue.textRange.endLine}
                  </span>
                </li>}
              {locationsCount > 0 &&
                <li className="issue-meta">
                  {onIssuesPage
                    ? locationsBadge
                    : <Link onClick={stopPropagation} target="_blank" to={issueUrl}>
                        {locationsBadge}
                      </Link>}
                </li>}
              <li className="issue-meta">
                <Link
                  className="js-issue-permalink icon-link"
                  onClick={stopPropagation}
                  target="_blank"
                  to={issueUrl}
                />
              </li>
              {hasSimilarIssuesFilter &&
                <li className="issue-meta">
                  <SimilarIssuesFilter
                    isOpen={props.currentPopup === 'similarIssues'}
                    issue={issue}
                    togglePopup={props.togglePopup}
                    onFail={props.onFail}
                    onFilter={props.onFilter}
                  />
                </li>}
            </ul>
          </td>
        </tr>
      </tbody>
    </table>
  );
}
