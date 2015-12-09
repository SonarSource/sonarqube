import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/quality-gate-detail-projects.hbs';
import '../../components/common/select-list';

export default Marionette.ItemView.extend({
  template: Template,

  onRender: function () {
    if (!this.model.isDefault()) {
      new window.SelectList({
        el: this.$('#select-list-projects'),
        width: '100%',
        readOnly: !this.options.canEdit,
        focusSearch: false,
        format: function (item) {
          return item.name;
        },
        searchUrl: baseUrl + '/api/qualitygates/search?gateId=' + this.model.id,
        selectUrl: baseUrl + '/api/qualitygates/select',
        deselectUrl: baseUrl + '/api/qualitygates/deselect',
        extra: {
          gateId: this.model.id
        },
        selectParameter: 'projectId',
        selectParameterValue: 'id',
        labels: {
          selected: window.t('quality_gates.projects.with'),
          deselected: window.t('quality_gates.projects.without'),
          all: window.t('quality_gates.projects.all'),
          noResults: window.t('quality_gates.projects.noResults')
        },
        tooltips: {
          select: window.t('quality_gates.projects.select_hint'),
          deselect: window.t('quality_gates.projects.deselect_hint')
        }
      });
    }
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit
    });
  }
});
