import Backbone from 'backbone';

<<<<<<< d3fd3a3175fac49d0c2874dc33e06497d4505de1
export default Backbone.Model.extend({
  idAttribute: 'key',

  getDuration: function () {
    var duration = null;
    if (this.has('startedAt')) {
      var startedAtMoment = moment(this.get('startedAt')),
          finishedAtMoment = moment(this.get('finishedAt') || new Date()),
          diff = finishedAtMoment.diff(startedAtMoment);
      duration = {
        seconds: Math.floor(diff / 1000) % 60,
        minutes: Math.floor(diff / (1000 * 60)) % 60,
        hours: Math.floor(diff / (1000 * 60 * 60)) % 24
      };
=======
  return Backbone.Model.extend({
    getDuration: function () {
      var duration = null;
      if (this.has('startedAt')) {
        var startedAtMoment = moment(this.get('startedAt')),
            finishedAtMoment = moment(this.get('finishedAt') || new Date()),
            diff = finishedAtMoment.diff(startedAtMoment);
        duration = {
          seconds: Math.floor(diff / 1000) % 60,
          minutes: Math.floor(diff / (1000 * 60)) % 60,
          hours: Math.floor(diff / (1000 * 60 * 60)) % 24
        };
      }
      return duration;
    },

    isDanger: function () {
      var dangerStatuses = ['CANCELLED', 'FAILED'];
      return dangerStatuses.indexOf(this.get('status')) !== -1;
>>>>>>> SONAR-6834 use new API
    }
    return duration;
  },

  isDanger: function () {
    var dangerStatuses = ['CANCELLED', 'FAILED'];
    return dangerStatuses.indexOf(this.get('status')) !== -1;
  }
});


