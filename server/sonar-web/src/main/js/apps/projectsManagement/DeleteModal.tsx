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
import * as React from 'react';
import { FlagMessage, Modal } from '~design-system';
import { Project } from '../../api/project-management';
import { toISO8601WithOffsetString } from '../../helpers/dates';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { deleteBulkProjects } from '../../api/codescan';

export interface Props {
  organization: string;
  analyzedBefore: Date | undefined;
  onClose: () => void;
  onConfirm: () => void;
  provisioned: boolean;
  qualifier: string;
  query: string;
  selection: Project[];
  total: number;
}

interface State {
  loading: boolean;
}

export default class DeleteModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleConfirmClick = () => {
    this.setState({ loading: true });
    const { analyzedBefore } = this.props;
    const parameters = this.props.selection.length
      ? {
          organization: this.props.organization,
          projects: this.props.selection.map((s) => s.key).join(),
        }
      : {
          organization: this.props.organization,
          analyzedBefore: analyzedBefore && toISO8601WithOffsetString(analyzedBefore),
          onProvisionedOnly: this.props.provisioned || undefined,
          qualifiers: this.props.qualifier,
          q: this.props.query || undefined,
        };
    deleteBulkProjects(parameters).then(
      () => {
        if (this.mounted) {
          this.props.onConfirm();
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  renderWarning = () => (
    <FlagMessage variant="warning">
      {this.props.selection.length
        ? translateWithParameters(
            'projects_management.delete_selected_warning',
            this.props.selection.length,
          )
        : translateWithParameters('projects_management.delete_all_warning', this.props.total)}
    </FlagMessage>
  );

  render() {
    const header = translate('qualifiers.delete', this.props.qualifier);

    return (
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        body={
          <>
            {this.renderWarning()}
            <p className="sw-mt-2">
              {translate('qualifiers.delete_confirm', this.props.qualifier)}
            </p>
          </>
        }
        primaryButton={
          <Button
            hasAutoFocus
            isDisabled={this.state.loading}
            onClick={this.handleConfirmClick}
            type="submit"
            variety={ButtonVariety.Danger}
          >
            {translate('delete')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
