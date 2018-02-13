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
import Form from './Form';
import { CustomMeasure } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';

interface Props {
  measure: CustomMeasure;
  onEdit: (data: { description: string; id: string; value: string }) => Promise<void>;
}

interface State {
  modal: boolean;
}

export default class EditButton extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = () => {
    this.setState({ modal: true });
  };

  handleClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
    }
  };

  handleSubmit = (data: { description: string; value: string }) => {
    return this.props.onEdit({ id: this.props.measure.id, ...data });
  };

  render() {
    return (
      <>
        <ActionsDropdownItem className="js-custom-measure-update" onClick={this.handleClick}>
          {translate('update_verb')}
        </ActionsDropdownItem>
        {this.state.modal && (
          <Form
            confirmButtonText={translate('update_verb')}
            header={translate('custom_measures.update_custom_measure')}
            measure={this.props.measure}
            onClose={this.handleClose}
            onSubmit={this.handleSubmit}
          />
        )}
      </>
    );
  }
}
