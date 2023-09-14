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
import { DangerButtonPrimary, Modal } from 'design-system';
import * as React from 'react';
import { deleteQualityGate } from '../../../api/quality-gates';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityGatesUrl } from '../../../helpers/urls';
import { QualityGate } from '../../../types/types';

interface Props {
  readonly onClose: () => void;
  onDelete: () => Promise<void>;
  qualityGate: QualityGate;
  router: Router;
}

export class DeleteQualityGateForm extends React.PureComponent<Props> {
  onDelete = () => {
    const { qualityGate } = this.props;
    return deleteQualityGate({ name: qualityGate.name })
      .then(this.props.onDelete)
      .then(() => {
        this.props.router.push(getQualityGatesUrl());
      });
  };

  render() {
    const { qualityGate } = this.props;

    return (
      <Modal
        headerTitle={translate('quality_gates.delete')}
        onClose={this.props.onClose}
        body={translateWithParameters('quality_gates.delete.confirm.message', qualityGate.name)}
        primaryButton={
          <DangerButtonPrimary autoFocus type="submit" onClick={this.onDelete}>
            {translate('delete')}
          </DangerButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}

export default withRouter(DeleteQualityGateForm);
