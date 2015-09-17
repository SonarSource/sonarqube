import Marionette from 'backbone.marionette';
import Conditions from './conditions';
import DetailConditionsView from './gate-conditions-view';
import ProjectsView from './gate-projects-view';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['quality-gate-detail'],

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

  showConditions: function () {
    var conditions = new Conditions(this.model.get('conditions')),
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


