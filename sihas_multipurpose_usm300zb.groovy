/*
 *  Copyright 2021 SmartThings
 *
 *  Ported for Hubitat Elevation platform by kkossev 2022/10/14 11:43 PM ver. 2.0.1
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType

metadata {
    definition (name: "SiHAS Multipurpose Sensor", namespace: "shinasys", author: "SHINA SYSTEM") {
        capability "Motion Sensor"
        capability "Configuration"
        capability "Battery"
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
        capability "Refresh"
        capability "Sensor"
        
        attribute "batteryVoltage", "string"
        
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0400,0003,0406,0402,0001,0405,0500", outClusters:"0004,0003,0019", model:"USM-300Z", manufacturer:"ShinaSystem", deviceJoinName: "SiHAS MultiPurpose Sensor"
    }
    preferences {
        section {
            input (name: "logEnable", type: "bool", title: "Debug logging", description: "<i>Debug information, useful for troubleshooting. Recommended value is <b>false</b></i>", defaultValue: false)
            input (name: "txtEnable", type: "bool", title: "Description text logging", description: "<i>Display sensor states in HE log page. Recommended value is <b>true</b></i>", defaultValue: true)
            input "tempOffset"    , "decimal", title: "Temperature offset", description: "<i>Select how many degrees to adjust the temperature.</i>", range: "-100.0..100.0", displayDuringSetup: false, defaultValue: 0.0
            input "humidityOffset", "number", title: "Humidity offset"   , description: "<i>Enter a percentage to adjust the humidity.</i>", range: "*..*", displayDuringSetup: false, defaultValue: 0
        }
    }
}

private getILLUMINANCE_MEASUREMENT_CLUSTER() { 0x0400 }
private getRELATIVE_HUMIDITY_CLUSTER() { 0x0405 }
private getOCCUPANCY_SENSING_CLUSTER() { 0x0406 }
private getATTRIBUTE_IAS_ZONE_STATUS() { 0x0000 }
private getPOWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE() { 0x0020 }
private getTEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE() { 0x0000 }
private getRELATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE() { 0x0000 }
private getILLUMINANCE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE() { 0x0000 }
private getOCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE() { 0x0000 }

private List<Map> collectAttributes(Map descMap) {
    List<Map> descMaps = new ArrayList<Map>()
    descMaps.add(descMap)
    if (descMap.additionalAttrs) {
        descMaps.addAll(descMap.additionalAttrs)
    }
    return  descMaps
}

def parse(String description) {
    if (settings?.logEnable) {log.debug "${device.displayName} Parsing message from device: $description"}
    Map map = zigbee.getEvent(description)
    if (settings?.logEnable) {log.trace "${device.displayName} Map =  $map"}
    if (!map) {
        if (description?.startsWith('read attr')) {
            Map descMap = zigbee.parseDescriptionAsMap(description)
            if (settings?.logEnable) {log.trace "${device.displayName} descMap =  $descMap"}
            if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
                List<Map> descMaps = collectAttributes(descMap)
                def battMap = descMaps.find { it.attrInt == POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE }
                if (battMap) {
                    map = getBatteryResult(Integer.parseInt(battMap.value, 16))
                }
            } 
            else if (descMap?.clusterInt == OCCUPANCY_SENSING_CLUSTER && descMap.attrInt == OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE && descMap?.value) {
                map = getMotionResult(descMap.value == "01" ? "active" : "inactive")
            }
            else if (descMap?.clusterInt == ILLUMINANCE_MEASUREMENT_CLUSTER && descMap.attrInt == ILLUMINANCE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE && descMap?.value) {
                map.name = "illuminance"
                map.value = Integer.parseInt(descMap?.value,16)
                map.descriptionText = "${device.displayName} illuminance was ${map.value} lx"
                map.unit = "lx"
                map.translatable = true
                if (settings?.txtEnable) {log.info "${map.descriptionText}"}                
            }
            else if (descMap?.clusterInt == RELATIVE_HUMIDITY_CLUSTER && descMap.attrInt == RELATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE && descMap?.value) {
                map.name = "humidity"
                map.value = Integer.parseInt(descMap?.value,16) / 100 as int
                if (humidityOffset) {
                    map.value = map.value + (int) humidityOffset
                }                
                map.descriptionText = "${device.displayName} humidity was ${map.value} %RH"
                map.unit = "%RH"
                map.translatable = true
                if (settings?.txtEnable) {log.info "${map.descriptionText}"}                
            }
            else {
                if (settings?.logEnable) {log.warn "${device.displayName} unprocessed read attr device: clusterInt=${descMap?.clusterInt}  attrInt=${descMap.attrInt} value=${descMap?.value}"}
            }
        } // if read attr
        else if (description?.startsWith('illuminance:')) { //parse illuminance
            map = parseCustomMessage(description)
            if (settings?.logEnable) {log.debug "${device.displayName} illuminance custom message: ${map}"}
        }
        else {
            if (settings?.logEnable) {log.debug "${device.displayName} NOT PARSED message from device: $description"}
        }
    } 
    else if (map.name == "temperature") {
        Map descMap = zigbee.parseDescriptionAsMap(description)
        float rawValue = ((Integer.parseInt(descMap?.value,16) / 100.0) as float)
        map.value = rawValue
        if (tempOffset) {
            //map.value = new BigDecimal((map.value as float) + (tempOffset as float)).setScale(3/*1*/, BigDecimal.ROUND_HALF_UP)
            map.value = rawValue + (tempOffset as float)
        }
        map.value = map.value.round(1)
        map.descriptionText = temperatureScale == 'C' ? "${device.displayName} temperature was ${map.value}°C" : "${device.displayName} temperature was ${map.value}°F"
        map.translatable = true
        if (settings?.txtEnable) {log.info "${map.descriptionText}"}
    } 
    else if (map.name == "humidity") {
        if (humidityOffset) {
            map.value = map.value + (int) humidityOffset
        }
        map.descriptionText = "${device.displayName} humidity was ${map.value}%"
        map.unit = "%"
        map.translatable = true
        if (settings?.txtEnable) {log.info "${map.descriptionText}"}
    } 
    else if (map.name == "battery") {   // [name:battery, value:87.0]
        map.descriptionText = "${device.displayName} battery was ${map.value}%"
        map.unit = "%"
        map.translatable = true    
        if (settings?.txtEnable) {log.info "${map.descriptionText}"}
    } 
    else if (map.name == "batteryVoltage") {
        map.descriptionText = "${device.displayName} battery voltage was ${map.value}V"
        map.unit = "V"
        map.translatable = true    
        if (settings?.txtEnable) {log.info "${map.descriptionText}"}
    } 
    else {
        if (settings?.logEnable) {log.warn "${device.displayName} unprocessed Map event from device: ${map}"}
    }

    def result = map ? createEvent(map) : [:]

    if (description?.startsWith('enroll request')) {
        List cmds = zigbee.enrollResponse()
        result = cmds?.collect { new hubitat.device.HubAction(it) }
    }
    if (settings?.logEnable) {log.debug "${device.displayName} result: $result"}
    return result
}

