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

import { DiscreetSelect } from 'design-system';
import * as React from 'react';
import { GroupBase, OptionProps, components } from 'react-select';
import { setIssueTransition } from '../../../api/issues';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import { LabelValueSelectOption } from '../../controls/Select';
import StatusIcon from '../../icons/StatusIcon';
import StatusHelper from '../../shared/StatusHelper';
import { updateIssue } from '../actions';

interface Props {
  hasTransitions: boolean;
  isOpen: boolean;
  issue: Pick<Issue, 'key' | 'resolution' | 'status' | 'transitions' | 'type'>;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

function SingleValueFactory(issue: Props['issue']) {
  return function SingleValue<
    V,
    Option extends LabelValueSelectOption<V>,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>
  >(props: OptionProps<Option, IsMulti, Group>) {
    return (
      <components.SingleValue {...props}>
        <StatusHelper
          className="sw-flex sw-items-center"
          resolution={issue.resolution}
          status={issue.status}
        />
      </components.SingleValue>
    );
  };
}

export default class IssueTransition extends React.PureComponent<Props> {
  setTransition = ({ value }: { value: string }) => {
    updateIssue(
      this.props.onChange,
      setIssueTransition({ issue: this.props.issue.key, transition: value })
    );

    this.toggleSetTransition(false);
  };

  toggleSetTransition = (open: boolean) => {
    this.props.togglePopup('transition', open);
  };

  handleClose = () => {
    this.toggleSetTransition(false);
  };

  render() {
    const { issue } = this.props;

    const transitions = issue.transitions.map((transition) => ({
      label: translate('issue.transition', transition),
      value: transition,
      Icon: <StatusIcon status={transition} />,
    }));

    if (this.props.hasTransitions) {
      return (
        <DiscreetSelect
          aria-label={translateWithParameters(
            'issue.transition.status_x_click_to_change',
            translate('issue.status', issue.status)
          )}
          size="medium"
          className="it__issue-transition"
          components={{ SingleValue: SingleValueFactory(issue) }}
          menuIsOpen={this.props.isOpen && this.props.hasTransitions}
          options={transitions}
          setValue={this.setTransition}
          onMenuClose={this.handleClose}
          onMenuOpen={() => this.toggleSetTransition(true)}
          value={issue.resolution ?? 'OPEN'}
          customValue={
            <StatusHelper className="sw-flex" resolution={issue.resolution} status={issue.status} />
          }
        />
      );
    }

    const resolution = issue.resolution && ` (${translate('issue.resolution', issue.resolution)})`;

    return (
      <span className="sw-flex sw-items-center sw-gap-1">
        <StatusIcon status={issue.status} />

        {translate('issue.status', issue.status)}

        {resolution}
      </span>
    );
  }
}
