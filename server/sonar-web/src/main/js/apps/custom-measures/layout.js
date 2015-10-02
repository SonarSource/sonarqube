import Marionette from 'backbone.marionette';
import Template from './templates/custom-measures-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    headerRegion: '#custom-measures-header',
    listRegion: '#custom-measures-list',
    listFooterRegion: '#custom-measures-list-footer'
  }
});


