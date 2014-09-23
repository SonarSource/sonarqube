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
      var props = this.getWidgetProps();
      $.get(`${baseUrl}/widget/show?id=${this.model.id}&${props}`, (html) => {
        this.model.set('html', html);
        this.render();
      });
    }

    getWidgetProps() {
      var props = this.model.get('props')
          .map(function (prop) {
            return `${prop.key}=${encodeURIComponent(prop.value)}`;
          })
          .join('&');
      return props;
    }

    serializeData() {
      var props = this.getWidgetProps();
      return _.extend(super.serializeData(), {
        url: `${baseUrl}/widget?id=${this.model.id}&${props}`
      });
    }

  }

  WidgetView.prototype.template = Templates['widget']

  return WidgetView;

});
