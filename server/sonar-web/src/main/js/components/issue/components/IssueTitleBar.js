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
import IssueChangelog from './IssueChangelog';
import IssueMessage from './IssueMessage';
import { getSingleIssueUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import type { Issue } from '../types';

type Props = {
  issue: Issue,
  currentPopup: string,
  onFail: (Error) => void,
  onFilterClick?: () => void,
  togglePopup: (string) => void
};

export default function IssueTitleBar(props: Props) {
  const { issue } = props;
  const hasSimilarIssuesFilter = props.onFilterClick != null;

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
              {issue.line != null &&
                <li className="issue-meta">
                  <span className="issue-meta-label" title={translate('line_number')}>
                    L{issue.line}
                  </span>
                </li>}
              <li className="issue-meta">
                <a
                  className="js-issue-permalink icon-link"
                  href={getSingleIssueUrl(issue.key)}
                  target="_blank"
                />
              </li>
              {hasSimilarIssuesFilter &&
                <li className="issue-meta">
                  <button
                    className="js-issue-filter button-link issue-action issue-action-with-options"
                    aria-label={translate('issue.filter_similar_issues')}
                    onClick={props.onFilterClick}>
                    <i className="icon-filter icon-half-transparent" />{' '}
                    <i className="icon-dropdown" />
                  </button>
                </li>}
            </ul>
          </td>
        </tr>
      </tbody>
    </table>
  );
}
