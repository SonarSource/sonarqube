/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.charts.deprecated;

import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import java.util.StringTokenizer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.AbstractRenderer;

public abstract class BaseChartWeb extends BaseChart {

  // Chart types
  public static final String BAR_CHART_HORIZONTAL = "hb";
  public static final String BAR_CHART_VERTICAL = "vb";
  public static final String BAR_CHART_VERTICAL_CUSTOM = "cvb";
  public static final String STACKED_BAR_CHART = "sb";
  public static final String PIE_CHART = "p";
  public static final String SPARKLINES_CHART = "sl";

  // Chart params
  public static final String CHART_PARAM_TYPE = "cht";
  public static final String CHART_PARAM_VALUES = "chv";
  public static final String CHART_PARAM_COLORS = "chc";
  public static final String CHART_PARAM_RANGEMAX = "chrm";
  public static final String CHART_PARAM_TITLE = "chti";
  public static final String CHART_PARAM_DIMENSIONS = "chdi";
  public static final String CHART_PARAM_RANGEAXIS_VISIBLE = "chrav";
  public static final String CHART_PARAM_CATEGORIES = "chca";
  public static final String CHART_PARAM_CATEGORIES_AXISMARGIN_VISIBLE = "chcav";
  public static final String CHART_PARAM_CATEGORIES_AXISMARGIN_UPPER = "chcaamu";
  public static final String CHART_PARAM_CATEGORIES_AXISMARGIN_LOWER = "chcaaml";
  public static final String CHART_PARAM_SERIES = "chse";
  public static final String CHART_PARAM_SERIES_AXISMARGIN_UPPER = "chseamu";
  public static final String CHART_PARAM_SERIES_AXISMARGIN_LOWER = "chseaml";
  public static final String CHART_PARAM_SERIES_AXISMARGIN_TICKUNIT = "chsetu";
  public static final String CHART_PARAM_INSETS = "chins";
  public static final String CHART_PARAM_OUTLINE_VISIBLE = "chov";
  public static final String CHART_PARAM_OUTLINE_RANGEGRIDLINES_VISIBLE = "chorgv";

  // Default labels
  public static final String DEFAULT_NAME_CATEGORY = "category";
  public static final String DEFAULT_NAME_SERIE = "serie";
  public static final String DEFAULT_MESSAGE_NODATA = "No data available";

  // Default values
  public static final double DEFAULT_CATEGORIES_AXISMARGIN = 0.0;
  public static final double DEFAULT_SERIES_AXISMARGIN = 0.0;

  // Default dimensions
  public static final int DEFAULT_WIDTH = 60;
  public static final int DEFAULT_HEIGHT = 20;

  // Default font
  public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 13);

  protected JFreeChart jfreechart = null;
  protected Map<String, String> params = null;

  public BaseChartWeb(Map<String, String> params) {
    super(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    this.params = params;
  }

  protected boolean isParamValueValid(String paramValue) {
    return paramValue != null && paramValue.length() > 0;
  }

  protected double convertParamToDouble(String paramValue) {
    return convertParamToDouble(paramValue, 0.0);
  }
  
  protected void configureColors(String colors, AbstractRenderer renderer) {
    try {
      if (colors != null && colors.length() > 0) {
        StringTokenizer stringTokenizer = new StringTokenizer(colors, ",");
        int i = 0;
        while (stringTokenizer.hasMoreTokens()) {
          renderer.setSeriesPaint(i, Color.decode("0x" + stringTokenizer.nextToken()));
          i++;
        }
      } else {
        configureDefaultColors(renderer);
      }
    } catch (Exception e) {
      configureDefaultColors(renderer);
    }
  }

  protected void configureDefaultColors(AbstractRenderer renderer) {
    for (int i=0 ; i<COLORS.length ; i++) {
      renderer.setSeriesPaint(i, COLORS[i]);
    }
  }

  protected double convertParamToDouble(String paramValue, double paramDefault) {
    double result = paramDefault;
    if (isParamValueValid(paramValue)) {
      try {
        result = Double.parseDouble(paramValue);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    return result;
  }


  protected void configureDimensions(String dimensions) {
    try {
      if (dimensions == null || dimensions.length() == 0) {
        // Do nothing, default dimensions are already setted
      } else if (dimensions.indexOf('x') == -1) {
        int iDim = Integer.parseInt(dimensions);
        setWidth(iDim);
        setHeight(iDim);
      } else {
        StringTokenizer st = new StringTokenizer(dimensions, "x");
        int iWidth = Integer.parseInt(st.nextToken());
        int iHeight = iWidth;
        if (st.hasMoreTokens()) {
          iHeight = Integer.parseInt(st.nextToken());
        }
        setWidth(iWidth);
        setHeight(iHeight);
      }
    } catch (NumberFormatException e) {
      // Do nothing, default dimensions are already setted
    }
  }

  protected void applyCommonParams() {
    configureChartTitle(jfreechart, params.get(BaseChartWeb.CHART_PARAM_TITLE));
    configureDimensions(params.get(BaseChartWeb.CHART_PARAM_DIMENSIONS));
  }

}
