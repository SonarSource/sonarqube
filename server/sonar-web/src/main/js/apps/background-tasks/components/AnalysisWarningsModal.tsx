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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { FlagMessage, HtmlFormatter, Modal, Spinner } from 'design-system';
import * as React from 'react';
import { dismissAnalysisWarning, getTask } from '../../../api/ce';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { sanitizeStringRestricted } from '../../../helpers/sanitize';
import { TaskWarning } from '../../../types/tasks';
import { CurrentUser } from '../../../types/users';

interface Props {
  componentKey?: string;
  currentUser: CurrentUser;
  onClose: () => void;
  taskId: string;
}

interface State {
  dismissedWarning?: string;
  loading: boolean;
  warnings: TaskWarning[];
}

export class AnalysisWarningsModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: false,
      warnings: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.loadWarnings(this.props.taskId);
  }

  componentDidUpdate(prevProps: Props) {
    const { taskId } = this.props;
    if (prevProps.taskId !== taskId) {
      this.loadWarnings(taskId);
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

    const body = (
      <Spinner loading={loading}>
        {warnings.map(({ dismissable, key, message }) => (
          <React.Fragment key={key}>
            <div className="sw-flex sw-items-center sw-mt-2">
              <FlagMessage variant="warning">
                <HtmlFormatter>
                  <span
                    // eslint-disable-next-line react/no-danger
                    dangerouslySetInnerHTML={{
                      __html: sanitizeStringRestricted(message.trim().replace(/\n/g, '<br />')),
                    }}
                  />
                </HtmlFormatter>
              </FlagMessage>
            </div>
            <div>
              {dismissable && currentUser.isLoggedIn && (
                <div className="sw-mt-4">
                  <Button
                    isDisabled={Boolean(dismissedWarning)}
                    onClick={() => {
                      this.handleDismissMessage(key);
                    }}
                    variety={ButtonVariety.DangerOutline}
                  >
                    {translate('dismiss_permanently')}
                  </Button>

                  <Spinner className="sw-ml-2" loading={dismissedWarning === key} />
                </div>
              )}
            </div>
          </React.Fragment>
        ))}
      </Spinner>
    );

    return (
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        body={body}
        primaryButton={null}
        secondaryButtonLabel={translate('close')}
      />
    );
  }
}

export default withCurrentUserContext(AnalysisWarningsModal);
