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
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import { Button, ButtonLink } from '../../../components/controls/buttons';
import ModalButton, { ModalProps } from '../../../components/controls/ModalButton';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { Condition as ConditionType, Dict, Metric, QualityGate } from '../../../types/types';
import { getWeakAndMissingConditions } from '../utils';
import CaycReviewUpdateConditionsModal from './ConditionReviewAndUpdateModal';
import ConditionsTable from './ConditionsTable';

interface Props {
  canEdit: boolean;
  metrics: Dict<Metric>;
  onAddCondition: (condition: ConditionType) => void;
  onRemoveCondition: (Condition: ConditionType) => void;
  onSaveCondition: (newCondition: ConditionType, oldCondition: ConditionType) => void;
  qualityGate: QualityGate;
  updatedConditionId?: string;
  conditions: ConditionType[];
  scope: 'new' | 'overall' | 'new-cayc';
}

interface State {
  showEdit: boolean;
}

export default class CaycConditions extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { showEdit: false };
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.qualityGate.id !== prevProps.qualityGate.id) {
      this.setState({ showEdit: false });
    }
  }

  toggleEditing = () => {
    const { showEdit } = this.state;
    this.setState({ showEdit: !showEdit });
  };

  renderConfirmModal = ({ onClose }: ModalProps) => (
    <CaycReviewUpdateConditionsModal {...this.props} onClose={onClose} />
  );

  render() {
    const { conditions, canEdit } = this.props;
    const { showEdit } = this.state;
    const { weakConditions, missingConditions } = getWeakAndMissingConditions(conditions);
    const caycDescription = canEdit
      ? `${translate('quality_gates.cayc.description')} ${translate(
          'quality_gates.cayc.description.extended'
        )}`
      : translate('quality_gates.cayc.description');

    return (
      <div className="cayc-conditions-wrapper big-padded big-spacer-top big-spacer-bottom">
        <h4>{translate('quality_gates.cayc')}</h4>
        <div className="big-padded-top big-padded-bottom">
          <FormattedMessage
            id="quality_gates.cayc.description"
            defaultMessage={caycDescription}
            values={{
              link: (
                <DocLink to="/user-guide/clean-as-you-code/">
                  {translate('quality_gates.cayc')}
                </DocLink>
              ),
            }}
          />
        </div>
        {(weakConditions.length > 0 || missingConditions.length > 0) && (
          <Alert className="big-spacer-bottom" variant="warning">
            <h4 className="spacer-bottom cayc-warning-header">
              {translate('quality_gates.cayc_condition.missing_warning.title')}
            </h4>
            <p className="cayc-warning-description">
              {translate('quality_gates.cayc_condition.missing_warning.description')}
            </p>
            {canEdit && (
              <ModalButton modal={this.renderConfirmModal}>
                {({ onClick }) => (
                  <Button className="big-spacer-top spacer-bottom" onClick={onClick}>
                    {translate('quality_gates.cayc_condition.review_update')}
                  </Button>
                )}
              </ModalButton>
            )}
          </Alert>
        )}
        {canEdit && (
          <ButtonLink className="pull-right spacer-right" onClick={this.toggleEditing}>
            {showEdit
              ? translate('quality_gates.cayc.lock_edit')
              : translate('quality_gates.cayc.unlock_edit')}
          </ButtonLink>
        )}

        <div className="big-padded-top">
          <ConditionsTable
            {...this.props}
            showEdit={showEdit}
            missingConditions={missingConditions}
          />
        </div>
      </div>
    );
  }
}
