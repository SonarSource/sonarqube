/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import UpdateKeyConfirm from './UpdateKeyConfirm';
import { Button } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: { key: string; name: string };
  onKeyChange: (oldKey: string, newKey: string) => Promise<void>;
}

interface State {
  newKey?: string;
}

export default class UpdateKeyForm extends React.PureComponent<Props, State> {
  state: State = {};

  handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newKey = event.currentTarget.value;
    this.setState({ newKey });
  };

  handleResetClick = () => {
    this.setState({ newKey: undefined });
  };

  render() {
    const { component } = this.props;
    const { newKey } = this.state;
    const value = newKey !== undefined ? newKey : component.key;
    const hasChanged = newKey !== undefined && newKey !== component.key;

    return (
      <div className="js-fine-grained-update" data-key={component.key}>
        <input
          className="input-super-large big-spacer-right"
          onChange={this.handleInputChange}
          type="text"
          value={value}
        />

        <UpdateKeyConfirm component={component} newKey={newKey} onConfirm={this.props.onKeyChange}>
          {({ onClick }) => (
            <Button disabled={!hasChanged} onClick={onClick}>
              {translate('update_verb')}
            </Button>
          )}
        </UpdateKeyConfirm>

        <Button className="spacer-left" disabled={!hasChanged} onClick={this.handleResetClick}>
          {translate('reset_verb')}
        </Button>
      </div>
    );
  }
}
