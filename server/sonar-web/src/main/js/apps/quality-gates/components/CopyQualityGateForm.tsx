/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { copyQualityGate } from '../../../api/quality-gates';
import ConfirmModal from '../../../components/controls/ConfirmModal';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { QualityGate } from '../../../types/types';

interface Props {
  onClose: () => void;
  onCopy: () => Promise<void>;
  organization: string;
  qualityGate: QualityGate;
  router: Router;
}

interface State {
  name: string;
}

export class CopyQualityGateForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { name: props.qualityGate.name };
  }

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleCopy = () => {
    const { qualityGate, organization } = this.props;
    const { name } = this.state;

    return copyQualityGate({ id: qualityGate.id, name, organization }).then((newQualityGate) => {
      this.props.onCopy();
      this.props.router.push(getQualityGateUrl(organization, String(newQualityGate.id)));
    });
  };

  render() {
    const { qualityGate } = this.props;
    const { name } = this.state;
    const confirmDisable = !name || (qualityGate && qualityGate.name === name);

    return (
      <ConfirmModal
        confirmButtonText={translate('copy')}
        confirmDisable={confirmDisable}
        header={translate('quality_gates.copy')}
        onClose={this.props.onClose}
        onConfirm={this.handleCopy}
        size="small"
      >
        <MandatoryFieldsExplanation className="modal-field" />
        <div className="modal-field">
          <label htmlFor="quality-gate-form-name">
            {translate('name')}
            <MandatoryFieldMarker />
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

export default withRouter(CopyQualityGateForm);
