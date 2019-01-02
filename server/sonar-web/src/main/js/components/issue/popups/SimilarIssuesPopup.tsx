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
import * as React from 'react';
import SelectList from '../../common/SelectList';
import SelectListItem from '../../common/SelectListItem';
import { DropdownOverlay } from '../../controls/Dropdown';
import SeverityHelper from '../../shared/SeverityHelper';
import StatusHelper from '../../shared/StatusHelper';
import QualifierIcon from '../../icons-components/QualifierIcon';
import TagsIcon from '../../icons-components/TagsIcon';
import IssueTypeIcon from '../../ui/IssueTypeIcon';
import Avatar from '../../ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { fileFromPath, limitComponentName } from '../../../helpers/path';

interface Props {
  issue: T.Issue;
  onFilter: (property: string, issue: T.Issue) => void;
}

export default class SimilarIssuesPopup extends React.PureComponent<Props> {
  handleSelect = (property: string) => {
    this.props.onFilter(property, this.props.issue);
  };

  render() {
    const { issue } = this.props;

    const items = [
      'type',
      'severity',
      'status',
      'resolution',
      'assignee',
      'rule',
      ...(issue.tags || []).map(tag => `tag###${tag}`),
      'project',
      issue.subProject ? 'module' : undefined,
      'file'
    ].filter(item => item) as string[];

    return (
      <DropdownOverlay noPadding={true}>
        <header className="menu-search">
          <h6>{translate('issue.filter_similar_issues')}</h6>
        </header>

        <SelectList
          className="issues-similar-issues-menu"
          currentItem={items[0]}
          items={items}
          onSelect={this.handleSelect}>
          <SelectListItem item="type">
            <IssueTypeIcon className="little-spacer-right" query={issue.type} />
            {translate('issue.type', issue.type)}
          </SelectListItem>

          <SelectListItem item="severity">
            <SeverityHelper severity={issue.severity} />
          </SelectListItem>

          <SelectListItem item="status">
            <StatusHelper resolution={undefined} status={issue.status} />
          </SelectListItem>

          <SelectListItem item="resolution">
            {issue.resolution != null
              ? translate('issue.resolution', issue.resolution)
              : translate('unresolved')}
          </SelectListItem>

          <SelectListItem item="assignee">
            {issue.assignee != null ? (
              <span>
                {translate('assigned_to')}
                <Avatar
                  className="little-spacer-left little-spacer-right"
                  hash={issue.assigneeAvatar}
                  name={issue.assigneeName || issue.assignee}
                  size={16}
                />
                {issue.assigneeName || issue.assignee}
              </span>
            ) : (
              translate('unassigned')
            )}
          </SelectListItem>

          <li className="divider" />

          <SelectListItem item="rule">{limitComponentName(issue.ruleName)}</SelectListItem>

          {issue.tags != null &&
            issue.tags.map(tag => (
              <SelectListItem item={`tag###${tag}`} key={`tag###${tag}`}>
                <TagsIcon className="icon-half-transparent little-spacer-right text-middle" />
                <span className="text-middle">{tag}</span>
              </SelectListItem>
            ))}

          <li className="divider" />

          <SelectListItem item="project">
            <QualifierIcon className="little-spacer-right" qualifier="TRK" />
            {issue.projectName}
          </SelectListItem>

          {issue.subProject != null && (
            <SelectListItem item="module">
              <QualifierIcon className="little-spacer-right" qualifier="BRC" />
              {issue.subProjectName}
            </SelectListItem>
          )}

          <SelectListItem item="file">
            <QualifierIcon className="little-spacer-right" qualifier={issue.componentQualifier} />
            {fileFromPath(issue.componentLongName)}
          </SelectListItem>
        </SelectList>
      </DropdownOverlay>
    );
  }
}
