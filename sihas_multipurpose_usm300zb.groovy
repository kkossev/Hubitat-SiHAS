/*
 *  Copyright 2021 SmartThings
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
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "SiHAS Multipurpose Sensor", namespace: "shinasys", author: "SHINA SYSTEM") {
        capability "Motion Sensor"
        capability "Configuration"
        capability "Battery"
        capability "Temperature Measurement"
        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
        capability "Refresh"
        capability "Health Check"
        capability "Sensor"
        
        fingerprint inClusters: "0000,0001,0003,0020,0400,0402,0405,0406,0500", outClusters: "0003,0004,0019", manufacturer: "ShinaSystem", model: "USM-300Z", deviceJoinName: "SiHAS MultiPurpose Sensor", mnmn: "SmartThings", vid: "generic-motion-6"        
    }
    preferences {
        section {
            input "tempOffset"    , "number", title: "Temperature offset", description: "Select how many degrees to adjust the temperature.", range: "-100..100", displayDuringSetup: false
            input "humidityOffset", "number", title: "Humidity offset"   , description: "Enter a percentage to adjust the humidity.", range: "*..*", displayDuringSetup: false
        }
    }
}

private getILLUMINANCE_MEASUREMENT_CLUSTER() { 0x0400 }
private getOCCUPANCY_SENSING_CLUSTER() { 0x0406 }
private getPOWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE() { 0x0020 }
private getTEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE() { 0x0000 }
private getRALATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE() { 0x0000 }
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
    log.debug "Parsing message from device: $description"

    Map map = zigbee.getEvent(description)
    if (!map) {
        if (description?.startsWith('read attr')) {
            Map descMap = zigbee.parseDescriptionAsMap(description)
            if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
                List<Map> descMaps = collectAttributes(descMap)
                def battMap = descMaps.find { it.attrInt == POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE }
                if (battMap) {
                    map = getBatteryResult(Integer.parseInt(battMap.value, 16))
                }
            } else if (descMap?.clusterInt == OCCUPANCY_SENSING_CLUSTER && descMap.attrInt == OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE && descMap?.value) {
                map = getMotionResult(descMap.value == "01" ? "active" : "inactive")
            }
        } else if (description?.startsWith('illuminance:')) { //parse illuminance
            map = parseCustomMessage(description)
        }
    } else if (map.name == "temperature") {
        if (tempOffset) {
            map.value = new BigDecimal((map.value as float) + (tempOffset as float)).setScale(1, BigDecimal.ROUND_HALF_UP)
        }
        map.descriptionText = temperatureScale == 'C' ? "${device.displayName} temperature was ${map.value}°C" : "${device.displayName} temperature was ${map.value}°F"
        map.translatable = true
    } else if (map.name == "humidity") {
        if (humidityOffset) {
            map.value = map.value + (int) humidityOffset
        }
        map.descriptionText = "${device.displayName} humidity was ${map.value}%"
        map.unit = "%"
        map.translatable = true
    }

    def result = map ? createEvent(map) : [:]

    if (description?.startsWith('enroll request')) {
        List cmds = zigbee.enrollResponse()
        result = cmds?.collect { new physicalgraph.device.HubAction(it) }
    }
    log.debug "result: $result"
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
    }
    return result
}

private Map getMotionResult(value) {
    String descriptionText = value == 'active' ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
    return [
        name           : 'motion',
        value          : value,
        descriptionText: descriptionText,
        translatable   : true
    ]
}


/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE)
}

def refresh() {
    def refreshCmds = []

    refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE)

    refreshCmds += zigbee.readAttribute(zigbee.RELATIVE_HUMIDITY_CLUSTER, RALATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
    refreshCmds += zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
    refreshCmds += zigbee.readAttribute(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
    refreshCmds += zigbee.readAttribute(OCCUPANCY_SENSING_CLUSTER, OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE)
    refreshCmds += zigbee.enrollResponse()
    return refreshCmds
}

def configure() {
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
    configCmds += zigbee.configureReporting(zigbee.RELATIVE_HUMIDITY_CLUSTER, RALATIVE_HUMIDITY_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE, DataType.UINT16, 15, 300, 40/*10/100=0.4%*/)
    configCmds += zigbee.configureReporting(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE, DataType.UINT16, 15, 3600, 1/*1 lux*/)
    configCmds += zigbee.configureReporting(OCCUPANCY_SENSING_CLUSTER, OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE, DataType.BITMAP8, 1, 600, 1)
    configCmds += zigbee.configureReporting(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS, DataType.BITMAP16, 0, 0xffff, null) // set : none reporting flag, device sends out notification to the bound devices.
    
    return configCmds + refresh()
}

