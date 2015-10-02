import Marionette from 'backbone.marionette';
import Template from './templates/metrics-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    headerRegion: '#metrics-header',
    listRegion: '#metrics-list',
    listFooterRegion: '#metrics-list-footer'
  }
});


