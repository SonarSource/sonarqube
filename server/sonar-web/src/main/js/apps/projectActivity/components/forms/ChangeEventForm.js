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
  project: string,
  onClose: () => void
};

type State = {
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

  closeForm = () => {
    if (this.mounted) {
      this.setState({ name: this.props.event.name });
    }
    this.props.onClose();
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
      this.setState({ processing: false });
    }
    this.props.onClose();
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
          <form onSubmit={this.handleSubmit}>
            <div className="spacer-bottom">
              <input
                  value={this.state.name}
                  autoFocus={true}
                  disabled={this.state.processing}
                  className="input-medium"
                  type="text"
                  onChange={this.changeInput}/>
            </div>
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
        </div>
    );
  }
}
