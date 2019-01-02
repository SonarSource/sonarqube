/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import DeferredSpinner from './DeferredSpinner';
import Modal from '../controls/Modal';
import { ResetButtonLink } from '../ui/buttons';
import { translate } from '../../helpers/l10n';
import WarningIcon from '../icons-components/WarningIcon';
import { getTask } from '../../api/ce';

interface Props {
  onClose: () => void;
  taskId?: string;
  warnings?: string[];
}

interface State {
  loading: boolean;
  warnings: string[];
}

export default class AnalysisWarningsModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { loading: !props.warnings, warnings: props.warnings || [] };
  }

  componentDidMount() {
    this.mounted = true;
    if (!this.props.warnings && this.props.taskId) {
      this.loadWarnings(this.props.taskId);
    }
  }

  componentDidUpdate(prevProps: Props) {
    const { taskId, warnings } = this.props;
    if (!warnings && taskId && prevProps.taskId !== taskId) {
      this.loadWarnings(taskId);
    } else if (warnings && prevProps.warnings !== warnings) {
      this.setState({ warnings });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadWarnings(taskId: string) {
    this.setState({ loading: true });
    getTask(taskId, ['warnings']).then(
      ({ warnings = [] }) => {
        if (this.mounted) {
          this.setState({ loading: false, warnings });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  keepLineBreaks = (warning: string) => {
    if (warning.includes('\n')) {
      const lines = warning.split('\n');
      return (
        <>
          {lines.map((line, index) => (
            <React.Fragment key={index}>
              {line}
              {index < lines.length - 1 && <br />}
            </React.Fragment>
          ))}
        </>
      );
    } else {
      return warning;
    }
  };

  render() {
    const header = translate('warnings');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <div className="modal-body modal-container js-analysis-warnings">
          <DeferredSpinner loading={this.state.loading}>
            {this.state.warnings.map((warning, index) => (
              <div className="panel panel-vertical" key={index}>
                <WarningIcon className="pull-left spacer-right" />
                <div className="overflow-hidden markdown">{this.keepLineBreaks(warning)}</div>
              </div>
            ))}
          </DeferredSpinner>
        </div>

        <footer className="modal-foot">
          <ResetButtonLink className="js-modal-close" onClick={this.props.onClose}>
            {translate('close')}
          </ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
