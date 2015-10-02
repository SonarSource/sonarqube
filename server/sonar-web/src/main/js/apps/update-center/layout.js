import Marionette from 'backbone.marionette';
import Template from './templates/update-center-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    headerRegion: '#update-center-header',
    searchRegion: '#update-center-search',
    listRegion: '#update-center-plugins',
    footerRegion: '#update-center-footer'
  }
});


