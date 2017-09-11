/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Modal from 'react-modal';
import { setLogLevel } from '../../../api/system';
import { translate } from '../../../helpers/l10n';

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

const LOG_LEVELS = ['INFO', 'DEBUG', 'TRACE'];

export default class ChangeLogLevelForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { newLevel: props.logLevel, updating: false };
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { newLevel } = this.state;
    if (!this.state.updating && newLevel !== this.props.logLevel) {
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
    const disableSubmit = updating || newLevel === this.props.logLevel;
    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>
        <form id="set-log-level-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            {LOG_LEVELS.map(level => (
              <p key={level} className="spacer-bottom">
                <input
                  type="radio"
                  className="spacer-right text-middle"
                  name="system.log_levels"
                  value={level}
                  checked={level === newLevel}
                  onChange={this.handleLevelChange}
                />
                {level}
              </p>
            ))}
            <div className="alert alert-info spacer-top">{this.props.infoMsg}</div>
            {newLevel !== 'INFO' && (
              <div className="alert alert-danger spacer-top">
                {translate('system.log_level.warning')}
              </div>
            )}
          </div>
          <div className="modal-foot">
            {updating && <i className="spinner spacer-right" />}
            <button disabled={disableSubmit} id="set-log-level-submit">
              {translate('save')}
            </button>
            <a href="#" id="set-log-level-cancel" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
        </form>
      </Modal>
    );
  }
}
