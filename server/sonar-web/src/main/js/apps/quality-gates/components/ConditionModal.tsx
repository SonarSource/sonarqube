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
import AddConditionSelect from './AddConditionSelect';
import ConditionOperator from './ConditionOperator';
import Modal from '../../../components/controls/Modal';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { Metric } from '../../../app/types';

interface Props {
  metrics: Metric[];
  header: string;
  onClose: () => void;
}

interface State {
  metric: string;
  submitting: boolean;
}

export default class ConditionModal extends React.PureComponent<Props, State> {
  state = { metric: '', submitting: false };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
  };

  handleChooseType = (metric: string) => {
    this.setState({ metric });
  };

  render() {
    const { header, metrics, onClose } = this.props;
    const { submitting } = this.state;
    return (
      <Modal contentLabel={header} onRequestClose={onClose}>
        <form onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="create-user-login">
                {translate('quality_gates.conditions.metric')}
              </label>
              <AddConditionSelect metrics={metrics} onAddCondition={this.handleChooseType} />
            </div>
            <div className="modal-field">
              <label htmlFor="create-user-login">
                {translate('quality_gates.conditions.metric')}
              </label>
              <ConditionOperator
                canEdit={true}
                condition={}
                metric={this.state.metric}
                onOperatorChange={() => {}}
              />
            </div>
          </div>

          <div className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <SubmitButton disabled={submitting} id="coding-rules-custom-rule-creation-reactivate">
              {header}
            </SubmitButton>
            <ResetButtonLink
              disabled={submitting}
              id="coding-rules-custom-rule-creation-cancel"
              onClick={onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
