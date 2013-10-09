/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stestr@nethome.nu>
 *
 * This file is part of OpenNetHome.
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// package nu.nethome.home.items.upm;

import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.items.LoggerComponent;
import nu.nethome.home.items.ValueItem;
import nu.nethome.home.system.Event;
import nu.nethome.home.system.HomeService;
import nu.nethome.util.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Presents and logs temperature values received by an RubicsonGrill-temperature sensor. The actual
 * values are received as events which may be sent by any kind of receiver module
 * which can receive RubicsonGrill messages from the hardware devices.
 *
 * @author Stefan
 */
@Plugin
public class RubicsonGrillThermometer extends HomeItemAdapter implements HomeItem, ValueItem {

    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"RubicsonGrillThermometer\" Category=\"Thermometers\" >"
            + "  <Attribute Name=\"Temperature\" 	Type=\"String\" Get=\"getValue\" Default=\"true\" />"
            + "  <Attribute Name=\"TimeSinceUpdate\" 	Type=\"String\" Get=\"getTimeSinceUpdate\" />"
            + "  <Attribute Name=\"DeviceCode\" Type=\"String\" Get=\"getDeviceCode\" />"
            + "  <Attribute Name=\"LogFile\" Type=\"String\" Get=\"getLogFile\" 	Set=\"setLogFile\" />"
            + "  <Attribute Name=\"LastUpdate\" Type=\"String\" Get=\"getLastUpdate\" />"
            + "</HomeItem> ");

    private static Logger logger = Logger.getLogger(RubicsonGrillThermometer.class.getName());
    private LoggerComponent tempLoggerComponent = new LoggerComponent(this);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss yyyy.MM.dd ");

    // Public attributes
    private double temperature = 0;
    private String itemDeviceCode = "1";
    private Date latestUpdateOrCreation = new Date();
    private boolean hasBeenUpdated = false;

    public RubicsonGrillThermometer() {
    }

    public int receiveEvent(Event event) {
        // Check if the event is an RubicsonGrill_Message and in that case check if it is
        // intended for this thermometer (by House Code and Device Code).
        // See http://wiki.nethome.nu/doku.php/events#upm_message
        if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("RubicsonGrill_Message")) {
//            if (event.getAttribute("RubicsonGrill.DeviceCode").equals(itemDeviceCode)) {
                // Recalculate the raw temperature value to Celsius Degrees
                temperature = event.getAttributeInt("RubicsonGrill.TempC");
                logger.finer("Temperature update: " + temperature + " degrees");
                // Format and store the current time.
                latestUpdateOrCreation = new Date();
                hasBeenUpdated = true;
//            }
        }
        return 0;
    }

    public String getModel() {
        return MODEL;
    }

    /* Activate the instance
      * @see ssg.home.HomeItem#activate()
      */
    public void activate(HomeService server) {
        super.activate(server);
        // Activate the logger component
        tempLoggerComponent.activate();
    }

    /**
     * HomeItem method which stops all object activity for program termination
     */
    public void stop() {
        tempLoggerComponent.stop();
    }

    public String getValue() {
        return hasBeenUpdated ? String.format("%.1f", temperature) : "";
    }


    /**
     * @return Returns the DeviceCode.
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getDeviceCode() {
        return itemDeviceCode;
    }

    /**
     * @param deviceCode The DeviceCode to set.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setDeviceCode(String deviceCode) {
        itemDeviceCode = deviceCode;
    }

    /**
     * @return the LastUpdate
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getLastUpdate() {
        return hasBeenUpdated ? dateFormatter.format(latestUpdateOrCreation) : "";
    }


    /**
     * @return Returns the LogFile.
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getLogFile() {
        return tempLoggerComponent.getFileName();
    }

    /**
     * @param logfile The LogFile to set.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setLogFile(String logfile) {
        tempLoggerComponent.setFileName(logfile);
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getTimeSinceUpdate() {
        return Long.toString((new Date().getTime() - latestUpdateOrCreation.getTime()) / 1000);
    }
}
