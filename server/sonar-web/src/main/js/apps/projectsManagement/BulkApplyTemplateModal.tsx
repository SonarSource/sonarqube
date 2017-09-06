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
import * as React from 'react';
import Modal from 'react-modal';
import * as Select from 'react-select';
import {
  getPermissionTemplates,
  PermissionTemplate,
  bulkApplyTemplate,
  applyTemplateToProject
} from '../../api/permissions';
import { translate, translateWithParameters } from '../../helpers/l10n';

export interface Props {
  onClose: () => void;
  organization: string;
  provisioned: boolean;
  qualifier: string;
  query: string;
  selection: string[];
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
  mounted: boolean;
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
    getPermissionTemplates(this.props.organization).then(
      ({ permissionTemplates }) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            permissionTemplate:
              permissionTemplates.length > 0 ? permissionTemplates[0].id : undefined,
            permissionTemplates: permissionTemplates
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  bulkApplyToAll = (permissionTemplate: string) => {
    const data = {
      organization: this.props.organization,
      q: this.props.query ? this.props.query : undefined,
      qualifier: this.props.qualifier,
      templateId: permissionTemplate
    };
    return bulkApplyTemplate(data);
  };

  bulkApplyToSelected = (permissionTemplate: string) => {
    const { selection } = this.props;
    let lastRequest = Promise.resolve();

    selection.forEach(projectKey => {
      const data = {
        organization: this.props.organization,
        projectKey,
        templateId: permissionTemplate
      };
      lastRequest = lastRequest.then(() => applyTemplateToProject(data));
    });

    return lastRequest;
  };

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleConfirmClick = () => {
    const { permissionTemplate } = this.state;
    if (permissionTemplate) {
      this.setState({ submitting: true });
      const request = this.props.selection.length
        ? this.bulkApplyToSelected(permissionTemplate)
        : this.bulkApplyToAll(permissionTemplate);
      request.then(
        () => {
          if (this.mounted) {
            this.setState({ done: true, submitting: false });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
          }
        }
      );
    }
  };

  handlePermissionTemplateChange = ({ value }: { value: string }) => {
    this.setState({ permissionTemplate: value });
  };

  renderWarning = () => {
    return this.props.selection.length
      ? <div className="alert alert-info">
          {translateWithParameters(
            'permission_templates.bulk_apply_permission_template.apply_to_selected',
            this.props.selection.length
          )}
        </div>
      : <div className="alert alert-warning">
          {translateWithParameters(
            'permission_templates.bulk_apply_permission_template.apply_to_all',
            this.props.total
          )}
        </div>;
  };

  renderSelect = () =>
    <div className="modal-field">
      <label>
        {translate('template')}
        <em className="mandatory">*</em>
      </label>
      <Select
        clearable={false}
        disabled={this.state.submitting}
        onChange={this.handlePermissionTemplateChange}
        options={this.state.permissionTemplates!.map(t => ({ label: t.name, value: t.id }))}
        searchable={false}
        value={this.state.permissionTemplate}
      />
    </div>;

  render() {
    const { done, loading, permissionTemplates, submitting } = this.state;
    const header = translate('permission_templates.bulk_apply_permission_template');

    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>
            {header}
          </h2>
        </header>

        <div className="modal-body">
          {done &&
            <div className="alert alert-success">
              {translate('projects_role.apply_template.success')}
            </div>}

          {loading && <i className="spinner" />}

          {!loading && !done && permissionTemplates && this.renderWarning()}
          {!loading && !done && permissionTemplates && this.renderSelect()}
        </div>

        <footer className="modal-foot">
          {submitting && <i className="spinner spacer-right" />}
          {!loading &&
            !done &&
            permissionTemplates &&
            <button disabled={submitting} onClick={this.handleConfirmClick}>
              {translate('apply')}
            </button>}
          <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
            {done ? translate('close') : translate('cancel')}
          </a>
        </footer>
      </Modal>
    );
  }
}
