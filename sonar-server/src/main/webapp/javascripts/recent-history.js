window.Sonar = {};

Sonar.RecentHistory = function () {
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
};
  
Sonar.RecentHistory.prototype.add = function (resourceKey, resourceName, iconPath) {
  var sonarHistory = this.getRecentHistory();
  
  if (resourceKey != '') {
    var newEntry = {'key': resourceKey, 'name': resourceName, 'iconPath': iconPath};
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

Sonar.RecentHistory.prototype.populateRecentHistoryPanel = function () {
  var historyLinksList = $j('#recent-history-list');
  historyLinksList.empty();
  
  var recentHistory = this.getRecentHistory();  
  if (recentHistory.length == 0) {
    $j("#recent-history").hide();
  } else {    
    recentHistory.forEach(function (resource) {
      historyLinksList.append('<li><img width="16" height="16" src="'
                            + baseUrl
                            + resource['iconPath']
                            + '"><a href="'
                            + baseUrl
                            + '/dashboard/index/'
                            + resource['key']
                            + '"> ' 
                            + resource['name'] 
                            + '</a></li>');
    });
    $j("#recent-history").show();
  }
};
