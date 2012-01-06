window.SonarWidgets = {};


//******************* STACK AREA CHART ******************* //

SonarWidgets.StackArea = function (divId) {
  this.wDivId = divId;
  this.wHeight;
  this.wData;
  this.wSnapshots;
  this.wMetrics;
  this.wColors;
  this.height = function (height) {
    this.wHeight = height;
    return this;
  };
  this.data = function (data) {
    this.wData = data;
    return this;
  };
  this.snapshots = function (snapshots) {
    this.wSnapshots = snapshots;
    return this;
  };
  this.metrics = function (metrics) {
    this.wMetrics = metrics;
    return this;
  };
  this.colors = function (colors) {
    this.wColors = colors;
    return this;
  }
};

SonarWidgets.StackArea.prototype.render = function () {

  var trendData = this.wData;
  var metrics = this.wMetrics;
  var snapshots = this.wSnapshots;
  var colors = this.wColors;

  var widgetDiv = $(this.wDivId);
  var headerFont = "10.5px Arial,Helvetica,sans-serif";

  /* Computes the total of the trendData of each date */
  var total = [];
  for (i = 0; i < trendData[0].size(); i++) {
    total[i] = 0;
    for (j = 0; j < metrics.size(); j++) {
      total[i] += trendData[j][i].y;
    }
    total[i] = "" + Math.round(total[i] * 10) / 10
  }

  /* Computes the highest Y value */
  var maxY = 0;
  for (i = 0; i < trendData[0].size(); i++) {
    var currentYSum = 0;
    for (j = 0; j < trendData.size(); j++) {
      currentYSum += trendData[j][i].y;
    }
    if (currentYSum > maxY) {
      maxY = currentYSum;
    }
  }

  /* Computes minimum width of left margin according to the max Y value so that the Y-axis is correctly displayed */
  var leftMargin = 25;
  var maxYLength = (Math.round(maxY) + "").length;
  minMargin = maxYLength * 7 + Math.floor(maxYLength / 3) * 2; // first part is for numbers and second for commas (1000-separator)
  if (minMargin > leftMargin) {
    leftMargin = minMargin;
  }

  /* Sizing and scales. */
  var headerHeight = 40;
  var w = widgetDiv.getOffsetParent().getWidth() - leftMargin - 40;
  var h = (this.wHeight == null ? 200 : this.wHeight) + headerHeight;

  var x = pv.Scale.linear(pv.blend(pv.map(trendData, function (d) {
    return d;
  })),
    function (d) {
      return d.x
    }).range(0, w);
  var y = pv.Scale.linear(0, maxY).range(0, h - headerHeight);
  var idx_numbers = trendData[0].size();
  var idx = idx_numbers - 1;

  function computeIdx(xPixels) {
    var mx = x.invert(xPixels);
    var i = pv.search(trendData[0].map(function (d) {
      return d.x;
    }), mx);
    i = i < 0 ? (-i - 2) : i;
    i = i < 0 ? 0 : i;
    return i;
  }

  /* The root panel. */
  var vis = new pv.Panel()
    .canvas(widgetDiv)
    .width(w)
    .height(h)
    .left(leftMargin)
    .right(20)
    .bottom(30)
    .top(20)
    .strokeStyle("#CCC");

  /* X-axis */
  vis.add(pv.Rule)
    .data(x.ticks())
    .left(x)
    .bottom(-10)
    .height(10)
    .anchor("bottom")
    .add(pv.Label)
    .text(x.tickFormat);

  /* Y-axis and ticks. */
  vis.add(pv.Rule)
    .data(y.ticks(6))
    .bottom(y)
    .strokeStyle("rgba(128,128,128,.2)")
    .anchor("left")
    .add(pv.Label)
    .text(y.tickFormat);

  /* The stack layout */
  var area = vis.add(pv.Layout.Stack)
    .layers(trendData)
    .x(function (d) {
      return x(d.x);
    })
    .y(function (d) {
      return y(d.y);
    })
    .layer
    .add(pv.Area)
    .fillStyle(function () {
      return colors[this.parent.index % colors.size()][0];
    })
    .strokeStyle("rgba(128,128,128,.8)");

  /* Stack labels. */
  var firstIdx = computeIdx(w / 5);
  var lastIdx = computeIdx(w * 4 / 5);
  vis.add(pv.Panel)
    .extend(area.parent)
    .add(pv.Area)
    .extend(area)
    .fillStyle(null)
    .strokeStyle(null)
    .anchor(function () {
      return (idx == idx_numbers - 1 || idx > lastIdx) ? "right" : ((idx == 0 || idx < firstIdx) ? "left" : "center");
    })
    .add(pv.Label)
    .visible(function (d) {
      return this.index == idx && d.y != 0;
    })
    .font(function (d) {
      return Math.round(5 + Math.sqrt(y(d.y))) + "px sans-serif";
    })
    .textStyle("#DDD")
    .text(function (d) {
      return metrics[this.parent.index] + ": " + d.y;
    });

  /* The total cost of the selected dot in the header. */
  vis.add(pv.Label)
    .left(8)
    .top(16)
    .font(headerFont)
    .text(function () {
      return "Total: " + total[idx];
    });

  /* The date of the selected dot in the header. */
  vis.add(pv.Label)
    .left(w / 2)
    .top(16)
    .font(headerFont)
    .text(function () {
      return snapshots[idx].ld;
    });


  /* The event labels */
  eventColor = "rgba(75,159,213,1)";
  eventHoverColor = "rgba(202,227,242,1)";
  vis.add(pv.Line)
    .strokeStyle("rgba(0,0,0,.001)")
    .data(snapshots)
    .left(function (s) {
      return x(s.d);
    })
    .bottom(0)
    .anchor("top")
    .add(pv.Dot)
    .bottom(-6)
    .shape("triangle")
    .angle(pv.radians(180))
    .strokeStyle("grey")
    .visible(function (s) {
      return s.e.size() > 0;
    })
    .fillStyle(function () {
      return this.index == idx ? eventHoverColor : eventColor;
    })
    .add(pv.Dot)
    .radius(3)
    .visible(function (s) {
      return s.e.size() > 0 && this.index == idx;
    })
    .left(w / 2 + 8)
    .top(24)
    .shape("triangle")
    .fillStyle(function () {
      return this.index == idx ? eventHoverColor : eventColor;
    })
    .strokeStyle("grey")
    .anchor("right")
    .add(pv.Label)
    .font(headerFont)
    .text(function (s) {
      return s.e.size() == 0 ? "" : s.e[0] + ( s.e[1] ? " (... +" + (s.e.size() - 1) + ")" : "");
    });

  /* An invisible bar to capture events (without flickering). */
  vis.add(pv.Bar)
    .fillStyle("rgba(0,0,0,.001)")
    .width(w + 30)
    .height(h + 30)
    .event("mouseout", function () {
      i = -1;
      return vis;
    })
    .event("mousemove", function () {
      idx = computeIdx(vis.mouse().x);
      return vis;
    });

  vis.render();
};


