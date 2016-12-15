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
import type { Event } from '../../../../store/projectActivity/duck';
import { translate } from '../../../../helpers/l10n';

type Props = {
  analysis: string,
  changeEvent: () => Promise<*>,
  changeEventButtonText: string,
  event: Event,
  project: string
};

type State = {
  open: boolean,
  processing: boolean,
  name: string
}

export default class ChangeEventForm extends React.Component {
  mounted: boolean;
  props: Props;
  state: State;

  constructor (props: Props) {
    super(props);
    this.state = {
      open: false,
      processing: false,
      name: props.event.name
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
      this.setState({ open: false, name: this.props.event.name });
    }
  };

  changeInput = (e: Object) => {
    if (this.mounted) {
      this.setState({ name: e.target.value });
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

  handleSubmit = (e: Object) => {
    e.preventDefault();
    this.setState({ processing: true });
    this.props.changeEvent(
        this.props.project,
        this.props.analysis,
        this.props.event.key,
        this.state.name
    ).then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render () {
    return (
        <div className="project-activity-analysis-form">
          {this.state.open ? (
                  <form onSubmit={this.handleSubmit}>
                    <input
                        value={this.state.name}
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
                  <button className="button-clean" onClick={this.openForm}>
                    <i className="icon-edit"/>
                  </button>
              )}
        </div>
    );
  }
}
