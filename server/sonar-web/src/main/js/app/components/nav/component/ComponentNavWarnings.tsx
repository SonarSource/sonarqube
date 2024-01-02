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
import { FormattedMessage } from 'react-intl';
import AnalysisWarningsModal from '../../../../components/common/AnalysisWarningsModal';
import { Alert } from '../../../../components/ui/Alert';
import { translate } from '../../../../helpers/l10n';
import { TaskWarning } from '../../../../types/tasks';

interface Props {
  componentKey: string;
  isBranch: boolean;
  onWarningDismiss: () => void;
  warnings: TaskWarning[];
}

interface State {
  modal: boolean;
}

export default class ComponentNavWarnings extends React.PureComponent<Props, State> {
  state: State = { modal: false };

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ modal: true });
  };

  handleCloseModal = () => {
    this.setState({ modal: false });
  };

  render() {
    return (
      <>
        <Alert className="js-component-analysis-warnings flex-1" display="inline" variant="warning">
          <FormattedMessage
            defaultMessage={translate('component_navigation.last_analysis_had_warnings')}
            id="component_navigation.last_analysis_had_warnings"
            values={{
              branchType: this.props.isBranch
                ? translate('branches.branch')
                : translate('branches.pr'),
              warnings: (
                <a href="#" onClick={this.handleClick}>
                  <FormattedMessage
                    defaultMessage={translate('component_navigation.x_warnings')}
                    id="component_navigation.x_warnings"
                    values={{
                      warningsCount: this.props.warnings.length,
                    }}
                  />
                </a>
              ),
            }}
          />
        </Alert>
        {this.state.modal && (
          <AnalysisWarningsModal
            componentKey={this.props.componentKey}
            onClose={this.handleCloseModal}
            onWarningDismiss={this.props.onWarningDismiss}
            warnings={this.props.warnings}
          />
        )}
      </>
    );
  }
}
