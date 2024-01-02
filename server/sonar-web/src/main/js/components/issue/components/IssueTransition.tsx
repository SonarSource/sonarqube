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
import { setIssueTransition } from '../../../api/issues';
import { ButtonLink } from '../../../components/controls/buttons';
import Toggler from '../../../components/controls/Toggler';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import StatusHelper from '../../shared/StatusHelper';
import { updateIssue } from '../actions';
import SetTransitionPopup from '../popups/SetTransitionPopup';

interface Props {
  hasTransitions: boolean;
  isOpen: boolean;
  issue: Pick<Issue, 'key' | 'resolution' | 'status' | 'transitions' | 'type'>;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default class IssueTransition extends React.PureComponent<Props> {
  setTransition = (transition: string) => {
    updateIssue(
      this.props.onChange,
      setIssueTransition({ issue: this.props.issue.key, transition })
    );
    this.toggleSetTransition(false);
  };

  toggleSetTransition = (open?: boolean) => {
    this.props.togglePopup('transition', open);
  };

  handleClose = () => {
    this.toggleSetTransition(false);
  };

  render() {
    const { issue } = this.props;

    if (this.props.hasTransitions) {
      return (
        <div className="dropdown">
          <Toggler
            onRequestClose={this.handleClose}
            open={this.props.isOpen && this.props.hasTransitions}
            overlay={
              <SetTransitionPopup onSelect={this.setTransition} transitions={issue.transitions} />
            }
          >
            <ButtonLink
              aria-label={translateWithParameters(
                'issue.transition.status_x_click_to_change',
                translate('issue.status', issue.status)
              )}
              aria-expanded={this.props.isOpen}
              className="issue-action issue-action-with-options js-issue-transition"
              onClick={this.toggleSetTransition}
            >
              <StatusHelper
                className="issue-meta-label"
                resolution={issue.resolution}
                status={issue.status}
              />
              <DropdownIcon className="little-spacer-left" />
            </ButtonLink>
          </Toggler>
        </div>
      );
    }

    return (
      <StatusHelper
        className="issue-meta-label"
        resolution={issue.resolution}
        status={issue.status}
      />
    );
  }
}
