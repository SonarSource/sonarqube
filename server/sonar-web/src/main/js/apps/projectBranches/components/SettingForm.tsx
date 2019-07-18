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
import { Button, ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { resetSettingValue, setSimpleSettingValue } from '../../../api/settings';
import { sanitizeTranslation } from '../../settings/utils';

interface Props {
  branch?: string;
  onClose: () => void;
  onChange: () => void;
  project: string;
  setting: T.SettingValue;
}

interface State {
  submitting: boolean;
  value?: string;
}

export default class SettingForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { submitting: false, value: props.setting.value };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { value } = this.state;
    if (!value) {
      return;
    }

    this.setState({ submitting: true });
    setSimpleSettingValue({
      branch: this.props.branch,
      component: this.props.project,
      key: this.props.setting.key,
      value
    }).then(this.props.onChange, this.stopLoading);
  };

  handleValueChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ value: event.currentTarget.value });
  };

  handleResetClick = () => {
    this.setState({ submitting: true });
    resetSettingValue({
      keys: this.props.setting.key,
      component: this.props.project,
      branch: this.props.branch
    }).then(this.props.onChange, this.stopLoading);
  };

  render() {
    const { setting } = this.props;
    const submitDisabled = this.state.submitting || this.state.value === setting.value;

    return (
      <form onSubmit={this.handleSubmit}>
        <div className="modal-body">
          <div
            className="big-spacer-bottom markdown"
            dangerouslySetInnerHTML={{
              __html: sanitizeTranslation(translate(`property.${setting.key}.description`))
            }}
          />
          <div className="modal-field">
            <input
              autoFocus={true}
              className="input-super-large"
              onChange={this.handleValueChange}
              required={true}
              type="text"
              value={this.state.value}
            />
            {setting.inherited && (
              <div className="modal-field-description">{translate('settings._default')}</div>
            )}
            {!setting.inherited && setting.parentValue && (
              <div className="modal-field-description">
                {translateWithParameters('settings.default_x', setting.parentValue)}
              </div>
            )}
          </div>
        </div>
        <footer className="modal-foot">
          {!setting.inherited && setting.parentValue && (
            <Button
              className="pull-left"
              disabled={this.state.submitting}
              onClick={this.handleResetClick}
              type="reset">
              {translate('reset_to_default')}
            </Button>
          )}
          {this.state.submitting && <i className="spinner spacer-right" />}
          <SubmitButton disabled={submitDisabled}>{translate('save')}</SubmitButton>
          <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
        </footer>
      </form>
    );
  }
}
