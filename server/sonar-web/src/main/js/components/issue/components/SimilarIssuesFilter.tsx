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
import FilterIcon from 'sonar-ui-common/components/icons/FilterIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import SimilarIssuesPopup from '../popups/SimilarIssuesPopup';

interface Props {
  isOpen: boolean;
  issue: T.Issue;
  togglePopup: (popup: string, show?: boolean) => void;
  onFilter?: (property: string, issue: T.Issue) => void;
}

export default class SimilarIssuesFilter extends React.PureComponent<Props> {
  handleFilter = (property: string, issue: T.Issue) => {
    this.togglePopup(false);
    if (this.props.onFilter) {
      this.props.onFilter(property, issue);
    }
  };

  togglePopup = (open?: boolean) => {
    this.props.togglePopup('similarIssues', open);
  };

  handleClose = () => {
    this.togglePopup(false);
  };

  render() {
    return (
      <div className="dropdown">
        <Toggler
          onRequestClose={this.handleClose}
          open={this.props.isOpen}
          overlay={<SimilarIssuesPopup issue={this.props.issue} onFilter={this.handleFilter} />}>
          <ButtonLink
            aria-label={translate('issue.filter_similar_issues')}
            className="issue-action issue-action-with-options js-issue-filter"
            onClick={this.togglePopup}
            title={translate('issue.filter_similar_issues')}>
            <FilterIcon className="icon-half-transparent" />
            <DropdownIcon className="icon-half-transparent" />
          </ButtonLink>
        </Toggler>
      </div>
    );
  }
}
