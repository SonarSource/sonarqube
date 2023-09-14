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
import { ButtonPrimary, FormField, InputField, Modal } from 'design-system';
import * as React from 'react';
import { copyQualityGate } from '../../../api/quality-gates';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { QualityGate } from '../../../types/types';

interface Props {
  onClose: () => void;
  onCopy: () => Promise<void>;
  qualityGate: QualityGate;
  router: Router;
}

interface State {
  name: string;
}

const FORM_ID = 'rename-quality-gate';

export class CopyQualityGateForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { name: props.qualityGate.name };
  }

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleCopy = (event: React.FormEvent) => {
    event.preventDefault();

    const { qualityGate } = this.props;
    const { name } = this.state;

    return copyQualityGate({ sourceName: qualityGate.name, name }).then((newQualityGate) => {
      this.props.onCopy();
      this.props.router.push(getQualityGateUrl(newQualityGate.name));
    });
  };

  render() {
    const { qualityGate } = this.props;
    const { name } = this.state;
    const buttonDisabled = !name || (qualityGate && qualityGate.name === name);

    return (
      <Modal
        headerTitle={translate('quality_gates.copy')}
        onClose={this.props.onClose}
        body={
          <form id={FORM_ID} onSubmit={this.handleCopy}>
            <MandatoryFieldsExplanation />
            <FormField
              label={translate('name')}
              htmlFor="quality-gate-form-name"
              required
              className="sw-my-2"
            >
              <InputField
                autoFocus
                id="quality-gate-form-name"
                maxLength={100}
                onChange={this.handleNameChange}
                size="auto"
                type="text"
                value={name}
              />
            </FormField>
          </form>
        }
        primaryButton={
          <ButtonPrimary autoFocus type="submit" disabled={buttonDisabled} form={FORM_ID}>
            {translate('copy')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}

export default withRouter(CopyQualityGateForm);
