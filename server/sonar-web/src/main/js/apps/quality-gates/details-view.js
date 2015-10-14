import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Conditions from './conditions';
import DetailConditionsView from './gate-conditions-view';
import ProjectsView from './gate-projects-view';
import Template from './templates/quality-gate-detail.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    conditionsRegion: '#quality-gate-conditions',
    projectsRegion: '#quality-gate-projects'
  },

  modelEvents: {
    'change': 'render'
  },

  onRender: function () {
    this.showConditions();
    this.showProjects();
  },

  orderByName: function (conditions) {
    let metrics = this.options.metrics;
    return _.sortBy(conditions, (condition) => {
      return _.findWhere(metrics, { key: condition.metric }).name;
    });
  },

  showConditions: function () {
    var conditions = new Conditions(this.orderByName(this.model.get('conditions'))),
        view = new DetailConditionsView({
          canEdit: this.options.canEdit,
          collection: conditions,
          model: this.model,
          metrics: this.options.metrics,
          periods: this.options.periods
        });
    this.conditionsRegion.show(view);
  },

  showProjects: function () {
    var view = new ProjectsView({
      canEdit: this.options.canEdit,
      model: this.model
    });
    this.projectsRegion.show(view);
  }
});