//******************* TIMELINE CHART ******************* //
/*
 * Displays the evolution of metrics on a line chart, displaying related events.
 * 
 * Parameters of the Timeline class:
 *   - data: array of arrays, each containing maps {x,y,yl} where x is a (JS) date, y is a number value (representing a metric value at
 *           a given time), and yl the localized value of y. The {x,y, yl} maps must be sorted by ascending date.
 *   - metrics: array of metric names. The order is important as it defines which array of the "data" parameter represents which metric.
 *   - snapshots: array of maps {sid,d} where sid is the snapshot id and d is the locale-formatted date of the snapshot. The {sid,d} 
 *                maps must be sorted by ascending date.
 *   - events: array of maps {sid,d,l[{n}]} where sid is the snapshot id corresponding to an event, d is the (JS) date of the event, and l
 *             is an array containing the different event names for this date.
 *   - height: height of the chart area (notice header excluded). Defaults to 80.
 * 
 * Example: displays 2 metrics:
 * 
 <code>
 function d(y,m,d,h,min,s) {
 return new Date(y,m,d,h,min,s);
 }
 var data = [
 [{x:d(2011,5,15,0,1,0),y:912.00,yl:"912"},{x:d(2011,6,21,0,1,0),y:152.10,yl:"152.10"}],
 [{x:d(2011,5,15,0,1,0),y:52.20,yi:"52.20"},{x:d(2011,6,21,0,1,0),y:1452.10,yi:"1,452.10"}]
 ];
 var metrics = ["Lines of code","Rules compliance"];
 var snapshots = [{sid:1,d:"June 15, 2011 00:01"},{sid:30,d:"July 21, 2011 00:01"}];
 var events = [
 {sid:1,d:d(2011,5,15,0,1,0),l:[{n:"0.6-SNAPSHOT"},{n:"Sun checks"}]},
 {sid:30,d:d(2011,6,21,0,1,0),l:[{n:"0.7-SNAPSHOT"}]}
 ];

 var timeline = new SonarWidgets.Timeline('timeline-chart-20')
 .height(160)
 .data(data)
 .snapshots(snapshots)
 .metrics(metrics)
 .events(events);
 timeline.render();
 </code>
 * 
 */

SonarWidgets.Timeline = function (divId) {
  this.wDivId = divId;
  this.wHeight;
  this.wData;
  this.wSnapshots;
  this.wMetrics;
  this.wEvents;
  this.height = function (height) {
    this.wHeight = height;
    return this;
  };
  this.data = function (data) {
    this.wData = data;
    return this;
  };
  this.snapshots = function (snapshots) {
    this.wSnapshots = snapshots;
    return this;
  };
  this.metrics = function (metrics) {
    this.wMetrics = metrics;
    return this;
  };
  this.events = function (events) {
    this.wEvents = events;
    return this;
  };
};

