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
import { changeVersion } from '../actions';
import type { Analysis, Event } from '../../../store/projectActivity/duck';
import { translate } from '../../../helpers/l10n';


class ChangeVersionForm extends React.Component {
  mounted: boolean;
  props: {
    analysis: Analysis,
    changeVersion: () => Promise<*>,
    event: Event,
    project: string
  };
  state: Object;

  constructor (props) {
    super(props);
    this.state = {
      open: false,
      processing: false,
      version: props.event.name
    };
  }

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
      this.setState({ open: false, version: this.props.event.name });
    }
  };

  changeInput = e => {
    if (this.mounted) {
      this.setState({ version: e.target.value });
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
    this.props.changeVersion(
        this.props.project,
        this.props.analysis.key,
        this.props.event.key,
        this.state.version
    ).then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render () {
    return (
        <div className="project-activity-analysis-form">
          {this.state.open ? (
                  <form onSubmit={this.handleSubmit}>
                    <input
                        value={this.state.version}
                        autoFocus={true}
                        disabled={this.state.processing}
                        className="input-medium little-spacer-right"
                        type="text"
                        onChange={this.changeInput}/>
                    {this.state.processing ? (
                            <i className="spinner"/>
                        ) : (
                            <span>
                              <button type="submit">{translate('save')}</button>
                              <button type="reset" className="button-link spacer-left" onClick={this.closeForm}>
                                {translate('cancel')}
                              </button>
                            </span>
                        )}
                  </form>
              ) : (
                  <button onClick={this.openForm}>{translate('project_activity.change_version')}</button>
              )}
        </div>
    );
  }
}

const mapStateToProps = null;

const mapDispatchToProps = { changeVersion };

export default connect(mapStateToProps, mapDispatchToProps)(ChangeVersionForm);
