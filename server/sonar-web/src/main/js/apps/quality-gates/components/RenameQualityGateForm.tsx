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
import ConfirmModal from '../../../components/controls/ConfirmModal';
import { renameQualityGate } from '../../../api/quality-gates';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onRename: () => Promise<void>;
  organization?: string;
  qualityGate: T.QualityGate;
}

interface State {
  name: string;
}

export default class RenameQualityGateForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { name: props.qualityGate.name };
  }

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleRename = () => {
    const { qualityGate, organization } = this.props;
    const { name } = this.state;

    if (!name) {
      return undefined;
    }

    return renameQualityGate({ id: qualityGate.id, name, organization }).then(() =>
      this.props.onRename()
    );
  };

  render() {
    const { qualityGate } = this.props;
    const { name } = this.state;
    const confirmDisable = !name || (qualityGate && qualityGate.name === name);

    return (
      <ConfirmModal
        confirmButtonText={translate('rename')}
        confirmDisable={confirmDisable}
        header={translate('quality_gates.rename')}
        onClose={this.props.onClose}
        onConfirm={this.handleRename}
        size="small">
        <div className="modal-field">
          <label htmlFor="quality-gate-form-name">
            {translate('name')}
            <em className="mandatory">*</em>
          </label>
          <input
            autoFocus={true}
            id="quality-gate-form-name"
            maxLength={100}
            onChange={this.handleNameChange}
            required={true}
            size={50}
            type="text"
            value={name}
          />
        </div>
      </ConfirmModal>
    );
  }
}
