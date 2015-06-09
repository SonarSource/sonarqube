define([
  'components/issue/models/issue'
], function (Issue) {

  return Issue.extend({
    reset: function (attrs, options) {
      // TODO remove me soon
      var keepFields = ['index', 'selected', 'componentUuid', 'componentLongName', 'componentQualifier',
                        'projectLongName', 'projectUuid', 'ruleName', 'comments'];
      keepFields.forEach(function (field) {
        attrs[field] = this.get(field);
      }.bind(this));
      return this._super(attrs, options);
    }
  });

});
