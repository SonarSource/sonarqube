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
import { addVersion } from '../actions';
import type { Analysis } from '../../../store/projectActivity/duck';
import { translate } from '../../../helpers/l10n';

class AddVersionForm extends React.Component {
  mounted: boolean;
  props: {
    addVersion: (analysis: string, version: string) => Promise<*>,
    analysis: Analysis,
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
    this.props.addVersion(this.props.project, this.props.analysis.key, this.state.version)
        .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render () {
    return (
        <div>
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
                  <button onClick={this.openForm}>{translate('project_activity.add_version')}</button>
              )}
        </div>
    );
  }
}

const mapStateToProps = null;

const mapDispatchToProps = { addVersion };

export default connect(mapStateToProps, mapDispatchToProps)(AddVersionForm);
