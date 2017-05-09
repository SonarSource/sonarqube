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
import React, { Component } from 'react';
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

export default class Details extends Component {
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
    const { qualityGate, onCopy } = this.props;
    const { router } = this.context;

    new CopyView({
      qualityGate,
      onCopy: newQualityGate => {
        onCopy(newQualityGate);
        router.push(`/quality_gates/show/${newQualityGate.id}`);
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
    const { qualityGate, onDelete } = this.props;
    const { router } = this.context;

    new DeleteView({
      qualityGate,
      onDelete: qualityGate => {
        onDelete(qualityGate);
        router.replace('/quality_gates');
      }
    }).render();
  }

  render() {
    const { qualityGate, edit, metrics } = this.props;
    const { onAddCondition, onDeleteCondition, onSaveCondition } = this.props;

    if (!qualityGate) {
      return (
        <div className="search-navigator-workspace">
          <div className="search-navigator-workspace-header" style={{ top: 30 }}>
            <h2 className="search-navigator-header-component">&nbsp;</h2>
          </div>
          <div className="search-navigator-workspace-details" />
        </div>
      );
    }

    return (
      <div className="search-navigator-workspace">
        <Helmet title={qualityGate.name} />
        <DetailsHeader
          qualityGate={qualityGate}
          edit={edit}
          onRename={this.handleRenameClick.bind(this)}
          onCopy={this.handleCopyClick.bind(this)}
          onSetAsDefault={this.handleSetAsDefaultClick.bind(this)}
          onDelete={this.handleDeleteClick.bind(this)}
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
  router: React.PropTypes.object.isRequired
};
