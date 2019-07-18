/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import IssueIcon from 'sonar-ui-common/components/icons/IssueIcon';
import { sortByType } from '../../../helpers/issues';

interface Props {
  issues: T.Issue[];
  line: T.SourceLine;
  onClick: () => void;
}

export default class LineIssuesIndicator extends React.PureComponent<Props> {
  handleClick = (event: React.MouseEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClick();
  };

  render() {
    const { issues, line } = this.props;
    const hasIssues = issues.length > 0;
    const className = classNames('source-meta', 'source-line-issues', {
      'source-line-with-issues': hasIssues
    });
    const mostImportantIssue = hasIssues ? sortByType(issues)[0] : null;

    return (
      <td
        className={className}
        data-line-number={line.line}
        onClick={hasIssues ? this.handleClick : undefined}
        role={hasIssues ? 'button' : undefined}
        tabIndex={hasIssues ? 0 : undefined}>
        {mostImportantIssue != null && <IssueIcon type={mostImportantIssue.type} />}
        {issues.length > 1 && <span className="source-line-issues-counter">{issues.length}</span>}
      </td>
    );
  }
}
