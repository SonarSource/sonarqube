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
import { setIssueType } from '../../../api/issues';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { IssueResponse } from '../../../types/issues';
import { Issue, RawQuery } from '../../../types/types';

interface Props {
  canSetType: boolean;
  issue: Pick<Issue, 'type'>;
  setIssueProperty: (
    property: keyof Issue,
    popup: string,
    apiCall: (query: RawQuery) => Promise<IssueResponse>,
    value: string
  ) => void;
}

export default class IssueType extends React.PureComponent<Props> {
  setType = ({ value }: { value: string }) => {
    this.props.setIssueProperty('type', 'set-type', setIssueType, value);
  };

  render() {
    const { issue } = this.props;
    const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
    const typesOptions = TYPES.map((type) => ({
      label: translate('issue.type', type),
      value: type,
      Icon: <IssueTypeIcon query={type} />,
    }));
    if (this.props.canSetType) {
      return (
        <DiscreetSelect
          aria-label={translateWithParameters(
            'issue.type.type_x_click_to_change',
            translate('issue.type', issue.type)
          )}
          className="it__issue-type"
          options={typesOptions}
          setValue={this.setType}
          value={issue.type}
        />
      );
    }

    return (
      <span className="sw-flex sw-items-center sw-gap-1">
        <IssueTypeIcon query={issue.type} />
        {translate('issue.type', issue.type)}
      </span>
    );
  }
}
