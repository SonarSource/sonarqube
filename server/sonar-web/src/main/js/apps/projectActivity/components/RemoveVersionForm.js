/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import { removeVersion } from '../actions';
import type { Analysis, Event } from '../../../store/projectActivity/duck';
import { translate } from '../../../helpers/l10n';

class RemoveVersionForm extends React.Component {
  mounted: boolean;
  props: {
    removeVersion: () => Promise<*>,
    analysis: Analysis,
    event: Event,
    project: string
  };

  state = {
    open: false,
    processing: false,
    version: ''
  };

  componentDidMount () {
    this.mounted = true;
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  openForm = () => {
    if (this.mounted) {
      this.setState({ open: true });
    }
  };

  closeForm = () => {
    if (this.mounted) {
      this.setState({ open: false, version: '' });
    }
  };

  stopProcessing = () => {
    if (this.mounted) {
      this.setState({ processing: false });
    }
  };

  stopProcessingAndClose = () => {
    if (this.mounted) {
      this.setState({ open: false, processing: false });
    }
  };

  handleSubmit = e => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props.removeVersion(this.props.project, this.props.analysis.key, this.props.event.key)
        .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render () {
    return (
        <div className="project-activity-analysis-form">
          {this.state.open ? (
                  <form onSubmit={this.handleSubmit}>
                    <span className="spacer-right">
                      {translate('project_activity.remove_version.question')}
                    </span>
                    {this.state.processing ? (
                            <i className="spinner"/>
                        ) : (
                            <span>
                              <button type="submit" className="button-red">{translate('remove')}</button>
                              <button type="reset" className="button-link spacer-left" onClick={this.closeForm}>
                                {translate('cancel')}
                              </button>
                            </span>
                        )}
                  </form>
              ) : (
                  <button className="button-red" onClick={this.openForm}>
                    {translate('project_activity.remove_version')}
                  </button>
              )}
        </div>
    );
  }
}

const mapStateToProps = null;

const mapDispatchToProps = { removeVersion };

export default connect(mapStateToProps, mapDispatchToProps)(RemoveVersionForm);
