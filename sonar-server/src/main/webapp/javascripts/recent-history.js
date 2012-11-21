window.Sonar = {};

Sonar.RecentHistory = function (applicationContext) {
  this.appContext = applicationContext;
  this.translations = {};
  this.addTranslation = function (key, value) {
    this.translations[key] = value;
    return this;
  }
};

Sonar.RecentHistory.prototype.getRecentHistory = function() {
  var sonarHistory = localStorage.getItem("sonar_recent_history");
  if (sonarHistory == null) {
    sonarHistory = new Array();
  } else {
    sonarHistory = JSON.parse(sonarHistory);
  }
  return sonarHistory;
};
  
Sonar.RecentHistory.prototype.clear = function () {
  localStorage.clear();
  $("sonar_recent_history_dropdown").hide();
  this.populateRecentHistoryDropDown();
};
  
Sonar.RecentHistory.prototype.add = function (resourceKey, resourceName, resourceQualifier) {
  var sonarHistory = this.getRecentHistory();
  
  if (resourceKey != '') {
    var newEntry = {'key': resourceKey, 'name': resourceName, 'qualifier': resourceQualifier};
    // removes the element of the array if it exists
    for (i = 0; i < sonarHistory.length; i++) {
      var item = sonarHistory[i];
      if (item['key'] == resourceKey) {
        sonarHistory.splice(i, 1);
        break;
      }
    }    
    // then add it to the beginning of the array
    sonarHistory.unshift(newEntry);
    // and finally slice the array to keep only 10 elements
    sonarHistory = sonarHistory.slice(0,10);
    
    localStorage.setItem("sonar_recent_history", JSON.stringify(sonarHistory));
  }
};

Sonar.RecentHistory.prototype.populateRecentHistoryDropDown = function () {
  var dropdown = $j('#sonar_recent_history_dropdown');
  dropdown.empty();
  
  var recentHistory = this.getRecentHistory();
  
  recentHistory.forEach(function (resource) {
    dropdown.append('<li><img width="16" height="16" src="'
                          + sonarRecentHistory.appContext
                          + '/images/q/'
                          + resource['qualifier']
                          + '.png"><a href="'
                          + sonarRecentHistory.appContext
                          + '/dashboard/index/'
                          + resource['key']
                          + '">' 
                          + resource['name'] 
                          + '</a></li>');
  });
  
  if (recentHistory.length == 0) {
    dropdown.append('<li style="color: #000 !important">' + this.translations['no_history_yet'] + '</li>');
  } else {
    dropdown.append('<li class="clear_list"><a href="#" onclick="sonarRecentHistory.clear(); return false;">' + this.translations['clear'] + '</a></li>');
  }
};
