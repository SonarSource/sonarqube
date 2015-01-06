define([
    'backbone'
], function (Backbone) {

  return Backbone.Model.extend({
    idAttribute: 'key',

    addExtraAttributes: function (repositories) {
      var repo = _.findWhere(repositories, { key: this.get('repo') }) || this.get('repo'),
          repoName = repo != null ? repo.name : repo,
          isManual = this.get('repo') === 'manual',
          isCustom = this.has('templateKey');
      this.set({
        repoName: repoName,
        isManual: isManual,
        isCustom: isCustom
      }, { silent: true });
    }
  });

});
