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
import type { Analysis, Event } from '../../../../store/projectActivity/duck';
import { translate } from '../../../../helpers/l10n';

type Props = {
  analysis: Analysis,
  deleteEvent: () => Promise<*>,
  event: Event,
  project: string,
  removeEventButtonText: string,
  removeEventQuestion: string,
  onClose: () => void
};

type State = {
  processing: boolean
}

export default class RemoveVersionForm extends React.Component {
  mounted: boolean;
  props: Props;

  state: State = {
    processing: false
  };

  componentDidMount () {
    this.mounted = true;
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  closeForm = () => {
    this.props.onClose();
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
    this.props.deleteEvent(this.props.project, this.props.analysis, this.props.event.key)
        .then(this.stopProcessingAndClose, this.stopProcessing);
  };

  render () {
    return (
        <div className="project-activity-analysis-form">
          <form onSubmit={this.handleSubmit}>
            <div className="spacer-bottom">
              {translate(this.props.removeEventQuestion)}
            </div>
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
        </div>
    );
  }
}
