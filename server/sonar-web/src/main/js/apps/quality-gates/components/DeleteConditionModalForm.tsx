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
import Modal from '../../../components/controls/Modal';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate, translateWithParameters, getLocalizedMetricName } from '../../../helpers/l10n';
import { Condition, Metric } from '../../../app/types';
import { deleteCondition } from '../../../api/quality-gates';

interface Props {
  onClose: () => void;
  condition: Condition;
  metric: Metric;
  onDelete: (condition: Condition) => void;
  organization?: string;
}

interface State {
  loading: boolean;
  name: string | null;
}

export default class DeleteConditionModalForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false, name: null };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    const { organization, condition } = this.props;
    if (condition.id !== undefined) {
      deleteCondition({ id: condition.id, organization }).then(
        () => this.props.onDelete(condition),
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  render() {
    const header = translate('quality_gates.delete_condition');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="delete-condition-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <div className="js-modal-messages" />
            <p>
              {translateWithParameters(
                'quality_gates.delete_condition.confirm.message',
                getLocalizedMetricName(this.props.metric)
              )}
            </p>
          </div>
          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <SubmitButton
              className="button-red"
              disabled={this.state.loading}
              id="delete-profile-submit">
              {translate('delete')}
            </SubmitButton>
            <ResetButtonLink id="delete-profile-cancel" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
