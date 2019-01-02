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
import { Button } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  myIssues: boolean;
  onMyIssuesChange: (myIssues: boolean) => void;
}

export default class MyIssuesFilter extends React.PureComponent<Props> {
  handleClick = (myIssues: boolean) => () => {
    this.props.onMyIssuesChange(myIssues);
  };

  render() {
    const { myIssues } = this.props;

    return (
      <div className="issues-my-issues-filter">
        <div className="button-group">
          <Button
            className={myIssues ? 'button-active' : undefined}
            onClick={this.handleClick(true)}>
            {translate('issues.my_issues')}
          </Button>
          <Button
            className={myIssues ? undefined : 'button-active'}
            onClick={this.handleClick(false)}>
            {translate('all')}
          </Button>
        </div>
      </div>
    );
  }
}
