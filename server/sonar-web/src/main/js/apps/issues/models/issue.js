define([
  'components/issue/models/issue'
], function (Issue) {

  return Issue.extend({
    reset: function (attrs, options) {
      var keepFields = ['index', 'selected', 'comments'];
      keepFields.forEach(function (field) {
        attrs[field] = this.get(field);
      }.bind(this));
      Issue.prototype.reset.call(this, attrs, options);
    }
  });

});
