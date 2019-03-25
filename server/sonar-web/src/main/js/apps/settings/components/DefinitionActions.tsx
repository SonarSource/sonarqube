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
import Modal from '../../../components/controls/Modal';
import { Button, ResetButtonLink, SubmitButton } from '../../../components/ui/buttons';
import { isEmptyValue, getDefaultValue, getSettingValue } from '../utils';
import { translate } from '../../../helpers/l10n';

type Props = {
  changedValue: string;
  hasError: boolean;
  hasValueChanged: boolean;
  isDefault: boolean;
  onCancel: () => void;
  onReset: () => void;
  onSave: () => void;
  setting: T.Setting;
};

type State = { reseting: boolean };

export default class DefinitionActions extends React.PureComponent<Props, State> {
  state: State = { reseting: false };

  handleClose = () => {
    this.setState({ reseting: false });
  };

  handleReset = () => {
    this.setState({ reseting: true });
  };

  handleSubmit = () => {
    this.props.onReset();
    this.handleClose();
  };

  renderModal() {
    const header = translate('settings.reset_confirm.title');
    return (
      <Modal contentLabel={header} onRequestClose={this.handleClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <p>{translate('settings.reset_confirm.description')}</p>
          </div>
          <footer className="modal-foot">
            <SubmitButton className="button-red">{translate('reset_verb')}</SubmitButton>
            <ResetButtonLink onClick={this.handleClose}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    const { setting, isDefault, changedValue, hasValueChanged } = this.props;

    const hasValueToResetTo = !isEmptyValue(setting.definition, getSettingValue(setting));
    const hasBeenChangedToEmptyValue =
      changedValue != null && isEmptyValue(setting.definition, changedValue);
    const showReset =
      hasValueToResetTo && (hasBeenChangedToEmptyValue || (!isDefault && !hasValueChanged));

    return (
      <>
        {isDefault && !hasValueChanged && (
          <div className="spacer-top note" style={{ lineHeight: '24px' }}>
            {translate('settings._default')}
          </div>
        )}
        <div className="settings-definition-changes nowrap">
          {hasValueChanged && (
            <Button
              className="spacer-right button-success"
              disabled={this.props.hasError}
              onClick={this.props.onSave}>
              {translate('save')}
            </Button>
          )}

          {showReset && (
            <Button className="spacer-right" onClick={this.handleReset}>
              {translate('reset_verb')}
            </Button>
          )}

          {hasValueChanged && (
            <ResetButtonLink className="spacer-right" onClick={this.props.onCancel}>
              {translate('cancel')}
            </ResetButtonLink>
          )}

          {showReset && (
            <span className="note">
              {translate('default')}
              {': '}
              {getDefaultValue(setting)}
            </span>
          )}

          {this.state.reseting && this.renderModal()}
        </div>
      </>
    );
  }
}
