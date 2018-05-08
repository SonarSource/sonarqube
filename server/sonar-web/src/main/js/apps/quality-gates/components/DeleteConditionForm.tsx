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
import { deleteCondition } from '../../../api/quality-gates';
import { Metric, Condition } from '../../../app/types';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { Button } from '../../../components/ui/buttons';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  condition: Condition;
  metric: Metric;
  onDelete: (condition: Condition) => void;
  organization?: string;
}

export default class DeleteConditionForm extends React.PureComponent<Props> {
  onDelete = () => {
    const { organization, condition } = this.props;
    if (condition.id !== undefined) {
      return deleteCondition({ id: condition.id, organization }).then(() =>
        this.props.onDelete(condition)
      );
    }
    return undefined;
  };

  render() {
    return (
      <ConfirmButton
        confirmButtonText={translate('delete')}
        isDestructive={true}
        modalBody={translateWithParameters(
          'quality_gates.delete_condition.confirm.message',
          getLocalizedMetricName(this.props.metric)
        )}
        modalHeader={translate('quality_gates.delete_condition')}
        onConfirm={this.onDelete}>
        {({ onClick }) => (
          <Button className="delete-condition little-spacer-left button-red" onClick={onClick}>
            {translate('delete')}
          </Button>
        )}
      </ConfirmButton>
    );
  }
}
