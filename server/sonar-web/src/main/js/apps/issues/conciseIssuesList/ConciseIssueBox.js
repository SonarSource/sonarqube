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
import classNames from 'classnames';
import ConciseIssueLocations from './ConciseIssueLocations';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import TypeHelper from '../../../components/shared/TypeHelper';
import type { Issue } from '../../../components/issue/types';

type Props = {|
  issue: Issue,
  onClick: string => void,
  selected: boolean
|};

export default function ConciseIssueBox(props: Props) {
  const { issue, selected } = props;

  const handleClick = (event: Event) => {
    event.preventDefault();
    props.onClick(issue.key);
  };

  const clickAttributes = selected ? {} : { onClick: handleClick, role: 'listitem', tabIndex: 0 };

  return (
    <div className={classNames('concise-issue-box', { selected })} {...clickAttributes}>
      <div className="concise-issue-box-message">{issue.message}</div>
      <div className="concise-issue-box-attributes">
        <TypeHelper type={issue.type} />
        <SeverityHelper className="big-spacer-left" severity={issue.severity} />
        <ConciseIssueLocations flows={issue.flows} />
      </div>
    </div>
  );
}
