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
import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import DetailsHeader from './DetailsHeader';
import DetailsContent from './DetailsContent';
import { fetchQualityGate } from '../../../api/quality-gates';

export default class Details extends React.PureComponent {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  componentDidMount() {
    this.props.fetchMetrics();
    this.fetchDetails();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.params.id !== this.props.params.id) {
      this.fetchDetails();
    }
  }

  fetchDetails = () =>
    fetchQualityGate({
      id: this.props.params.id,
      organization: this.props.organization && this.props.organization.key
    }).then(qualityGate => this.props.onShow(qualityGate), () => {});

  render() {
    const { organization, metrics, qualityGate } = this.props;
    const { onAddCondition, onDeleteCondition, onSaveCondition } = this.props;

    if (!qualityGate) {
      return null;
    }

    return (
      <div className="layout-page-main">
        <Helmet title={qualityGate.name} />
        <DetailsHeader
          qualityGate={qualityGate}
          onRename={this.props.onRename}
          onCopy={this.props.onCopy}
          onSetAsDefault={this.props.onSetAsDefault}
          onDelete={this.props.onDelete}
          organization={organization && organization.key}
        />

        <DetailsContent
          gate={qualityGate}
          metrics={metrics}
          onAddCondition={onAddCondition}
          onSaveCondition={onSaveCondition}
          onDeleteCondition={onDeleteCondition}
          organization={organization && organization.key}
        />
      </div>
    );
  }
}
