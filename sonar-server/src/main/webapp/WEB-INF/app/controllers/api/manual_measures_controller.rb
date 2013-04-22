#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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

require 'json'

class Api::ManualMeasuresController < Api::ApiController

  #
  # GET /api/manual_measures?resource=<resource>&metric=<optional metric>
  #
  def index
    resource=load_resource(params[:resource], :user)

    metric=nil
    if params[:metric].present?
      metric=Metric.by_key(params[:metric])
      bad_request("Unknown metric: #{params[:metric]}") if metric.nil?
    end

    result = resource.manual_measures
    if metric
      result = result.select{|m| m.metric_id==metric.id}
    end

    respond_to do |format|
      format.json { render :json => jsonp(manual_measures_to_json(resource, result)) }
      format.xml { render :xml => xml_not_supported }
    end
  end

    #
    # POST /api/manual_measures?resource=<resource>&metric=<metric>&val=<optional decimal value>&text=<optional text>
    # Create or update measure.
    #
  def create
    resource=load_resource(params[:resource], :admin)

    metric=Metric.by_key(params[:metric])
    bad_request("Unknown metric: #{params[:metric]}") if metric.nil?

    value=params[:val]
    bad_request("Not a numeric value: #{value}") if value && !Api::Utils.is_number?(value)

    measure=ManualMeasure.first(:conditions => ['resource_id=? and metric_id=?', resource.id, metric.id])
    if measure.nil?
      measure=ManualMeasure.new(:resource => resource, :user_login => current_user.login, :metric_id => metric.id)
    end

    measure.value = value
    measure.text_value = params[:text]
    measure.description = params[:desc]
    measure.save!

    respond_to do |format|
      format.json { render :json => jsonp(manual_measures_to_json(resource, [measure])) }
      format.xml { render :xml => xml_not_supported }
    end
  end
  

    #
    # DELETE /api/manual_measures?resource=<resource>&metric=<metric>
    #
  def destroy
    resource=load_resource(params[:resource], :admin)

    metric=Metric.by_key(params[:metric])
    bad_request("Unknown metric: #{params[:metric]}") if metric.nil?

    count = ManualMeasure.delete_all(['resource_id=? and metric_id=?', resource.id, metric.id])

    render_success "Deleted #{count} measures"
  end

  private

  def manual_measures_to_json(resource, manual_measures)
    json = []
    manual_measures.each do |m|
      json<<manual_measure_to_json(resource, m)
    end
    json
  end

  def manual_measure_to_json(resource, manual_measure)
    hash={:id => manual_measure.id.to_i, :metric => manual_measure.metric.key, :resource => resource.key}
    hash[:val]=manual_measure.value if manual_measure.value
    hash[:text]=manual_measure.text_value if manual_measure.text_value
    hash[:desc]=manual_measure.description if manual_measure.description
    hash[:created_at]=format_datetime(manual_measure.created_at)
    hash[:updated_at]=format_datetime(manual_measure.updated_at) if manual_measure.updated_at
    if manual_measure.user
      hash[:login]=manual_measure.user_login
      hash[:username]=manual_measure.user.name
    end
    hash
  end
end
