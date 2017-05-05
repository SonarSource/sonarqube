/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ModalForm from '../../../../components/common/modal-form';
import { applyTemplateToProject, getPermissionTemplates } from '../../../../api/permissions';
import Template from '../templates/ApplyTemplateTemplate.hbs';

export default ModalForm.extend({
  template: Template,

  initialize() {
    this.loadPermissionTemplates();
    this.done = false;
  },

  loadPermissionTemplates() {
    return getPermissionTemplates(this.options.organization.key).then(r => {
      this.permissionTemplates = r.permissionTemplates;
      this.render();
    });
  },

  onRender() {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('#project-permissions-template').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    const permissionTemplate = this.$('#project-permissions-template').val();
    this.disableForm();

    const data = {
      organization: this.options.organization.key,
      projectKey: this.options.project.key,
      templateId: permissionTemplate
    };
    applyTemplateToProject(data)
      .then(() => {
        this.trigger('done');
        this.done = true;
        this.render();
      })
      .catch(function(e) {
        e.response.json().then(r => {
          this.showErrors(r.errors, r.warnings);
          this.enableForm();
        });
      });
  },

  serializeData() {
    return {
      permissionTemplates: this.permissionTemplates,
      project: this.options.project,
      done: this.done
    };
  }
});
