define([
    'backbone'
], function (Backbone) {

  return Backbone.Model.extend({
    idAttribute: 'key',

    addExtraAttributes: function (languages, repositories) {
      var langName = languages[this.get('lang')] || this.get('lang'),
          repo = _.findWhere(repositories, { key: this.get('repo') }) || this.get('repo'),
          isManual = this.get('repo') === 'manual',
          isCustom = this.has('templateKey');
      this.set({
        langName: langName,
        repo: repo,
        isManual: isManual,
        isCustom: isCustom
      });
    }
  });

});
