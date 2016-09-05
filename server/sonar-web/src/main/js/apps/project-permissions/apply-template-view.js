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
import _ from 'underscore';
import ModalForm from '../../components/common/modal-form';
import { applyTemplateToProject, bulkApplyTemplateToProject } from '../../api/permissions';
import Template from './templates/project-permissions-apply-template.hbs';

export default ModalForm.extend({
  template: Template,

  onRender () {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('#project-permissions-template').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onFormSubmit () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    const that = this;
    const permissionTemplate = this.$('#project-permissions-template').val();
    this.disableForm();

    if (this.options.project) {
      applyTemplateToProject({
        data: { projectId: this.options.project.id, templateId: permissionTemplate }
      }).done(function () {
        that.options.refresh();
        that.destroy();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    } else {
      const data = { templateId: permissionTemplate };
      if (this.options.query) {
        data.q = this.options.query;
      }
      if (this.options.filter && this.options.filter !== '__ALL__') {
        data.qualifier = this.options.filter;
      }

      bulkApplyTemplateToProject({ data }).done(function () {
        that.options.refresh();
        that.destroy();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  },

  serializeData () {
    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      permissionTemplates: this.options.permissionTemplates,
      project: this.options.project,
      projectsCount: _.size(this.options.projects)
    });
  }
});
