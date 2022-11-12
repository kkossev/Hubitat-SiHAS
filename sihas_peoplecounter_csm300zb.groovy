/*
 *  Copyright 2022 SmartThings
 *
 *  Ported for Hubitat Elevation platform by kkossev
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
 *
 * ver. 2.0.0 2022-10-29 kkossev  - first version for HE platform
 * ver. 2.0.1 2022-11-12 kkossev  - 6:21PM analog input binding and configuration reprting OK!
 *
 */
import hubitat.zigbee.zcl.DataType
import hubitat.device.HubMultiAction

metadata {
	definition (name: "SiHAS People Counter", namespace: "shinasys", author: "SHINA SYSTEM") {
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
		capability "Sensor"
		capability "Momentary"
        
        attribute "peopleCounter", "number"
		attribute "freeze",        "enum", ["off", ""]
        attribute "inOutDir",      "enum", ["in", "out", "ready"]
        attribute "batteryVoltage", "string"
        attribute "transationInterval", "number"
        attribute "ledStatus",     "enum", ["true", "false"]
        attribute "inFastStatus",  "enum", ["true", "false"]
        attribute "outFastStatus", "enum", ["true", "false"]
        attribute "rfStatus",      "enum", ["true", "false"]
        attribute "rfPairing",     "enum", ["true", "false"]
        attribute "distanceInit",  "enum", ["true", "false"]
        
        
		//////////////////////////////////////////////////////////////
        // People Counter version description
        //////////////////////////////////////////////////////////////
		// application version > 10 : People Counter V2(TOF) Version (People Counter for Setting : 81~99)
        // application version < 10 : People Counter Version
		//////////////////////////////////////////////////////////////
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0001,000C", outClusters:"0000,0004,0003,0019,0006", model:"CSM-300Z", manufacturer:"ShinaSystem", deviceJoinName: "SiHAS People Counter"
	}
	preferences {
		section {
        	/* korean language
            input (name: "logEnable", type: "bool", title: "Debug logging", description: "<i>Debug information, useful for troubleshooting. Recommended value is <b>false</b></i>", defaultValue: false)
            input (name: "txtEnable", type: "bool", title: "Description text logging", description: "<i>Display sensor states in HE log page. Recommended value is <b>true</b></i>", defaultValue: true)
			input (
					title: "설정 설명", 
					description: "아래 설정은 V2(TOF) 버전에 해당하는 설정입니다.", 
					displayDuringSetup: false, 
					type: "paragraph", 
					element: "paragraph")        
			input ("ledStatus", "boolean", 
					title: "LED 상태 표시 여부", 
					description: "동작 상태를 LED로 표시할지 여부를 설정합니다.", 
					displayDuringSetup: false, 
					defaultValue: "true", 
					required: false)
			input ("transationInterval", "enum",
					title: "트랜잭션간 간격 설정", 
					description: "트랜잭션간 간격을 설정합니다. 연속으로 들어갈 일이 잦을 시 트랜잭션 인터벌을 짧게 반대의 경우 길게 설정하시면 됩니다.", 
					displayDuringSetup: false, 
					options: [0: "지연없음",
							  1: "0.2초",
							  2: "0.4초(기본값)",
							  3: "0.6초",
							  4: "0.8초",
							  5: "1.0초"],
					defaultValue: "2",
					required: false)
			input ("inFastStatus", "boolean", 
					title: "들어갈때 빠른 동작설정", 
					description: "카운터가 0이고 들어갈때 카운터 1로 설정하는것을 한 트랜잭션이 끝나기 전에 빠르게 설정을 할지 여부를 정할수 있습니다.", 
					displayDuringSetup: false, 
					defaultValue: "true", 
					required: false)
			input ("outFastStatus", "boolean", 
					title: "나갈때 빠른 동작설정", 
					description: "카운터가 1이고 나갈때 카운터를 0으로 설정하는것을 한 트랜잭션이 끝나기 전에 빠르게 설정을 할지 여부를 정할수 있습니다.", 
					displayDuringSetup: false, 
					defaultValue: "true", 
					required: false)
			input ("rfStatus", "boolean", 
					title: "RF 통신 동작", 
					description: "시하스 스위치와 연동을 위해서 RF 통신 동작 여부를 설정합니다.", 
					displayDuringSetup: false, 
					defaultValue: "false", 
					required: false)
			input ("rfPairing", "boolean", 
					title: "RF 페어링", 
					description: "시하스 스위치와 RF 페어링을 시작합니다. (먼저 RF통신 동작이 활성화되어야 합니다.)", 
					displayDuringSetup: false, 
					defaultValue: "false", 
					required: false)
			input ("distanceInit", "boolean", 
					title: "거리 재 조정", 
					description: "설치 위치가 바뀌면 거리 재조정을 진행해야합니다. 거리 재 조정 설정을 시작합니다. 5초동안 동작합니다.)", 
					displayDuringSetup: false, 
					defaultValue: "false", 
					required: false)
			*/
            // english version
            input (name: "logEnable", type: "bool", title: "Debug logging", description: "<i>Debug information, useful for troubleshooting. Recommended value is <b>false</b></i>", defaultValue: true)
            input (name: "txtEnable", type: "bool", title: "Description text logging", description: "<i>Display sensor states in HE log page. Recommended value is <b>true</b></i>", defaultValue: true)
            input (title: "Setting Description", description: "The settings below correspond to the V2 (TOF) version.", type: "paragraph", element: "paragraph")        
			input ("ledStatus", "bool", title: "LED status indication", description: "Sets whether the operational status is indicated by LED.", defaultValue: "true", required: false)
			input ("transationInterval", "enum", title: "Set transaction interval", description: "Sets the transaction interval. If you frequently enter consecutively, you can set the transaction interval to be short and long in the opposite case.", 
					displayDuringSetup: false, options: [0: "No delay", 1: "0.2 seconds", 2: "0.4 seconds(default)", 3: "0.6 seconds", 4: "0.8 seconds", 5: "1.0 seconds"], defaultValue: "2", required: false)
			input ("inFastStatus", "bool", title: "Set up quick action when entering", description: "When the counter is zero and enters, you can decide whether to set counter 1 quickly before one transaction ends.", defaultValue: "true", required: false)
			input ("outFastStatus", "bool", title: "Set up quick action when out", description: "When the counter is 1 and leaves, you can decide whether to set the counter to 0 quickly before one transaction ends.", defaultValue: "true", required: false)
			input ("rfStatus", "bool", title: "RF Communication Operation", description: "Set RF communication operation for interworking with SiHAS switch.", defaultValue: "false", required: false)
			input ("rfPairing", "bool", title: "RF Pairing", description: "Start RF pairing with the SiHAS switch. (RF communication operation must be enabled first.)", defaultValue: "false", required: false)
			input ("distanceInit", "bool", title: "Distance readjustment", description: "If the installation location changes, the distance must be readjusted. Start distance re-adjustment setting and operate for 5 seconds.", defaultValue: "false", required: false)
		}
	}
}

