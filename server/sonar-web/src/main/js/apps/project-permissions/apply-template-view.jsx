import $ from 'jquery';
import _ from 'underscore';
import ModalForm from '../../components/common/modal-form';
import {applyTemplateToProject} from '../../api/permissions';
import './templates';

export default ModalForm.extend({
  template: Templates['project-permissions-apply-template'],

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
