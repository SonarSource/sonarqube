/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import DeleteForm from './DeleteForm';
import Form from './Form';
import MeasureDate from './MeasureDate';
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from '../../../components/controls/ActionsDropdown';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

interface Props {
  measure: T.CustomMeasure;
  onDelete: (measureId: string) => Promise<void>;
  onEdit: (data: { description: string; id: string; value: string }) => Promise<void>;
}

interface State {
  deleteForm: boolean;
  editForm: boolean;
}

export default class Item extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    deleteForm: false,
    editForm: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleEditClick = () => {
    this.setState({ editForm: true });
  };

  handleDeleteClick = () => {
    this.setState({ deleteForm: true });
  };

  closeEditForm = () => {
    if (this.mounted) {
      this.setState({ editForm: false });
    }
  };

  closeDeleteForm = () => {
    if (this.mounted) {
      this.setState({ deleteForm: false });
    }
  };

  handleEditFormSubmit = (data: { description: string; value: string }) => {
    return this.props.onEdit({ id: this.props.measure.id, ...data });
  };

  handleDeleteFormSubmit = () => {
    return this.props.onDelete(this.props.measure.id);
  };

  render() {
    const { measure } = this.props;

    return (
      <tr data-metric={measure.metric.key}>
        <td className="nowrap">
          <div>
            <span className="js-custom-measure-metric-name">{measure.metric.name}</span>
            {measure.pending && (
              <Tooltip overlay={translate('custom_measures.pending_tooltip')}>
                <span className="js-custom-measure-pending badge badge-warning spacer-left">
                  {translate('custom_measures.pending')}
                </span>
              </Tooltip>
            )}
          </div>
          <span className="js-custom-measure-domain note">{measure.metric.domain}</span>
        </td>

        <td className="nowrap">
          <strong className="js-custom-measure-value">
            {formatMeasure(measure.value, measure.metric.type)}
          </strong>
        </td>

        <td>
          <span className="js-custom-measure-description">{measure.description}</span>
        </td>

        <td>
          <MeasureDate measure={measure} /> {translate('by_')}{' '}
          <span className="js-custom-measure-user">{measure.user.name}</span>
        </td>

        <td className="thin nowrap">
          <ActionsDropdown>
            <ActionsDropdownItem
              className="js-custom-measure-update"
              onClick={this.handleEditClick}>
              {translate('update_verb')}
            </ActionsDropdownItem>
            <ActionsDropdownDivider />
            <ActionsDropdownItem
              className="js-custom-measure-delete"
              destructive={true}
              onClick={this.handleDeleteClick}>
              {translate('delete')}
            </ActionsDropdownItem>
          </ActionsDropdown>
        </td>

        {this.state.editForm && (
          <Form
            confirmButtonText={translate('update_verb')}
            header={translate('custom_measures.update_custom_measure')}
            measure={this.props.measure}
            onClose={this.closeEditForm}
            onSubmit={this.handleEditFormSubmit}
          />
        )}

        {this.state.deleteForm && (
          <DeleteForm
            measure={this.props.measure}
            onClose={this.closeDeleteForm}
            onSubmit={this.handleDeleteFormSubmit}
          />
        )}
      </tr>
    );
  }
}
