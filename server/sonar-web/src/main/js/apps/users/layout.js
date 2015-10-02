import Marionette from 'backbone.marionette';
import Template from './templates/users-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    headerRegion: '#users-header',
    searchRegion: '#users-search',
    listRegion: '#users-list',
    listFooterRegion: '#users-list-footer'
  }
});


