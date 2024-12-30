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
import {
  ButtonPrimary,
  FlagMessage,
  FormField,
  InputSelect,
  LabelValueSelectOption,
  Modal,
} from '~design-system';
import { applyTemplateToProject, getPermissionTemplates } from '../../../../api/permissions';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { PermissionTemplate } from '../../../../types/types';

interface Props {
  onApply?: () => void;
  onClose: () => void;
  project: { key: string; name: string; organization: string };
}

interface State {
  done: boolean;
  loading: boolean;
  permissionTemplate?: string;
  permissionTemplates?: PermissionTemplate[];
}

export default class ApplyTemplate extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { done: false, loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchPermissionTemplates();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPermissionTemplates = () => {
    getPermissionTemplates(this.props.project.organization).then(
      ({ permissionTemplates }) => {
        if (this.mounted) {
          this.setState({ loading: false, permissionTemplates });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (this.state.permissionTemplate) {
      this.setState({ loading: true });
      return applyTemplateToProject({
        projectKey: this.props.project.key,
        templateId: this.state.permissionTemplate,
      }).then(() => {
        if (this.mounted) {
          if (this.props.onApply) {
            this.props.onApply();
          }
          this.setState({ done: true, loading: false });
        }
      });
    }
  };

  handlePermissionTemplateChange = ({ value }: LabelValueSelectOption) => {
    this.setState({ permissionTemplate: value });
  };

  render() {
    const header = translateWithParameters(
      'projects_role.apply_template_to_x',
      this.props.project.name,
    );

    const options = this.state.permissionTemplates
      ? this.state.permissionTemplates.map((permissionTemplate) => ({
          label: permissionTemplate.name,
          value: permissionTemplate.id,
        }))
      : [];

    const FORM_ID = 'project-permissions-apply-template-form';

    return (
      <Modal
        isOverflowVisible
        headerTitle={header}
        onClose={this.props.onClose}
        loading={this.state.loading}
        primaryButton={
          !this.state.done && (
            <ButtonPrimary
              disabled={this.state.loading || !this.state.permissionTemplate}
              type="submit"
              form={FORM_ID}
            >
              {translate('apply')}
            </ButtonPrimary>
          )
        }
        secondaryButtonLabel={translate(this.state.done ? 'close' : 'cancel')}
        body={
          <form id={FORM_ID} onSubmit={this.handleSubmit}>
            <div>
              {this.state.done && (
                <FlagMessage variant="success">
                  {translate('projects_role.apply_template.success')}
                </FlagMessage>
              )}

              {!this.state.done && !this.state.loading && (
                <FormField
                  label={translate('template')}
                  required
                  htmlFor="project-permissions-template-input"
                >
                  {this.state.permissionTemplates && (
                    <InputSelect
                      size="full"
                      id="project-permissions-template"
                      inputId="project-permissions-template-input"
                      onChange={this.handlePermissionTemplateChange}
                      options={options}
                      value={options.filter((o) => o.value === this.state.permissionTemplate)}
                    />
                  )}
                </FormField>
              )}
            </div>
          </form>
        }
      />
    );
  }
}
