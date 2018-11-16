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
import { Button, SubmitButton } from '../../components/ui/buttons';
import { translate, translateWithParameters } from '../../helpers/l10n';
import ConfirmButton from '../../components/controls/ConfirmButton';

interface Props {
  component: Pick<T.Component, 'key' | 'name'>;
  onKeyChange: (newKey: string) => Promise<void>;
}

interface State {
  newKey?: string;
}

export default class UpdateForm extends React.PureComponent<Props, State> {
  state: State = {};

  handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const newKey = event.currentTarget.value;
    this.setState({ newKey });
  };

  handleReset = () => {
    this.setState({ newKey: undefined });
  };

  render() {
    const { component } = this.props;
    const { newKey } = this.state;
    const value = newKey != null ? newKey : component.key;
    const hasChanged = value !== component.key;

    return (
      <ConfirmButton
        confirmButtonText={translate('update_verb')}
        confirmData={newKey}
        modalBody={
          <>
            {translateWithParameters('update_key.are_you_sure_to_change_key', component.name)}
            <div className="spacer-top">
              {translate('update_key.old_key')}
              {': '}
              <strong>{component.key}</strong>
            </div>
            <div className="spacer-top">
              {translate('update_key.new_key')}
              {': '}
              <strong>{newKey}</strong>
            </div>
          </>
        }
        modalHeader={translate('update_key.page')}
        onConfirm={this.props.onKeyChange}>
        {({ onFormSubmit }) => (
          <form onSubmit={onFormSubmit}>
            <input
              className="input-super-large"
              id="update-key-new-key"
              onChange={this.handleChange}
              placeholder={translate('update_key.new_key')}
              required={true}
              type="text"
              value={value}
            />

            <div className="spacer-top">
              <SubmitButton disabled={!hasChanged} id="update-key-submit">
                {translate('update_verb')}
              </SubmitButton>

              <Button
                className="spacer-left"
                disabled={!hasChanged}
                id="update-key-reset"
                onClick={this.handleReset}
                type="reset">
                {translate('reset_verb')}
              </Button>
            </div>
          </form>
        )}
      </ConfirmButton>
    );
  }
}
