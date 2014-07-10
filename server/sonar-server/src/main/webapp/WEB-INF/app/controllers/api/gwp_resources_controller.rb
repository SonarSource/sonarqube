#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

require "json"

# Google Wire Protocol controller helper.
# Used by the MotionChart plugin.
class Api::GwpResourcesController < Api::ResourceRestController

  before_filter :parse_gwp_params
  
  EMPTY_HASH={}
  TYPE_BOOLEAN = :boolean
  TYPE_STRING = :string
  TYPE_DATE = :date
  TYPE_DATE_TIME = :datetime
  TYPE_NUMBER = :number
  TYPE_TIME_OF_DAY = :timeofday
  
  def parse_gwp_params
    # tqx sample : reqId:0
    tqx=params[:tqx]
    if tqx.nil?
      rest_gwp_error("Missing tqx parameter", "invalid_request")
      return
    end
    tqx_params = tqx.split(';')
    tqx_params.each do |tqx_param|
      param_key_val = tqx_param.split(':')
      params[param_key_val[0]] = param_key_val[1]
    end
    params[:format] = params[:out] if params[:out]
    params[:callback] = params[:responseHandler] if params[:responseHandler]
  end
  
  def rest_to_json(objects)
    data_table = {:cols => [], :rows => []}
    fill_gwp_data_table(objects, data_table)
    rest_gwp_ok(data_table)
  end
  
  def add_column(data_table, id, label, type)
    data_table[:cols] << {:id => id, :label => label, :type => type}
  end
  
  def new_row(data_table)
    row = {:c => []}
    data_table[:rows] << row
    row
  end
  
  def add_row_value(row, value, formatted_value = nil)
    if value
      if formatted_value
        row[:c] << {:v => value, :f => formatted_value}
      else
        row[:c] << {:v => value}
      end
    else
      row[:c] << EMPTY_HASH
    end
  end
  
  def rest_status_ko(msg, error_code)
    json = JSON({:reqId => params[:reqId], :status => 'error', :errors => { :reason => "internal_error", :message => msg }})
    render :json => jsonp(json), :status => error_code
  end
  
  def rest_gwp_ok(data_table)
    gwp_resp = {:reqId => params[:reqId], :status => 'ok'}
    gwp_resp[:table] = data_table if data_table[:rows].size > 0
    gwp_resp
  end
  
  private
  
  def rest_gwp_error(message, reason)
    json = JSON({:reqId => params[:reqId], :status => 'error', :errors => { :reason =>reason, :message => message }})
    render :json => jsonp(json), :status => 500
  end
  
end

class Api::GwpJsonTime
  @time
  
  def initialize(time)
    @time = time
  end
  
  def to_json(options = nil)
    "new Date(#{@time.year},#{@time.month-1},#{@time.day},#{@time.hour},#{@time.min},#{@time.sec})"
  end
end

class Api::GwpJsonDate
  @time
  
  def initialize(time)
    @time = time
  end
  
  def to_json(options = nil)
    "new Date(#{@time.year},#{@time.month-1},#{@time.day})"
  end
end