private def parseCustomMessage(String description) {
    return [
        name           : description.split(": ")[0],
        value          : description.split(": ")[1],
        translatable   : true
    ]
}

private Map getBatteryResult(rawValue) {
    if (settings?.logEnable) log.trace "getBatteryResult rawValue=${rawValue}"
    def linkText = getLinkText(device)
    def result = [:]
    def volts = rawValue / 10

    if (!(rawValue == 0 || rawValue == 255)) {
        result.name = 'battery'
        result.translatable = true
        def minVolts = 2.2
        def maxVolts = 3.1
        
        if (isDSM300()) maxVolts = 3.0
        if (isCSM300()) minVolts = 1.9
        
        def pct = (volts - minVolts) / (maxVolts - minVolts)
        def roundedPct = Math.round(pct * 100)
        if (roundedPct <= 0)
            roundedPct = 1
        result.value = Math.min(100, roundedPct)
        result.descriptionText = "${device.displayName} battery was ${result.value}%"        
        if (settings?.txtEnable) {log.info "${result.descriptionText}"}
    }
    return result
}

private Map getMotionResult(value) {
    String descriptionText = value == 'active' ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
    if (settings?.txtEnable) {log.info "${descriptionText}"}
    return [
        name           : 'motion',
        value          : value,
        descriptionText: descriptionText,
        translatable   : true
    ]
}

def refresh() {
    def refreshCmds = []

    refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE)

    refreshCmds += zigbee.readAttribute(RELATIVE_HUMIDITY_CLUSTER, RELATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
    refreshCmds += zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
    refreshCmds += zigbee.readAttribute(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
    refreshCmds += zigbee.readAttribute(OCCUPANCY_SENSING_CLUSTER, OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE)
    refreshCmds += zigbee.enrollResponse()
    return refreshCmds
}

def configure() {
    if (settings?.logEnable) {log.debug "${device.displayName} configure()"}
    def configCmds = []

    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

    // temperature minReportTime 30 seconds, maxReportTime 5 min. Reporting interval if no activity
    // battery minReport 30 seconds, maxReportTime 6 hrs by default
    // humidity minReportTime 30 seconds, maxReportTime 60 min
    // illuminance minReportTime 30 seconds, maxReportTime 60 min
    // occupancy sensing minReportTime 10 seconds, maxReportTime 60 min
    // ex) zigbee.configureReporting(0x0001, 0x0020, DataType.UINT8, 600, 21600, 0x01)
    // This is for cluster 0x0001 (power cluster), attribute 0x0021 (battery level), whose type is UINT8,
    // the minimum time between reports is 10 minutes (600 seconds) and the maximum time between reports is 6 hours (21600 seconds),
    // and the amount of change needed to trigger a report is 1 unit (0x01).
    configCmds += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE, DataType.UINT8, 30, 21600, 0x01/*100mv*1*/)

    configCmds += zigbee.configureReporting(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE, DataType.INT16, 15, 300, 10/*10/100=0.1도*/)
    configCmds += zigbee.configureReporting(RELATIVE_HUMIDITY_CLUSTER, RELATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE, DataType.UINT16, 15, 300, 40/*10/100=0.4%*/)
    configCmds += zigbee.configureReporting(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE, DataType.UINT16, 15, 3600, 1/*1 lux*/)
    configCmds += zigbee.configureReporting(OCCUPANCY_SENSING_CLUSTER, OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE, DataType.BITMAP8, 1, 600, 1)
    configCmds += zigbee.configureReporting(zigbee.IAS_ZONE_CLUSTER, ATTRIBUTE_IAS_ZONE_STATUS, DataType.BITMAP16, 0, 0xffff, null) // set : none reporting flag, device sends out notification to the bound devices.
    
    return configCmds + refresh()
}

