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
import { LOGS_LEVELS } from '../utils';
import { setLogLevel } from '../../../api/system';
import Modal from '../../../components/controls/Modal';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  infoMsg: string;
  logLevel: string;
  onChange: (level: string) => void;
  onClose: () => void;
}

interface State {
  newLevel: string;
  updating: boolean;
}

export default class ChangeLogLevelForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { newLevel: props.logLevel, updating: false };
  }

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { newLevel } = this.state;
    if (!this.state.updating) {
      this.setState({ updating: true });
      setLogLevel(newLevel).then(
        () => this.props.onChange(newLevel),
        () => this.setState({ updating: false })
      );
    }
  };

  handleLevelChange = (event: React.ChangeEvent<HTMLInputElement>) =>
    this.setState({ newLevel: event.currentTarget.value });

  render() {
    const { updating, newLevel } = this.state;
    const header = translate('system.set_log_level');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="set-log-level-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            {LOGS_LEVELS.map(level => (
              <p className="spacer-bottom" key={level}>
                <input
                  checked={level === newLevel}
                  className="spacer-right text-middle"
                  id={`loglevel-${level}`}
                  name="system.log_levels"
                  onChange={this.handleLevelChange}
                  type="radio"
                  value={level}
                />
                <label className="text-middle" htmlFor={`loglevel-${level}`}>
                  {level}
                </label>
              </p>
            ))}
            <Alert className="spacer-top" variant="info">
              {this.props.infoMsg}
            </Alert>
            {newLevel !== 'INFO' && (
              <Alert className="spacer-top" variant="warning">
                {translate('system.log_level.warning')}
              </Alert>
            )}
          </div>
          <div className="modal-foot">
            {updating && <i className="spinner spacer-right" />}
            <SubmitButton disabled={updating} id="set-log-level-submit">
              {translate('save')}
            </SubmitButton>
            <ResetButtonLink id="set-log-level-cancel" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