private getOCCUPANCY_SENSING_CLUSTER() { 0x0406 }
private getANALOG_INPUT_BASIC_CLUSTER() { 0x000C }
private getPOWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE() { 0x0020 }
private getOCCUPANCY_SENSING_OCCUPANCY_ATTRIBUTE() { 0x0000 }
private getANALOG_INPUT_BASIC_PRESENT_VALUE_ATTRIBUTE() { 0x0055 }

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
			if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
				List<Map> descMaps = collectAttributes(descMap)
				def battMap = descMaps.find { it.attrInt == POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE }
				if (battMap) {
					map = getBatteryResult(Integer.parseInt(battMap.value, 16))
				}
			} else if (descMap?.clusterInt == ANALOG_INPUT_BASIC_CLUSTER && descMap.attrInt == ANALOG_INPUT_BASIC_PRESENT_VALUE_ATTRIBUTE && descMap?.value) {
				map = getAnalogInputResult(Integer.parseInt(descMap.value,16))
			}
		}
	}

	def result = map ? createEvent(map) : [:]

	log.debug "result: $result"
	return result
}

private Map getBatteryResult(rawValue) {
    if (settings?.logEnable) log.trace "${device.displayName} getBatteryResult rawValue=${rawValue}"
	def linkText = getLinkText(device)
	def result = [:]
	def volts = rawValue / 10

	if (!(rawValue == 0 || rawValue == 255)) {
		result.name = 'battery'
		result.translatable = true
		def minVolts = 1.9
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

private Map getAnalogInputResult(value) {
	def application = getDataValue("application")
	int version = zigbee.convertHexToInt(application)
	Float fpc = Float.intBitsToFloat(value.intValue())
	def prevInOut = device.currentState('inOutDir')?.value // in out status
	def prevCnt = device.currentState('peopleCounter')?.value 
	def freezeSts = device.currentState('freeze')?.value
	int pc = ((int)(fpc*10))/10 //people counter
	int inout = ((int)(fpc*10).round(0))%10; // inout direction : .1 = in, .2 = out, .0 = ready	
	 
	if (freezeSts == null) {
		sendEvent(name: "freeze", value: "off", displayed: true, isStateChange: true)		
	}
	
	if (freezeSts == null || freezeSts == "off") { // freeze off
		String inoutString = ( (inout==1) ? "in" : (inout==2) ? "out":"ready")
		String descriptionText1 = "${device.displayName} : $pc"
		String descriptionText2 = "${device.displayName} : $inoutString"

		log.debug "[$fpc] = people: $pc, dir: $inout, $inoutString"

		String motionActive = pc ? "active" : "inactive"
		sendEvent(name: "motion", value: motionActive, displayed: true, isStateChange: false)

		if((inoutString != "ready") && (prevInOut == inoutString)) {
			sendEvent(name: "inOutDir", value: "ready", displayed: true)
		}

		sendEvent(name: "inOutDir", value: inoutString, displayed: true, descriptionText: descriptionText2)
		if ( version > 10 && pc > 80 && pc < 100) { // version > 10 and People Count > 80 and People Count < 100 : TOF Setting Value, so ignore
			pc = prevCnt.toInteger()
		}		
		return [
			name           : 'peopleCounter',
			value          : pc,
			descriptionText: descriptionText1,
			translatable   : true
		]
	} else { // freeze on
		String descriptionText1 = "${device.displayName} : $prevCnt"
		pc = prevCnt.toInteger()
		return [
			name           : 'peopleCounter',
			value          : pc,
			descriptionText: descriptionText1,
			translatable   : true
		]
	}
}

def setPeopleCounter(peoplecounter) {
	int pc =  Float.floatToIntBits(peoplecounter);
	log.debug "SetPeopleCounter = $peoplecounter"
	return zigbee.writeAttribute(ANALOG_INPUT_BASIC_CLUSTER, ANALOG_INPUT_BASIC_PRESENT_VALUE_ATTRIBUTE, DataType.FLOAT4, pc, [:], 250)
}

def setFreeze(freezeSts) {
	def application = getDataValue("application")
	int version = zigbee.convertHexToInt(application)
    
	sendEvent(name: "freeze", value: freezeSts, displayed: true, isStateChange: true)
	if( freezeSts == "on") {
		if ( version > 10 ) {
			return setPeopleCounter(82)
		}
	} else {
		if ( version > 10 ) {
			return setPeopleCounter(84)
		} else {
        	return zigbee.readAttribute(ANALOG_INPUT_BASIC_CLUSTER, ANALOG_INPUT_BASIC_PRESENT_VALUE_ATTRIBUTE)	
        }
	}
	return null
}

def push() {
	setPeopleCounter(0)
}
/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE)
}

