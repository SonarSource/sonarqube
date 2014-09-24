define([
  'backbone.marionette',
  'templates/dashboard'
], function (Marionette, Templates) {

  var $ = jQuery;


  class WidgetView extends Marionette.ItemView {

    initialize() {
      this.requestContent();
    }

    requestContent() {
      var payload = { id: this.model.id };
      if (this.options.app.resource) {
        payload.resource = this.options.app.resource;
      }
      _.extend(payload, this.getWidgetProps());
      $.get(`${baseUrl}/widget/show`, payload, (html) => {
        this.model.set('html', html);
        this.render();
      });
    }

    getWidgetProps() {
      var props = this.model.get('props'),
          r = {};
      props.forEach(function (prop) {
        r[prop.key] = prop.value;
      });
      return r;
    }

  }

  WidgetView.prototype.template = Templates['widget']

  return WidgetView;

});
