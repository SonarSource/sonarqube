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
import { ButtonPrimary, FormField, InputField, Modal } from 'design-system/lib';
import * as React from 'react';
import { renameQualityGate } from '../../../api/quality-gates';
import { WithRouterProps, withRouter } from '../../../components/hoc/withRouter';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { QualityGate } from '../../../types/types';

interface Props extends WithRouterProps {
  onClose: () => void;
  onRename: () => Promise<void>;
  qualityGate: QualityGate;
}

interface State {
  name: string;
}

const FORM_ID = 'rename-quality-gate';

class RenameQualityGateForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { name: props.qualityGate.name };
  }

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleRename = (event: React.FormEvent) => {
    event.preventDefault();

    const { qualityGate, router } = this.props;
    const { name } = this.state;

    return renameQualityGate({ currentName: qualityGate.name, name }).then(() => {
      router.push(getQualityGateUrl(name));
      this.props.onRename();
    });
  };

  render() {
    const { qualityGate } = this.props;
    const { name } = this.state;
    const confirmDisable = !name || (qualityGate && qualityGate.name === name);

    return (
      <Modal
        headerTitle={translate('quality_gates.rename')}
        onClose={this.props.onClose}
        body={
          <form id={FORM_ID} onSubmit={this.handleRename}>
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
          <ButtonPrimary autoFocus type="submit" disabled={confirmDisable} form={FORM_ID}>
            {translate('rename')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}

export default withRouter(RenameQualityGateForm);
