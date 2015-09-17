import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import MainView from './main-view';

var App = new Marionette.Application();

App.on('start', function (options) {
  var viewOptions = _.extend(options, {
    model: new Backbone.Model()
  });
  var mainView = new MainView(viewOptions);
  mainView.render().refresh();
});

export default App;