def updated() {
    ArrayList<String> cmds = []
	def application = getDataValue("application")
	int version = 99
    if (application != null) {
        version = zigbee.convertHexToInt(application)
    }
    else {
        log.warn "${device.displayName} application version not found! Assuming new SiHAS People Counter ..."
    }
	
	if (version > 10) { // version > 10 and People Count > 80 and People Count < 100 : TOF Setting Value
		
		def ledStatusRet = (ledStatus != null) ? ledStatus : "true"
		if (ledStatusRet != device.latestValue("ledStatus")) {
			sendEvent(name: "ledStatus", value: ledStatusRet, descriptionText: "ledStatus set to ${ledStatusRet}")
			if ( ledStatusRet == "true") {
				cmds += setPeopleCounter(86)
			} else {
				cmds += setPeopleCounter(87)
			}
		}
		def transationIntervalRet = (transationInterval != null) ? transationInterval : "2"		
		if (transationIntervalRet != device.latestValue("transationInterval")) {
			sendEvent(name: "transationInterval", value: transationIntervalRet, descriptionText: "transationInterval set to ${transationIntervalRet}")
			if ( transationIntervalRet == "0") {
				cmds += setPeopleCounter(90)
			} else if ( transationIntervalRet == "1") {
				cmds += setPeopleCounter(91)
			} else if ( transationIntervalRet == "2") {
				cmds += setPeopleCounter(92)
			} else if ( transationIntervalRet == "3") {
				cmds += setPeopleCounter(93)
			} else if ( transationIntervalRet == "4") {
				cmds += setPeopleCounter(94)
			} else if ( transationIntervalRet == "5") {
				cmds += setPeopleCounter(95)
			}			
		}
		def inFastStatusRet = (inFastStatus != null) ? inFastStatus : "true"
		if (inFastStatusRet != device.latestValue("inFastStatus")) {
			sendEvent(name: "inFastStatus", value: inFastStatusRet, descriptionText: "inFastStatus set to ${inFastStatusRet}")
			if ( inFastStatusRet == "true") {
				cmds += setPeopleCounter(96)
			} else {
				cmds += setPeopleCounter(97)
			}
		}
		def outFastStatusRet = (outFastStatus != null) ? outFastStatus : "true"
		if (outFastStatusRet != device.latestValue("outFastStatus")) {
			sendEvent(name: "outFastStatus", value: outFastStatusRet, descriptionText: "outFastStatus set to ${outFastStatusRet}")
			if ( outFastStatusRet == "true") {
				cmds += setPeopleCounter(98)
			} else {
				cmds += setPeopleCounter(99)
			}
		}
		def rfStatusRet = (rfStatus != null) ? rfStatus : "false"
		if (rfStatusRet != device.latestValue("rfStatus")) {
			sendEvent(name: "rfStatus", value: rfStatusRet, descriptionText: "rfStatus set to ${rfStatusRet}")
			if ( rfStatusRet == "true") {
				cmds += setPeopleCounter(88)
			} else {
				cmds += setPeopleCounter(89)
			}
		}
		def rfPairingRet = (rfPairing != null) ? rfPairing : "false"
		if (rfPairingRet != device.latestValue("rfPairing")) {
			sendEvent(name: "rfPairing", value: rfPairingRet, descriptionText: "rfPairing set to ${rfPairingRet}")
			if ( rfPairingRet == "true") {
				cmds += setPeopleCounter(81)
			}
		}
		def distanceInitRet = (distanceInit != null) ? distanceInit : "false"
		if (distanceInitRet != device.latestValue("distanceInit")) {
			sendEvent(name: "distanceInit", value: distanceInitRet, descriptionText: "distanceInit set to ${distanceInitRet}")
			if ( distanceInitRet == "true") {
				cmds += setPeopleCounter(83)
			}
		}
        sendZigbeeCommands(cmds)
	} //if (version > 10)
    return null
}

