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
import $ from 'jquery';
import _ from 'underscore';
import ModalForm from '../../components/common/modal-form';
import { applyTemplateToProject } from '../../api/permissions';
import Template from './templates/project-permissions-apply-template.hbs';

export default ModalForm.extend({
  template: Template,

  onRender: function () {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('#project-permissions-template').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    var that = this;
    this.disableForm();

    var projects = this.options.project ? [this.options.project] : this.options.projects,
        permissionTemplate = this.$('#project-permissions-template').val(),
        looper = $.Deferred().resolve();

    projects.forEach(function (project) {
      looper = looper.then(function () {
        return applyTemplateToProject({
          data: { projectId: project.id, templateId: permissionTemplate }
        });
      });
    });

    looper.done(function () {
      that.options.refresh();
      that.destroy();
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  },

  serializeData: function () {
    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      permissionTemplates: this.options.permissionTemplates,
      project: this.options.project,
      projectsCount: _.size(this.options.projects)
    });
  }
});
