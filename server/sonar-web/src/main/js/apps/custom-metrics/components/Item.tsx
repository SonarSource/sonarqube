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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DeleteForm from './DeleteForm';
import Form, { MetricProps } from './Form';

interface Props {
  domains?: string[];
  metric: T.Metric;
  onDelete: (metricKey: string) => Promise<void>;
  onEdit: (data: { id: string } & MetricProps) => Promise<void>;
  types?: string[];
}

interface State {
  deleteForm: boolean;
  editForm: boolean;
}

export default class Item extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = { deleteForm: false, editForm: false };

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

  handleEditFormSubmit = (data: MetricProps) => {
    return this.props.onEdit({ id: this.props.metric.id, ...data });
  };

  handleDeleteFormSubmit = () => {
    return this.props.onDelete(this.props.metric.key);
  };

  render() {
    const { domains, metric, types } = this.props;

    return (
      <tr data-metric={metric.key}>
        <td className="width-30">
          <div>
            <strong className="js-metric-name">{metric.name}</strong>
            <span className="js-metric-key note little-spacer-left">{metric.key}</span>
          </div>
        </td>

        <td className="width-20">
          <span className="js-metric-domain">{metric.domain}</span>
        </td>

        <td className="width-20">
          <span className="js-metric-type">{translate('metric.type', metric.type)}</span>
        </td>

        <td className="width-20" title={metric.description}>
          <span className="js-metric-description">{metric.description}</span>
        </td>

        <td className="thin nowrap">
          <ActionsDropdown>
            {domains && types && (
              <ActionsDropdownItem className="js-metric-update" onClick={this.handleEditClick}>
                {translate('update_details')}
              </ActionsDropdownItem>
            )}
            <ActionsDropdownDivider />
            <ActionsDropdownItem
              className="js-metric-delete"
              destructive={true}
              onClick={this.handleDeleteClick}>
              {translate('delete')}
            </ActionsDropdownItem>
          </ActionsDropdown>
        </td>

        {this.state.editForm && domains && types && (
          <Form
            confirmButtonText={translate('update_verb')}
            domains={domains}
            header={translate('custom_metrics.update_metric')}
            metric={metric}
            onClose={this.closeEditForm}
            onSubmit={this.handleEditFormSubmit}
            types={types}
          />
        )}

        {this.state.deleteForm && (
          <DeleteForm
            metric={metric}
            onClose={this.closeDeleteForm}
            onSubmit={this.handleDeleteFormSubmit}
          />
        )}
      </tr>
    );
  }
}
