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
import { bulkApplyTemplate, getPermissionTemplates } from '../../api/permissions';
import { Project } from '../../api/project-management';
import Modal from '../../components/controls/Modal';
import Select from '../../components/controls/Select';
import { ResetButtonLink, SubmitButton } from '../../components/controls/buttons';
import { Alert } from '../../components/ui/Alert';
import MandatoryFieldMarker from '../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../components/ui/MandatoryFieldsExplanation';
import { toISO8601WithOffsetString } from '../../helpers/dates';
import { addGlobalErrorMessageFromAPI } from '../../helpers/globalMessages';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { PermissionTemplate } from '../../types/types';

export interface Props {
  analyzedBefore: Date | undefined;
  onClose: () => void;
  provisioned: boolean;
  qualifier: string;
  query: string;
  selection: Project[];
  total: number;
}

interface State {
  done: boolean;
  loading: boolean;
  permissionTemplate?: string;
  permissionTemplates?: PermissionTemplate[];
  submitting: boolean;
}

export default class BulkApplyTemplateModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { done: false, loading: true, submitting: false };

  componentDidMount() {
    this.mounted = true;
    this.loadPermissionTemplates();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadPermissionTemplates() {
    this.setState({ loading: true });
    getPermissionTemplates().then(
      ({ permissionTemplates }) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            permissionTemplate:
              permissionTemplates.length > 0 ? permissionTemplates[0].id : undefined,
            permissionTemplates,
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  }

  handleConfirmClick = () => {
    const { analyzedBefore } = this.props;
    const { permissionTemplate } = this.state;
    if (permissionTemplate) {
      this.setState({ submitting: true });
      const selection = this.props.selection.filter((s) => !s.managed);
      const parameters = selection.length
        ? {
            projects: selection.map((s) => s.key).join(),
            qualifiers: this.props.qualifier,
            templateId: permissionTemplate,
          }
        : {
            analyzedBefore: analyzedBefore && toISO8601WithOffsetString(analyzedBefore),
            onProvisionedOnly: this.props.provisioned || undefined,
            qualifiers: this.props.qualifier,
            q: this.props.query || undefined,
            templateId: permissionTemplate,
          };
      bulkApplyTemplate(parameters).then(
        () => {
          if (this.mounted) {
            this.setState({ done: true, submitting: false });
          }
        },
        (error) => {
          addGlobalErrorMessageFromAPI(error);
          if (this.mounted) {
            this.setState({ submitting: false });
          }
        },
      );
    }
  };

  handlePermissionTemplateChange = ({ value }: { value: string }) => {
    this.setState({ permissionTemplate: value });
  };

  renderWarning = () => {
    const { selection } = this.props;

    const managedProjects = selection.filter((s) => s.managed);
    const localProjects = selection.filter((s) => !s.managed);
    const isSelectionOnlyManaged = !!managedProjects.length && !localProjects.length;
    const isSelectionOnlyLocal = !managedProjects.length && !!localProjects.length;

    if (isSelectionOnlyManaged) {
      return (
        <Alert variant="error">
          {translate(
            'permission_templates.bulk_apply_permission_template.apply_to_only_github_projects',
          )}
        </Alert>
      );
    } else if (isSelectionOnlyLocal) {
      return (
        <Alert variant="warning">
          {this.props.selection.length
            ? translateWithParameters(
                'permission_templates.bulk_apply_permission_template.apply_to_selected',
                this.props.selection.length,
              )
            : translateWithParameters(
                'permission_templates.bulk_apply_permission_template.apply_to_all',
                this.props.total,
              )}
        </Alert>
      );
    }
    return (
      <Alert variant="warning">
        {translateWithParameters(
          'permission_templates.bulk_apply_permission_template.apply_to_selected',
          localProjects.length,
        )}
        <br />
        {translateWithParameters(
          'permission_templates.bulk_apply_permission_template.apply_to_github_projects',
          managedProjects.length,
        )}
      </Alert>
    );
  };

  renderSelect = (isSelectionOnlyManaged: boolean) => {
    const options =
      this.state.permissionTemplates !== undefined
        ? this.state.permissionTemplates.map((t) => ({ label: t.name, value: t.id }))
        : [];
    return (
      <div className="modal-field">
        <label htmlFor="bulk-apply-template-input">
          {translate('template')}
          <MandatoryFieldMarker />
        </label>
        <Select
          id="bulk-apply-template"
          inputId="bulk-apply-template-input"
          isDisabled={this.state.submitting || isSelectionOnlyManaged}
          onChange={this.handlePermissionTemplateChange}
          options={options}
          value={options.find((option) => option.value === this.state.permissionTemplate)}
        />
      </div>
    );
  };

  render() {
    const { done, loading, permissionTemplates, submitting } = this.state;
    const header = translate('permission_templates.bulk_apply_permission_template');

    const isSelectionOnlyManaged = this.props.selection.every((s) => s.managed === true);

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="small">
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <div className="modal-body">
          {done && (
            <Alert variant="success">{translate('projects_role.apply_template.success')}</Alert>
          )}

          {loading && <i className="spinner" />}

          {!loading && !done && permissionTemplates && (
            <>
              <MandatoryFieldsExplanation className="spacer-bottom" />
              {this.renderWarning()}
              {this.renderSelect(isSelectionOnlyManaged)}
            </>
          )}
        </div>

        <footer className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          {!loading && !done && permissionTemplates && (
            <SubmitButton
              disabled={submitting || isSelectionOnlyManaged}
              onClick={this.handleConfirmClick}
            >
              {translate('apply')}
            </SubmitButton>
          )}
          <ResetButtonLink onClick={this.props.onClose}>
            {done ? translate('close') : translate('cancel')}
          </ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
