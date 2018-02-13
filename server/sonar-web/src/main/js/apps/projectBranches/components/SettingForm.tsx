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
import { SettingValue, setSimpleSettingValue, resetSettingValue } from '../../../api/settings';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  branch?: string;
  onClose: () => void;
  onChange: () => void;
  project: string;
  setting: SettingValue;
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
    }).then(this.props.onChange, () => {
      if (this.mounted) {
        this.setState({ submitting: false });
      }
    });
  };

  handleValueChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ value: event.currentTarget.value });
  };

  handleResetClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ submitting: true });
    resetSettingValue(this.props.setting.key, this.props.project, this.props.branch).then(
      this.props.onChange,
      () => {
        if (this.mounted) {
          this.setState({ submitting: false });
        }
      }
    );
  };

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const { setting } = this.props;
    const submitDisabled = this.state.submitting || this.state.value === setting.value;

    return (
      <form onSubmit={this.handleSubmit}>
        <div className="modal-body">
          <div
            className="big-spacer-bottom markdown"
            dangerouslySetInnerHTML={{ __html: translate(`property.${setting.key}.description`) }}
          />
          <div className="big-spacer-bottom">
            <input
              autoFocus={true}
              className="input-super-large"
              onChange={this.handleValueChange}
              required={true}
              type="text"
              value={this.state.value}
            />
            {setting.inherited && (
              <div className="note spacer-top">{translate('settings._default')}</div>
            )}
            {!setting.inherited &&
              setting.parentValue && (
                <div className="note spacer-top">
                  {translateWithParameters('settings.default_x', setting.parentValue)}
                </div>
              )}
          </div>
        </div>
        <footer className="modal-foot">
          {!setting.inherited &&
            setting.parentValue && (
              <button
                className="pull-left"
                disabled={this.state.submitting}
                onClick={this.handleResetClick}
                type="reset">
                {translate('reset_to_default')}
              </button>
            )}
          {this.state.submitting && <i className="spinner spacer-right" />}
          <button disabled={submitDisabled} type="submit">
            {translate('save')}
          </button>
          <a href="#" onClick={this.handleCancelClick}>
            {translate('cancel')}
          </a>
        </footer>
      </form>
    );
  }
}