SonarWidgets.Timeline.prototype.render = function () {

  var trendData = this.wData;
  var metrics = this.wMetrics;
  var snapshots = this.wSnapshots;
  var events = this.wEvents;

  var widgetDiv = $(this.wDivId);
  var headerFont = "10.5px Arial,Helvetica,sans-serif";

  /* Sizing and scales. */
  var headerHeight = 4 + Math.max(this.wMetrics.size(), events ? 2 : 1) * 18;
  var w = widgetDiv.getOffsetParent().getWidth() - 60;
  var h = (this.wHeight == null || this.wHeight <= 0 ? 80 : this.wHeight) + headerHeight;
  var yMaxHeight = h - headerHeight;

  var x = pv.Scale.linear(pv.blend(pv.map(trendData, function (d) {
    return d;
  })),
    function (d) {
      return d.x
    }).range(0, w);
  var y = new Array(trendData.size());
  for (var i = 0; i < trendData.size(); i++) {
    y[i] = pv.Scale.linear(trendData[i],
      function (d) {
        return d.y;
      }).range(20, yMaxHeight);
  }
  var interpolate = "linear";
  /* cardinal or linear */
  var idx = trendData[0].size() - 1;

  /* The root panel. */
  var vis = new pv.Panel()
    .canvas(widgetDiv)
    .width(w)
    .height(h)
    .left(20)
    .right(20)
    .bottom(30)
    .top(5)
    .strokeStyle("#CCC");

  /* X-axis */
  vis.add(pv.Rule)
    .data(x.ticks())
    .left(x)
    .bottom(-10)
    .height(10)
    .anchor("bottom")
    .add(pv.Label)
    .text(x.tickFormat);

  /* A panel for each data series. */
  var panel = vis.add(pv.Panel)
    .data(trendData);

  /* The line. */
  var line = panel.add(pv.Line)
    .data(function (array) {
      return array;
    })
    .left(function (d) {
      return x(d.x);
    })
    .bottom(function (d) {
      var yAxis = y[this.parent.index](d.y);
      return isNaN(yAxis) ? yMaxHeight : yAxis;
    })
    .interpolate(function () {
      return interpolate;
    })
    .lineWidth(2);

  /* The mouseover dots and label in header. */
  line.add(pv.Dot)
    .data(function (d) {
      return [d[idx]];
    })
    .fillStyle(function () {
      return line.strokeStyle();
    })
    .strokeStyle("#000")
    .size(20)
    .lineWidth(1)
    .add(pv.Dot)
    .radius(3)
    .left(10)
    .top(function () {
      return 10 + this.parent.index * 14;
    })
    .anchor("right").add(pv.Label)
    .font(headerFont)
    .text(function (d) {
      return metrics[this.parent.index] + ": " + d.yl;
    });

  /* The date of the selected dot in the header. */
  vis.add(pv.Label)
    .left(w / 2)
    .top(16)
    .font(headerFont)
    .text(function () {
      return snapshots[idx].d;
    });

  /* The event labels */
  if (events) {
    eventColor = "rgba(75,159,213,1)";
    eventHoverColor = "rgba(202,227,242,1)";
    vis.add(pv.Line)
      .strokeStyle("rgba(0,0,0,.001)")
      .data(events)
      .left(function (e) {
        return x(e.d);
      })
      .bottom(0)
      .anchor("top")
      .add(pv.Dot)
      .bottom(-6)
      .shape("triangle")
      .angle(pv.radians(180))
      .strokeStyle("grey")
      .fillStyle(function (e) {
        return e.sid == snapshots[idx].sid ? eventHoverColor : eventColor;
      })
      .add(pv.Dot)
      .radius(3)
      .visible(function (e) {
        return e.sid == snapshots[idx].sid;
      })
      .left(w / 2 + 8)
      .top(24)
      .shape("triangle")
      .fillStyle(function (e) {
        return e.sid == snapshots[idx].sid ? eventHoverColor : eventColor;
      })
      .strokeStyle("grey")
      .anchor("right")
      .add(pv.Label)
      .font(headerFont)
      .text(function (e) {
        return e.l[0].n + ( e.l[1] ? " (... +" + (e.l.size() - 1) + ")" : "");
      });
  }

  /* An invisible bar to capture events (without flickering). */
  vis.add(pv.Bar)
    .fillStyle("rgba(0,0,0,.001)")
    .width(w + 30)
    .height(h + 30)
    .event("mouseout", function () {
      i = -1;
      return vis;
    })
    .event("mousemove", function () {
      var mx = x.invert(vis.mouse().x);
      idx = pv.search(trendData[0].map(function (d) {
        return d.x;
      }), mx);
      idx = idx < 0 ? (-idx - 2) : idx;
      idx = idx < 0 ? 0 : idx;
      return vis;
    });

  vis.render();
};