void sendZigbeeCommands(ArrayList<String> cmds) {
    if (logEnable) {log.trace "${device.displayName} sendZigbeeCommands(cmds=$cmds)"}
    hubitat.device.HubMultiAction allActions = new hubitat.device.HubMultiAction()
    cmds.each {
            allActions.add(new hubitat.device.HubAction(it, hubitat.device.Protocol.ZIGBEE))
    }
    sendHubCommand(allActions)
}


def refresh() {
    ArrayList<String> refreshCmds = []
	refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE, [:], 250)
	refreshCmds += zigbee.readAttribute(ANALOG_INPUT_BASIC_CLUSTER, ANALOG_INPUT_BASIC_PRESENT_VALUE_ATTRIBUTE, [:], 250)	
	//return refreshCmds
    sendZigbeeCommands( refreshCmds )
}


private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}

private List readDeviceBindingTable() {
   ["he raw 0x${device.deviceNetworkId} 0 0 0x0033 {00 00} {0x0000}", "delay 200",]
}


def configure() {
    def zigbeeEuiReversed = swapEndianHex(device.hub.zigbeeEui)
    def zigbeeIdReversed  = swapEndianHex(device.zigbeeId)
    log.trace "device.hub.zigbeeEuiReversed = $zigbeeEuiReversed   device.zigbeeIdReversed=${device.zigbeeId}  device.deviceNetworkId=${device.deviceNetworkId}"
    ArrayList<String> configCmds = []
    // ZDO: Binding Table Request (Cluster ID: 0x0033)
    //configCmds += readDeviceBindingTable()
    
	configCmds += zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, POWER_CONFIGURATION_BATTERY_VOLTAGE_ATTRIBUTE, DataType.UINT8, 30, 21600, 0x01, [:], 250)    // 100mV
    //configCmds += zigbee.configureReporting(ANALOG_INPUT_BASIC_CLUSTER, ANALOG_INPUT_BASIC_PRESENT_VALUE_ATTRIBUTE, 0x39/* DataType.FLOAT4*/, (int)1, 600, 1, [:], 250)
    configCmds += ["he raw 0x${device.deviceNetworkId} 0 0 0x0021 {00 $zigbeeIdReversed 01 0c 00 03 $zigbeeEuiReversed 01} {0x0000}", "delay 200",]            // bind request - OK, except the Frame Control Field which is always 0x40 in HE ( Acknowledgement Reques: true )
    configCmds += ["he cr 0x${device.deviceNetworkId} 0x01 0x000c 0x0055 0x39 0x0000 0x0258 {0ad7233c}", "delay 200",]                                         // {0ad7233c} = Float: 0.01 
    
 
	//configCmds += refresh()
    
    sendZigbeeCommands( configCmds )
    /*
	refresh()
*/
}

def installed() {
	if (settings?.txtEnable) log.info "${device.displayName} installed"
	sendEvent(name: "ledStatus", value:"true")
    sendEvent(name: "transationInterval", value:"2")
    sendEvent(name: "inFastStatus", value:"true")
    sendEvent(name: "outFastStatus", value:"true")
    sendEvent(name: "rfStatus", value:"false")
    sendEvent(name: "rfPairing", value:"false")
    sendEvent(name: "distanceInit", value:"false")
}