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
import { DeleteButton } from 'sonar-ui-common/components/controls/buttons';

export interface Props {
  idx: number;
  scmAccount: string;
  onChange: (idx: number, scmAccount: string) => void;
  onRemove: (idx: number) => void;
}

export default class UserScmAccountInput extends React.PureComponent<Props> {
  handleChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.props.onChange(this.props.idx, event.currentTarget.value);

  handleRemove = () => this.props.onRemove(this.props.idx);

  render() {
    return (
      <div className="js-scm-account display-flex-row spacer-bottom">
        <input
          maxLength={255}
          onChange={this.handleChange}
          type="text"
          value={this.props.scmAccount}
        />
        <DeleteButton onClick={this.handleRemove} />
      </div>
    );
  }
}
