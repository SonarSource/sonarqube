window.SonarWidgets = {}

SonarWidgets.Timeline = function (divId) {
	this.wDivId = divId;
	this.wHeight;
	this.wData;
	this.wSnapshots;
	this.wMetrics;
	this.wTranslations;
	this.wEvents;
	this.height = function(height) {
		this.wHeight = height;
		return this;
	}
	this.data = function(data) {
		this.wData = data;
		return this;
	}
	this.snapshots = function(snapshots) {
		this.wSnapshots = snapshots;
		return this;
	}
	this.metrics = function(metrics) {
		this.wMetrics = metrics;
		return this;
	}
	this.translations = function(translations) {
		this.wTranslations = translations;
		return this;
	}
	this.events = function(events) {
		this.wEvents = events;
		return this;
	}
}

SonarWidgets.Timeline.prototype.render = function() {
	
	var trendData = this.wData;
	var metrics = this.wMetrics;
	var snapshots = this.wSnapshots;
	var translations = this.wTranslations;
	var events = this.wEvents;
	
	var widgetDiv = document.getElementById(this.wDivId);
	var footerFont = "10.5px Arial,Helvetica,sans-serif";
	var show_y_axis = (trendData.size()==1)
	
	/* Sizing and scales. */
	var headerHeight = 4 + Math.max(this.wMetrics.size(), events ? 2 : 1) * 18;
	var w = widgetDiv.parentNode.clientWidth - 60; 
	var	h = (this.wHeight == null ? 80 : this.wHeight) + headerHeight;
	var yMaxHeight = h-headerHeight;

	var x = pv.Scale.linear(pv.blend(pv.map(trendData, function(d) {return d;})), function(d) {return d.x}).range(0, w);
	var y = new Array(trendData.size());
	for(var i = 0; i < trendData.size(); i++){
		y[i]=pv.Scale.linear(trendData[i], function(d) {return d.y;}).range(20, yMaxHeight);
	}
	var interpolate = "linear"; /* cardinal or linear */
	var idx = this.wData[0].size() - 1;

	/* The root panel. */
	var vis = new pv.Panel()
		.canvas(widgetDiv)
		.width(w)
		.height(h)
		.left(20)
		.right(20)
		.bottom(20)
		.top(5)
		.strokeStyle("#CCC");

	/* X-axis */
	vis.add(pv.Rule)
		.data(x.ticks())
		.left(x)
		.bottom(-5)
		.height(5)
		.anchor("bottom")
		.add(pv.Label)
		.text(x.tickFormat);

	/* A panel for each data series. */
	var panel = vis.add(pv.Panel)
		.data(trendData);

	/* The line. */
	var line = panel.add(pv.Line)
		.data(function(array) {return array;})
		.left(function(d) {return x(d.x);})
		.bottom(function(d) {var yAxis = y[this.parent.index](d.y); return isNaN(yAxis) ? yMaxHeight : yAxis;})
		.interpolate(function() {return interpolate;})
		.lineWidth(2);

	/* The mouseover dots and label in footer. */
	line.add(pv.Dot)
		.data(function(d) {return [d[idx]];})
		.fillStyle(function() {return line.strokeStyle();})
		.strokeStyle("#000")
		.size(20) 
		.lineWidth(1)
		.add(pv.Dot)
		.radius(3)
		.left(10)
		.top(function() {return 10 + this.parent.index * 14;})
		.anchor("right").add(pv.Label)
		.font(footerFont)
		.text(function(d) {return metrics[this.parent.index] + ": " + d.y.toFixed(2);});
	
	/* The date of the selected dot in footer. */
	vis.add(pv.Label)
		.left(w/2)
		.top(16)
		.font(footerFont)
		.text(function() {return translations.date + ": " + snapshots[idx].d;});
	
	/* The event labels */
	if (events) {
	  eventColor = "rgba(75,159,213,1)";
	  eventHoverColor = "rgba(202,227,242,1)";
	  vis.add(pv.Line)
		.strokeStyle("rgba(0,0,0,.001)")
		.data(events)
		.left(function(e) {return x(e.d);})
		.bottom(0)
		.anchor("top")
		.add(pv.Dot)
		.bottom(6)
		.shape("triangle")
		.strokeStyle("grey")
		.fillStyle(function(e) {return e.sid == snapshots[idx].sid ? eventHoverColor : eventColor})
		.add(pv.Dot)
		.radius(3)
		.visible(function(e) { return e.sid == snapshots[idx].sid;})
		.left(w/2+8)
		.top(24)
		.shape("triangle")
		.fillStyle(function(e) {return e.sid == snapshots[idx].sid ? eventHoverColor : eventColor})
		.strokeStyle("grey")
		.anchor("right")
		.add(pv.Label)
		.font(footerFont)
		.text(function(e) {return e.l[0].n + ( e.l[1] ? " (... +" + (e.l.size()-1) + ")" : "");});
	}
	
	/* An invisible bar to capture events (without flickering). */
	vis.add(pv.Bar)
		.fillStyle("rgba(0,0,0,.001)")
		.width(w+10)
		.event("mouseout", function() {
			i = -1;
			return vis;
		})
		.event("mousemove", function() {
			var mx = x.invert(vis.mouse().x);
			idx = pv.search(trendData[0].map(function(d) {return d.x;}), mx);
			idx = idx < 0 ? (-idx - 2) : idx;
			idx = idx < 0 ? 0 : idx;
			return vis;
		});
	
	vis.render();
	
}