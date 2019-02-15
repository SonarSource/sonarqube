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
import { translate } from '../../../../helpers/l10n';
import ConfirmModal from '../../../../components/controls/ConfirmModal';

interface Props {
  changeEvent: (event: string, name: string) => Promise<void>;
  event: T.AnalysisEvent;
  header: string;
  onClose: () => void;
}

interface State {
  name: string;
}

export default class ChangeEventForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { name: props.event.name };
  }

  changeInput = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.target.value });
  };

  handleSubmit = () => {
    return this.props.changeEvent(this.props.event.key, this.state.name);
  };

  render() {
    const { name } = this.state;
    return (
      <ConfirmModal
        confirmButtonText={translate('change_verb')}
        confirmDisable={!name || name === this.props.event.name}
        header={this.props.header}
        onClose={this.props.onClose}
        onConfirm={this.handleSubmit}
        size="small">
        <div className="modal-field">
          <label>{translate('name')}</label>
          <input autoFocus={true} onChange={this.changeInput} type="text" value={name} />
        </div>
      </ConfirmModal>
    );
  }
}
