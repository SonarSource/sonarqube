#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class Api::EventsController < Api::ApiController

  # GET /api/events?resource=xxx&category=xxx
  # Examples :
  #   - global events :   curl -v http://localhost:9000/api/events -u admin:admin
  #   - resource events : curl -v http://localhost:9000/api/events?resource=org.apache.struts:struts-parent
  #
  def index
    begin
      load_resource(:user, params[:resource])

      conditions=[]
      values={}

      if params[:categories]
        conditions<<'category IN (:categs)'
        values[:categs]=params[:categories].split(',')
      end
      
      if @resource
        conditions<<'resource_id=:rid'
        values[:rid]=@resource.id
      else
        conditions<<'resource_id IS NULL'
      end

      if params[:fromDateTime]
        from=parse_datetime(params[:fromDateTime], false)
      elsif params[:fromDate]
        from=Date::strptime(params[:fromDate])
      end
      if from
        conditions<<'event_date>=:from'
        values[:from]=from
      end

      if params[:toDateTime]
        to=parse_datetime(params[:toDateTime], false)
      elsif params[:toDate]
        to=Date::strptime(params[:toDate])
      end
      if to
        conditions<<'event_date<=:to'
        values[:to]=to
      end

      events=Event.find(:all, :conditions => [conditions.join(' AND '), values], :order => 'event_date DESC')

      respond_to do |format|
        format.json { render :json => jsonp(events_to_json(events)) }
        format.xml  { render :xml => events_to_xml(events) }
        format.text { render :text => text_not_supported }
      end

    rescue ApiException => e
      render_error(e.msg, e.code)
      
    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end

  # GET /api/events/:id
  # Example : curl -v http://localhost:9000/api/events/3
  def show
    begin
      event=Event.find(params[:id])
      load_resource(:user, event.resource_id)

      respond_to do |format|
        format.json { render :json => jsonp(events_to_json([event])) }
        format.xml  { render :xml => events_to_xml([event]) }
        format.text { render :text => text_not_supported }
      end

    rescue ActiveRecord::RecordNotFound => e
      render_error(e.message, 404)

    rescue ApiException => e
      render_error(e.msg, e.code)
      
    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end

  #
  # POST /api/events
  # Required parameters :
  # - name
  # - category
  #
  # Optional parameters :
  # - resource (id or key)
  # - description
  # - data
  # - dateTime (ISO format : 2010-12-25T23:59:59+0100)
  #
  # Example :
  # curl -d "name=foo&category=bar&dateTime=2010-12-25T23%3A59%3A59%2B0100" http://localhost:9000/api/events -v -u admin:admin
  #
  def create
    begin
      load_resource(:admin, params[:resource])

      date=parse_datetime(params[:dateTime], true)
      
      event=Event.new(
        :name => params[:name],
        :description => params[:description],
        :event_date => date,
        :category => params[:category],
        :resource_id => (@resource ? @resource.id : nil),
        :data => params[:data]
      )
      event.save!
      respond_to do |format|
        format.json { render :json => jsonp(events_to_json([event])) }
        format.xml  { render :xml => events_to_xml([event]) }
        format.text { render :text => text_not_supported }
      end

    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      render_error(e.message, 400)
    end
  end


  # DELETE /api/events/1
  # Example : curl -X DELETE  http://localhost:9000/api/events/9 -v
  def destroy
    begin
      event=Event.find(params[:id])
      load_resource(:admin, event.resource_id)
      event.delete
      render_success("Event deleted")
      
    rescue ActiveRecord::RecordNotFound => e
      render_error(e.message, 404)
      
    rescue ApiException => e
      render_error(e.msg, e.code)

    rescue Exception => e
      logger.error("Fails to execute #{request.url} : #{e.message}")
      render_error(e.message)
    end
  end


  private

  def load_resource(required_resource_role, resource_key=nil)
    if resource_key
      @resource=Project.by_key(resource_key)
      if @resource.nil?
        raise ApiException.new 404, "Resource not found: #{resource_key}"
      end

      unless has_role?(required_resource_role, @resource)
        raise ApiException.new 401, "Unauthorized"
      end
    else
      # global events
      unless is_admin?
        raise ApiException.new 401, "Unauthorized"
      end
    end
  end

  def events_to_json(events=[])
    json=[]
    events.each do |event|
      json<<event_to_json(event)
    end
    json
  end

  def event_to_json(event)
    hash={}
    hash[:id]=event.id.to_s
    hash[:rk]=event.resource.key if event.resource
    hash[:n]=event.name if event.name
    hash[:c]=event.category
    hash[:dt]=format_datetime(event.event_date) if event.event_date
    hash[:ds]=event.description if event.description
    hash[:data]=event.data if event.data
    hash
  end

  def events_to_xml(events, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.events do
      events.each do |event|
        event_to_xml(event, xml)
      end
    end
  end

  def event_to_xml(event, xml=Builder::XmlMarkup.new(:indent => 0))
    xml.event do
      xml.id(event.id.to_s)
      xml.name(event.name) if event.name
      xml.resourceKey(event.resource.key) if event.resource
      xml.category(event.category)
      xml.date(format_datetime(event.event_date)) if event.event_date
      xml.description(event.description) if event.description
      xml.data(event.data) if event.data
    end
  end

end