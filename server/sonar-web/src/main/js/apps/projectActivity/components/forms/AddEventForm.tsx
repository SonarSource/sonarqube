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
import ConfirmModal from '../../../../components/controls/ConfirmModal';
import { translate } from '../../../../helpers/l10n';
import { ParsedAnalysis } from '../../utils';

interface Props {
  addEvent: (analysis: string, name: string, category?: string) => Promise<void>;
  addEventButtonText: string;
  analysis: ParsedAnalysis;
  onClose: () => void;
}

interface State {
  name: string;
}

export default class AddEventForm extends React.PureComponent<Props, State> {
  state: State = { name: '' };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.target.value });
  };

  handleSubmit = () => {
    return this.props.addEvent(this.props.analysis.key, this.state.name);
  };

  render() {
    return (
      <ConfirmModal
        confirmButtonText={translate('save')}
        confirmDisable={!this.state.name}
        header={translate(this.props.addEventButtonText)}
        onClose={this.props.onClose}
        onConfirm={this.handleSubmit}
        size="small">
        <div className="modal-field">
          <label>{translate('name')}</label>
          <input
            autoFocus={true}
            onChange={this.handleNameChange}
            type="text"
            value={this.state.name}
          />
        </div>
      </ConfirmModal>
    );
  }
}
