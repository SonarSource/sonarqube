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
import ModalForm from '../../components/common/modal-form';
import { createProject } from '../../api/components';
import Template from './templates/projects-create-form.hbs';

export default ModalForm.extend({
  template: Template,

  onRender() {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy() {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  sendRequest() {
    const data = {
      name: this.$('#create-project-name').val(),
      branch: this.$('#create-project-branch').val(),
      project: this.$('#create-project-key').val()
    };
    if (this.options.organization) {
      data.organization = this.options.organization.key;
    }
    this.disableForm();
    return createProject(data)
      .then(project => {
        if (this.options.refresh) {
          this.options.refresh();
        }
        this.enableForm();
        this.createdProject = project;
        this.render();
      })
      .catch(error => {
        this.enableForm();
        error.response.json().then(r => this.showErrors(r.errors, r.warnings));
      });
  },

  serializeData() {
    return {
      ...ModalForm.prototype.serializeData.apply(this, arguments),
      createdProject: this.createdProject
    };
  }
});
