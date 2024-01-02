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
import { Project, bulkDeleteProjects } from '../../api/project-management';
import Modal from '../../components/controls/Modal';
import { ResetButtonLink, SubmitButton } from '../../components/controls/buttons';
import { Alert } from '../../components/ui/Alert';
import { toISO8601WithOffsetString } from '../../helpers/dates';
import { translate, translateWithParameters } from '../../helpers/l10n';

export interface Props {
  analyzedBefore: Date | undefined;
  onClose: () => void;
  onConfirm: () => void;
  provisioned: boolean;
  qualifier: string;
  query: string;
  selection: Project[];
  total: number;
}

interface State {
  loading: boolean;
}

export default class DeleteModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleConfirmClick = () => {
    this.setState({ loading: true });
    const { analyzedBefore } = this.props;
    const parameters = this.props.selection.length
      ? {
          projects: this.props.selection.map((s) => s.key).join(),
        }
      : {
          analyzedBefore: analyzedBefore && toISO8601WithOffsetString(analyzedBefore),
          onProvisionedOnly: this.props.provisioned || undefined,
          qualifiers: this.props.qualifier,
          q: this.props.query || undefined,
        };
    bulkDeleteProjects(parameters).then(
      () => {
        if (this.mounted) {
          this.props.onConfirm();
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  renderWarning = () => (
    <Alert variant="warning">
      {this.props.selection.length
        ? translateWithParameters(
            'projects_management.delete_selected_warning',
            this.props.selection.length,
          )
        : translateWithParameters('projects_management.delete_all_warning', this.props.total)}
    </Alert>
  );

  render() {
    const header = translate('qualifiers.delete', this.props.qualifier);

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <div className="modal-body">
          {this.renderWarning()}
          {translate('qualifiers.delete_confirm', this.props.qualifier)}
        </div>

        <footer className="modal-foot">
          {this.state.loading && <i className="spinner spacer-right" />}
          <SubmitButton
            className="button-red"
            disabled={this.state.loading}
            onClick={this.handleConfirmClick}
          >
            {translate('delete')}
          </SubmitButton>
          <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
