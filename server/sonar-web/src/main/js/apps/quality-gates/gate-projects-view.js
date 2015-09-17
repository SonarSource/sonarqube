import _ from 'underscore';
import Marionette from 'backbone.marionette';
import 'components/common/select-list';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['quality-gate-detail-projects'],

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
          selected: t('quality_gates.projects.with'),
          deselected: t('quality_gates.projects.without'),
          all: t('quality_gates.projects.all'),
          noResults: t('quality_gates.projects.noResults')
        },
        tooltips: {
          select: t('quality_gates.projects.select_hint'),
          deselect: t('quality_gates.projects.deselect_hint')
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


