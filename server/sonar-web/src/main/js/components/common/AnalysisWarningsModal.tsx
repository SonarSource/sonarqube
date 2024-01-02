/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { dismissAnalysisWarning, getTask } from '../../api/ce';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { ButtonLink } from '../../components/controls/buttons';
import Modal from '../../components/controls/Modal';
import WarningIcon from '../../components/icons/WarningIcon';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { translate } from '../../helpers/l10n';
import { sanitizeStringRestricted } from '../../helpers/sanitize';
import { TaskWarning } from '../../types/tasks';
import { CurrentUser } from '../../types/users';

interface Props {
  componentKey?: string;
  currentUser: CurrentUser;
  onClose: () => void;
  onWarningDismiss?: () => void;
  taskId?: string;
  warnings?: TaskWarning[];
}

interface State {
  loading: boolean;
  dismissedWarning?: string;
  warnings: TaskWarning[];
}

export class AnalysisWarningsModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: !props.warnings,
      warnings: props.warnings || [],
    };
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

  handleDismissMessage = async (messageKey: string) => {
    const { componentKey } = this.props;

    if (componentKey === undefined) {
      return;
    }

    this.setState({ dismissedWarning: messageKey });

    try {
      await dismissAnalysisWarning(componentKey, messageKey);

      if (this.props.onWarningDismiss) {
        this.props.onWarningDismiss();
      }
    } catch (e) {
      // Noop
    }

    if (this.mounted) {
      this.setState({ dismissedWarning: undefined });
    }
  };

  loadWarnings = async (taskId: string) => {
    this.setState({ loading: true });
    try {
      const { warnings = [] } = await getTask(taskId, ['warnings']);

      if (this.mounted) {
        this.setState({
          loading: false,
          warnings: warnings.map((w) => ({ key: w, message: w, dismissable: false })),
        });
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ loading: false });
      }
    }
  };

  render() {
    const { currentUser } = this.props;
    const { loading, dismissedWarning, warnings } = this.state;

    const header = translate('warnings');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <div className="modal-body modal-container js-analysis-warnings">
          <DeferredSpinner loading={loading}>
            {warnings.map(({ dismissable, key, message }) => (
              <div className="panel panel-vertical" key={key}>
                <WarningIcon className="pull-left spacer-right" />
                <div className="overflow-hidden markdown">
                  <span
                    // eslint-disable-next-line react/no-danger
                    dangerouslySetInnerHTML={{
                      __html: sanitizeStringRestricted(message.trim().replace(/\n/g, '<br />')),
                    }}
                  />

                  {dismissable && currentUser.isLoggedIn && (
                    <div className="spacer-top display-flex-inline">
                      <ButtonLink
                        disabled={Boolean(dismissedWarning)}
                        onClick={() => {
                          this.handleDismissMessage(key);
                        }}
                      >
                        {translate('dismiss_permanently')}
                      </ButtonLink>
                      {dismissedWarning === key && <i className="spinner spacer-left" />}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </DeferredSpinner>
        </div>

        <footer className="modal-foot">
          <ButtonLink className="js-modal-close" onClick={this.props.onClose}>
            {translate('close')}
          </ButtonLink>
        </footer>
      </Modal>
    );
  }
}

export default withCurrentUserContext(AnalysisWarningsModal);
