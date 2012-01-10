#
# Sonar, open source software quality management tool.
# Copyright (C) 2008-2012 SonarSource
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
class AlertsController < ApplicationController
  SECTION=Navigation::SECTION_CONFIGURATION

  verify :method => :post, :only => ['create', 'update', 'delete'], :redirect_to => { :action => 'index' }
  before_filter :admin_required, :except => [ 'index' ]


  #
  #
  # GET /alerts/index/<profile_id>
  #
  #
  def index
    @profile = Profile.find(params[:id])
    @alerts = @profile.alerts.sort
    @alert=Alert.new
  end

  #
  #
  # GET /alerts/show/<alert id>
  #
  #
  def show
    @alert = @profile.alerts.find(params[:id])
  end


  #
  #
  # GET /alerts/new?profile_id=<profile id>
  #
  #
  def new
    @profile = Profile.find(params[:profile_id])
    @alert = @profile.alerts.build(params[:alert])
    respond_to do |format|
      format.js {
        render :update do |page|
         page.replace_html :new_alert_form, :partial => 'new'
        end
      }
      format.html # new.html.erb
      format.xml  { render :xml => @alert }
    end
  end


  #
  #
  # GET /alerts/edit/<alert id>
  #
  #
  def edit
    @alert = @profile.alerts.find(params[:id])
  end


  #
  #
  # POST /alerts/create?profile_id=<profile id>&...
  #
  #
  def create
    @profile = Profile.find(params[:profile_id])
    @alert = @profile.alerts.build(params[:alert])
    
    respond_to do |format|
      if @alert.save
        flash[:notice] = message('alerts.alert_created')
        format.html { redirect_to :action => 'index', :id=>@profile.id }
        format.js { render :update do |page|
          page.redirect_to :action => 'index', :id=>@profile.id
        end}
      else

        @alerts = @profile.alerts.reload
        format.html { render :action => "index" }
        format.xml  { render :xml => @alert.errors, :status => :unprocessable_entity }
        format.js { render :update do |page|
          page.replace_html( 'new_alert_form', :partial => 'new')
          
        end}
      end
    end
  end

  #
  #
  # POST /alerts/update/<alert id>?profile_id=<profile id>
  #
  #
  def update
    @profile = Profile.find(params[:profile_id])
    @alerts=@profile.alerts
    alert = @alerts.find(params[:id])

    respond_to do |format|
      if alert.update_attributes(params[:alert])
        flash[:notice] = message('alerts.alert_updated')
        format.html { redirect_to :action => 'index', :id=>@profile.id }
        format.xml  { head :ok }
        format.js { render :update do |page| page.redirect_to :action => 'index', :id=>@profile.id end}
      else
        @alert=Alert.new
        format.html { render :action => "index" }
        format.xml  { render :xml => @alert.errors, :status => :unprocessable_entity }
        format.js { render :update do |page| 
          page.replace_html( "row_alert_#{alert.id}", :partial => 'edit', :locals => {:alert => alert})
        end}
      end
    end
  end

  #
  #
  # POST /alerts/delete/<alert id>?profile_id=<profile id>
  #
  #
  def delete
    @profile = Profile.find(params[:profile_id])
    @alert = @profile.alerts.find(params[:id])
    @alert.destroy
    flash[:notice] = message('alerts.alert_deleted')

    respond_to do |format|
      format.html { redirect_to(:action => 'index', :id=>@profile.id) }
      format.xml  { head :ok }
    end
  end

end
