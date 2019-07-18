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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { IssueResponse, setIssueType } from '../../../api/issues';
import SetTypePopup from '../popups/SetTypePopup';

interface Props {
  canSetType: boolean;
  isOpen: boolean;
  issue: Pick<T.Issue, 'type'>;
  setIssueProperty: (
    property: keyof T.Issue,
    popup: string,
    apiCall: (query: T.RawQuery) => Promise<IssueResponse>,
    value: string
  ) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default class IssueType extends React.PureComponent<Props> {
  toggleSetType = (open?: boolean) => {
    this.props.togglePopup('set-type', open);
  };

  setType = (type: string) => {
    this.props.setIssueProperty('type', 'set-type', setIssueType, type);
  };

  handleClose = () => {
    this.toggleSetType(false);
  };

  render() {
    const { issue } = this.props;
    if (this.props.canSetType) {
      return (
        <div className="dropdown">
          <Toggler
            onRequestClose={this.handleClose}
            open={this.props.isOpen && this.props.canSetType}
            overlay={<SetTypePopup issue={issue} onSelect={this.setType} />}>
            <ButtonLink
              className="issue-action issue-action-with-options js-issue-set-type"
              onClick={this.toggleSetType}>
              <IssueTypeIcon className="little-spacer-right" query={issue.type} />
              {translate('issue.type', issue.type)}
              <DropdownIcon className="little-spacer-left" />
            </ButtonLink>
          </Toggler>
        </div>
      );
    } else {
      return (
        <span>
          <IssueTypeIcon className="little-spacer-right" query={issue.type} />
          {translate('issue.type', issue.type)}
        </span>
      );
    }
  }
}
