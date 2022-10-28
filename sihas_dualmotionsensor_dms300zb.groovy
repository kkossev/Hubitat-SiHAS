/*
 *  Copyright 2021 SmartThings
 *
 *  Ported for Hubitat Elevation platform by kkossev 2022/10/28 11:52 PM ver. 2.0.0
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
	definition (name: "SiHAS Dual Motion Sensor", namespace: "shinasys", author: "SHINA SYSTEM") {
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
        capability "Sensor"		
        
        attribute "batteryVoltage", "string"
        attribute "motionInterval", "number"
        attribute "motionIn",  "enum", ["active", "inactive"]
        attribute "motionOut", "enum", ["active", "inactive"]
        attribute "motionAnd", "enum", ["active", "inactive"]
        
		// dual motion sensor : in(right),out(left) motion sensor -> motion(AND) = in & out, motion(OR) = in | out 
		fingerprint profileId:"0104", endpointId:"01", inClusters: "0000,0003,0406,0001,0500", outClusters: "0004,0003,0019", manufacturer: "ShinaSystem", model: "DMS-300Z", deviceJoinName: "SiHAS Dual Motion Sensor"
	}
	preferences {
		section {
            input (name: "logEnable", type: "bool", title: "Debug logging", description: "<i>Debug information, useful for troubleshooting. Recommended value is <b>false</b></i>", defaultValue: false)
            input (name: "txtEnable", type: "bool", title: "Description text logging", description: "<i>Display sensor states in HE log page. Recommended value is <b>true</b></i>", defaultValue: true)
			input "motionInterval", "number", title: "Motion Interval", description: "<i>What is the re-sensing time (seconds) after the motion sensor is detected.</i>", range: "1..100", defaultValue: 5, required: true, displayDuringSetup: true
		}
	}
}

private getOCCUPANCY_SENSING_CLUSTER() { 0x0406 }
private getATTRIBUTE_IAS_ZONE_STATUS() { 0x0000 }
private getPOWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE() { 0x0020 }
private getOCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE() { 0x0000 }
private getOCCUPIED_TO_UNOCCUPIED_DELAY_ATTRIBUTE() { 0x0010 }

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
        //log.trace "no event map!"
		if (description?.startsWith('zone status')) {
            //log.trace "zone status"
			map = parseIasMessage(description)
		} 
        else {
		    Map descMap = zigbee.parseDescriptionAsMap(description)
            if (settings?.logEnable) {log.debug "${device.displayName} descMap: $descMap"}
			if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
				List<Map> descMaps = collectAttributes(descMap)
				def battMap = descMaps.find { it.attrInt == POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE }
				if (battMap) {
					map = getBatteryResult(Integer.parseInt(battMap.value, 16))
				}
			} 
            else if (descMap?.clusterInt == zigbee.IAS_ZONE_CLUSTER && descMap.attrInt == ATTRIBUTE_IAS_ZONE_STATUS && descMap.commandInt != 0x07) {  // out : motion sensor
                //log.trace "out : motion sensor"
				def zs = new ZoneStatus(zigbee.convertHexToInt(descMap.value))
				map = translateZoneStatus(zs)
			} 
            else if (descMap?.clusterInt == OCCUPANCY_SENSING_CLUSTER && descMap.attrInt == OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE && descMap?.value) { // in : motion sensor
                //log.trace "in : motion sensor"
				def inMotion = descMap.value == "01" ? "active" : "inactive"
				def outMotion  = device.latestState('motionOut')?.value
				sendDualMotionResult("motionIn", inMotion)  
				sendDualMotionResult("motionAnd", (inMotion == "active" && outMotion == "active") ? "active":"inactive")
				map = (inMotion == "active" || outMotion == "active") ? getMotionOrResult('active') : getMotionOrResult('inactive')
			} 
            else if (descMap?.clusterInt == OCCUPANCY_SENSING_CLUSTER && descMap.attrInt == OCCUPIED_TO_UNOCCUPIED_DELAY_ATTRIBUTE && descMap?.value) {
                //log.trace "motionInterval"
				def interval = zigbee.convertHexToInt(descMap.value)
				if (settings?.logEnable) log.debug "${device.displayName} interval = [$interval]"
				map = [name:'motionInterval',value: interval]
			}
            else if (descMap?.clusterId == "0500" && descMap?.command == "04") {    //write attribute response (IAS)
                if (settings?.logEnable) log.debug "${device.displayName} IAS enroll write attribute response is ${descMap?.data[0] == "00" ? "success" : "<b>FAILURE</b>"}"
            } 
            else if (descMap?.cluster == "0500" && descMap?.command in ["01", "0A"] ) {    //IAS read attribute response
                if (settings?.logEnable) log.debug "${device.displayName} IAS read attribute ${descMap?.attrId} response is ${descMap?.value}"
                if (descMap?.attrId == "0000") {
                    if (settings?.logEnable) log.debug "${device.displayName} Zone State repot ignored value= ${Integer.parseInt(descMap?.value, 16)}"
                }
                else if (descMap?.attrId == "0002") {
                    if (settings?.logEnable) log.debug "${device.displayName} Zone status repoted: descMap=${descMap} value= ${Integer.parseInt(descMap?.value, 16)}"
                    //handleMotion(Integer.parseInt(descMap?.value, 16))
                } else if (descMap?.attrId == "000B") {
                    if (settings?.logEnable) log.debug "${device.displayName} IAS Zone ID: ${descMap.value}" 
                }
                else if (descMap?.attrId == "0013") {
                    def value = Integer.parseInt(descMap?.value, 16)
                    if (settings?.txtEnable) log.info "${device.displayName} Current Zone Sensitivity Level = ${value}"
                }
                else {
                    if (settings?.logEnable) log.warn "${device.displayName} Zone status: NOT PROCESSED ${descMap}" 
                }
            } // if IAS read attribute response            
            else {
                if (settings?.logEnable) log.warn "${device.displayName} UNPROCESSED!"
            }
		} // if no event
	}

	def result = map ? createEvent(map) : [:]

	if (description?.startsWith('enroll request')) {
		//List cmds = zigbee.enrollResponse()
		//result = cmds?.collect { new hubitat.device.HubAction(it) }
        if (settings?.logEnable) log.info "${device.displayName} Sending IAS enroll response..."
        ArrayList<String> cmds = zigbee.enrollResponse() + zigbee.readAttribute(0x0500, 0x0000)
        if (settings?.logEnable) log.debug "${device.displayName} enroll response: ${cmds}"
        sendZigbeeCommands( cmds )  
	}
	if (settings?.logEnable) log.debug "${device.displayName} result: $result"
	return result
}
// out : motion sensor
private Map parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)
	translateZoneStatus(zs)
}

private Map translateZoneStatus(ZoneStatus zs) {
	def inMotion  = device.latestState('motionIn')?.value
	def outMotion = (zs.isAlarm1Set() || zs.isAlarm2Set()) ? "active" : "inactive"
	sendDualMotionResult("motionOut", outMotion)
	sendDualMotionResult("motionAnd", (inMotion == "active" && outMotion == "active") ? "active":"inactive")
	return (inMotion == "active" || outMotion == "active") ? getMotionOrResult('active') : getMotionOrResult('inactive')
}

private Map getBatteryResult(rawValue) {
    if (settings?.logEnable) log.trace "${device.displayName} getBatteryResult rawValue=${rawValue}"
	def linkText = getLinkText(device)
	def result = [:]
	def volts = rawValue / 10

	if (!(rawValue == 0 || rawValue == 255)) {
		result.name = 'battery'
		result.translatable = true
		def minVolts = 2.2
		def maxVolts = 3.1
		
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		def roundedPct = Math.round(pct * 100)
		if (roundedPct <= 0)
			roundedPct = 1
		result.value = Math.min(100, roundedPct)
		result.descriptionText = "${device.displayName} battery was ${result.value}%"        
	}
	return result
}

private sendDualMotionResult(name, value) {
	String descriptionText = value == 'active' ? "${device.displayName} ${name} detected motion" : "${device.displayName}  ${name} has stopped"
	if (settings?.txtEnable) log.info "${device.displayName} $name = $value: $descriptionText"
	
	sendEvent(name: name, value: value, descriptionText: descriptionText,translatable   : true)    
}

private Map getMotionOrResult(value) {
	String descriptionText = value == 'active' ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
	return [
		name           : 'motion',
		value          : value,
		descriptionText: descriptionText,
		translatable   : true
	]
}



def updated() {
	if (settings?.txtEnable) log.info "${device.displayName} device updated $motionInterval"
	
	//set reportingInterval = 0 to trigger update
	if (isMotionIntervalChange()) {
		sendEvent(name: "motionInterval", value: getMotionReportInterval(), descriptionText: "Motion interval set to ${getMotionReportInterval()} seconds")
        ArrayList<String> cmds = zigbee.writeAttribute(OCCUPANCY_SENSING_CLUSTER, OCCUPIED_TO_UNOCCUPIED_DELAY_ATTRIBUTE, DataType.UINT16, getMotionReportInterval() as int, [:], 250)
		sendZigbeeCommands( cmds )
	}
}

//has interval been updated
def isMotionIntervalChange() {
	if (settings?.logEnable) log.debug "${device.displayName} isMotionIntervalChange ${getMotionReportInterval()} <- ${device.latestValue("motionInterval")}"
	return (getMotionReportInterval() != device.latestValue("motionInterval"))
}

//settings default interval
def getMotionReportInterval() {
	return (motionInterval != null ? motionInterval : 5)
}

def refresh() {
    if (logEnable) {log.debug "${device.displayName} refresh()..."}
	ArrayList<String> refreshCmds = []

	refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE, [:], 250)
	refreshCmds += zigbee.readAttribute(OCCUPANCY_SENSING_CLUSTER, OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE, [:], 250)
	refreshCmds += zigbee.readAttribute(OCCUPANCY_SENSING_CLUSTER, OCCUPIED_TO_UNOCCUPIED_DELAY_ATTRIBUTE, [:], 250)
	refreshCmds += zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, ATTRIBUTE_IAS_ZONE_STATUS, [:], 250)        
	refreshCmds += zigbee.enrollResponse(delay=250)
	sendZigbeeCommands( refreshCmds )
}

def configure() {
	def configCmds = []
    configCmds += zigbee.enrollResponse() + zigbee.readAttribute(0x0500, 0x0000)
	configCmds += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE, DataType.UINT8, 30, 21600, 0x01/*100mv*1*/, [:], 250)
	configCmds += zigbee.configureReporting(OCCUPANCY_SENSING_CLUSTER, OCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE, DataType.BITMAP8, 1, 600, 1, [:], 250)
	configCmds += zigbee.configureReporting(zigbee.IAS_ZONE_CLUSTER, ATTRIBUTE_IAS_ZONE_STATUS, DataType.BITMAP16, 0, 0xffff, null, [:], 250) // set : none reporting flag, device sends out notification to the bound devices.
    sendZigbeeCommands( configCmds )
	refresh()
}

void sendZigbeeCommands(ArrayList<String> cmds) {
    if (logEnable) {log.trace "${device.displayName} sendZigbeeCommands(cmds=$cmds)"}
    hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    cmds.each {
            allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
    }
    sendHubCommand(allActions)
}
