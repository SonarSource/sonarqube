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
import * as PropTypes from 'prop-types';
import { createQualityGate } from '../../../api/quality-gates';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { Button } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';

interface Props {
  onCreate: () => Promise<void>;
  organization?: string;
}

interface State {
  name: string;
}

export default class CreateQualityGateForm extends React.PureComponent<Props, State> {
  static contextTypes = {
    router: PropTypes.object
  };

  state = { name: '' };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleCreate = () => {
    const { organization } = this.props;
    const { name } = this.state;

    if (!name) {
      return undefined;
    }

    return createQualityGate({ name, organization })
      .then(qualityGate => {
        return this.props.onCreate().then(() => qualityGate);
      })
      .then(qualityGate => {
        this.context.router.push(getQualityGateUrl(String(qualityGate.id), organization));
      });
  };

  render() {
    const { name } = this.state;
    return (
      <ConfirmButton
        confirmButtonText={translate('save')}
        confirmDisable={!name}
        modalBody={
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
        }
        modalHeader={translate('quality_gates.create')}
        onConfirm={this.handleCreate}>
        {({ onClick }) => (
          <Button id="quality-gate-add" onClick={onClick}>
            {translate('create')}
          </Button>
        )}
      </ConfirmButton>
    );
  }
}
