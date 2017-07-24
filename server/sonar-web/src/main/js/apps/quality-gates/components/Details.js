/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import {
  fetchQualityGate,
  setQualityGateAsDefault,
  unsetQualityGateAsDefault
} from '../../../api/quality-gates';
import DetailsHeader from './DetailsHeader';
import DetailsContent from './DetailsContent';
import RenameView from '../views/rename-view';
import CopyView from '../views/copy-view';
import DeleteView from '../views/delete-view';
import { getQualityGatesUrl, getQualityGateUrl } from '../../../helpers/urls';

export default class Details extends React.PureComponent {
  componentDidMount() {
    this.fetchDetails();
  }

  componentDidUpdate(nextProps) {
    if (nextProps.params.id !== this.props.params.id) {
      this.fetchDetails();
    }
  }

  fetchDetails() {
    const { id } = this.props.params;
    const { onShow } = this.props;

    fetchQualityGate(id).then(qualityGate => {
      onShow(qualityGate);
    });
  }

  handleRenameClick() {
    const { qualityGate, onRename } = this.props;

    new RenameView({
      qualityGate,
      onRename: (qualityGate, newName) => {
        onRename(qualityGate, newName);
      }
    }).render();
  }

  handleCopyClick() {
    const { qualityGate, onCopy, organization } = this.props;
    const { router } = this.context;

    new CopyView({
      qualityGate,
      onCopy: newQualityGate => {
        onCopy(newQualityGate);
        router.push(getQualityGateUrl(newQualityGate.id, organization && organization.key));
      }
    }).render();
  }

  handleSetAsDefaultClick() {
    const { qualityGate, onSetAsDefault, onUnsetAsDefault } = this.props;

    if (qualityGate.isDefault) {
      unsetQualityGateAsDefault(qualityGate.id).then(() => onUnsetAsDefault(qualityGate));
    } else {
      setQualityGateAsDefault(qualityGate.id).then(() => onSetAsDefault(qualityGate));
    }
  }

  handleDeleteClick() {
    const { qualityGate, onDelete, organization } = this.props;
    const { router } = this.context;
    new DeleteView({
      qualityGate,
      onDelete: qualityGate => {
        onDelete(qualityGate);
        router.replace(getQualityGatesUrl(organization && organization.key));
      }
    }).render();
  }

  render() {
    const { qualityGate, edit, metrics } = this.props;
    const { onAddCondition, onDeleteCondition, onSaveCondition } = this.props;

    if (!qualityGate) {
      return null;
    }

    return (
      <div className="layout-page-main">
        <Helmet title={qualityGate.name} />
        <DetailsHeader
          qualityGate={qualityGate}
          edit={edit}
          onRename={this.handleRenameClick.bind(this)}
          onCopy={this.handleCopyClick.bind(this)}
          onSetAsDefault={this.handleSetAsDefaultClick.bind(this)}
          onDelete={this.handleDeleteClick.bind(this)}
          organization={this.props.organization}
        />

        <DetailsContent
          gate={qualityGate}
          canEdit={edit}
          metrics={metrics}
          onAddCondition={onAddCondition}
          onSaveCondition={onSaveCondition}
          onDeleteCondition={onDeleteCondition}
        />
      </div>
    );
  }
}

Details.contextTypes = {
  router: PropTypes.object.isRequired
};
