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
import { Button, ButtonGroup, ButtonVariety } from '@sonarsource/echoes-react';
import { Modal, Note } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Setting } from '../../../types/settings';
import { getDefaultValue, getPropertyName, isEmptyValue } from '../utils';

type Props = {
  changedValue?: string | string[] | boolean;
  hasError: boolean;
  hasValueChanged: boolean;
  isDefault: boolean;
  isEditing: boolean;
  onCancel: () => void;
  onReset: () => void;
  onSave: () => void;
  setting: Setting;
};

type State = { reseting: boolean };

const MODAL_FORM_ID = 'SETTINGS.RESET_CONFIRM.FORM';

export default class DefinitionActions extends React.PureComponent<Props, State> {
  state: State = { reseting: false };

  handleClose = () => {
    this.setState({ reseting: false });
  };

  handleReset = () => {
    this.setState({ reseting: true });
  };

  handleSubmit = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    this.props.onReset();
    this.handleClose();
  };

  renderModal() {
    const header = translate('settings.reset_confirm.title');
    return (
      <Modal
        headerTitle={header}
        onClose={this.handleClose}
        body={
          <form id={MODAL_FORM_ID} onSubmit={this.handleSubmit}>
            <p>{translate('settings.reset_confirm.description')}</p>
          </form>
        }
        primaryButton={
          <Button type="submit" form={MODAL_FORM_ID} variety={ButtonVariety.Danger}>
            {translate('reset_verb')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }

  render() {
    const { setting, changedValue, isDefault, isEditing, hasValueChanged, hasError } = this.props;
    const hasBeenChangedToEmptyValue =
      changedValue !== undefined && isEmptyValue(setting.definition, changedValue);
    const showReset = hasBeenChangedToEmptyValue || (!isDefault && setting.hasValue);
    const showCancel = hasValueChanged || isEditing;

    return (
      <div className="sw-mt-8">
        <ButtonGroup className="sw-mr-3">
          {hasValueChanged && (
            <Button
              isDisabled={hasError}
              onClick={this.props.onSave}
              variety={ButtonVariety.Primary}
            >
              {translate('save')}
            </Button>
          )}

          {showReset && (
            <Button
              aria-label={translateWithParameters(
                'settings.definition.reset',
                getPropertyName(setting.definition),
              )}
              onClick={this.handleReset}
            >
              {translate('reset_verb')}
            </Button>
          )}

          {showCancel && <Button onClick={this.props.onCancel}>{translate('cancel')}</Button>}
        </ButtonGroup>

        {showReset && (
          <Note>
            {translate('default')}
            {': '}
            {getDefaultValue(setting)}
          </Note>
        )}

        {this.state.reseting && this.renderModal()}
      </div>
    );
  }
}
