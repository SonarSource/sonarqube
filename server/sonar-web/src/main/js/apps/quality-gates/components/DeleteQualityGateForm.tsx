/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { deleteQualityGate } from '../../../api/quality-gates';
import { Button } from '../../../components/controls/buttons';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityGatesUrl } from '../../../helpers/urls';
import { QualityGate } from '../../../types/types';

interface Props {
  onDelete: () => Promise<void>;
  qualityGate: QualityGate;
  router: Router;
}

export class DeleteQualityGateForm extends React.PureComponent<Props> {
  onDelete = () => {
    const { qualityGate } = this.props;
    return deleteQualityGate({ id: qualityGate.id })
      .then(this.props.onDelete)
      .then(() => {
        this.props.router.push(getQualityGatesUrl());
      });
  };

  render() {
    const { qualityGate } = this.props;

    return (
      <ConfirmButton
        confirmButtonText={translate('delete')}
        isDestructive={true}
        modalBody={translateWithParameters(
          'quality_gates.delete.confirm.message',
          qualityGate.name
        )}
        modalHeader={translate('quality_gates.delete')}
        onConfirm={this.onDelete}
      >
        {({ onClick }) => (
          <Button
            className="little-spacer-left button-red"
            id="quality-gate-delete"
            onClick={onClick}
          >
            {translate('delete')}
          </Button>
        )}
      </ConfirmButton>
    );
  }
}

export default withRouter(DeleteQualityGateForm);
