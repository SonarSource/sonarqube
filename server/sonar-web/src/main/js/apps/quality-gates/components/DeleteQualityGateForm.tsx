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
import Modal from '../../../components/controls/Modal';
import { getQualityGatesUrl } from '../../../helpers/urls';
import { deleteQualityGate, QualityGate } from '../../../api/quality-gates';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onDelete: (qualityGate: QualityGate) => void;
  organization?: string;
  qualityGate: QualityGate;
}

interface State {
  loading: boolean;
}

export default class DeleteQualityGateForm extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object
  };

  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { organization, qualityGate } = this.props;
    this.setState({ loading: true });
    deleteQualityGate({ id: qualityGate.id, organization }).then(
      () => {
        this.props.onDelete(qualityGate);
        this.context.router.replace(getQualityGatesUrl(organization));
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { qualityGate } = this.props;
    const header = translate('quality_gates.delete');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="delete-profile-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <p>
              {translateWithParameters('quality_gates.delete.confirm.message', qualityGate.name)}
            </p>
          </div>
          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <SubmitButton className="js-delete button-red" disabled={this.state.loading}>
              {translate('delete')}
            </SubmitButton>
            <ResetButtonLink className="js-modal-close" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
