/**
 *	Copyright 2022 SmartThings
 *
 *  Imported for Hubitat Elevation platform by kkossev 2022/11/01 12:40 PM ver. 2.0.1 
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
import hubitat.zigbee.zcl.DataType

metadata {
	definition (name: "SiHAS Zigbee Power Meter", namespace: "shinasys", author: "SHINA SYSTEM") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Refresh"
		capability "Sensor"
		capability "Configuration"
		capability "Voltage Measurement"
		capability "Current Meter"
		//capability "frequencyMeasurement"
		//capability "powerfactorMeasurement"
		capability "Temperature Measurement"
		capability "Switch"

        attribute "powerFactor", "number"
        
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0B04,0702,0402", outClusters:"0004,0019", model:"PMM-300Z1", manufacturer:"ShinaSystem", deviceJoinName: "SiHAS Power Meter PMM-300Z1"        // SIHAS Power Meter 01 0104 0000 01 05 0000 0004 0003 0B04 0702 02 0004 0019
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0B04,0702,0402", outClusters:"0004,0019", model:"PMM-300Z2", manufacturer:"ShinaSystem", deviceJoinName: "SiHAS Energy Monitor PMM-300Z2"     // Single Phase, SIHAS Power Meter 01 0104 0000 01 06 0000 0004 0003 0B04 0702 0402 02 0004 0019
        fingerprint profileId:"0104", endpointId:"01", inClusters:"0000,0004,0003,0B04,0702,0402", outClusters:"0004,0019", model:"PMM-300Z3", manufacturer:"ShinaSystem", deviceJoinName: "SiHAS Energy Monitor PMM-300Z3"     // Three Phase,  SIHAS Power Meter 01 0104 0000 01 06 0000 0004 0003 0B04 0702 0402 02 0004 0019
      
	}
}

def getATTRIBUTE_READING_INFO_SET() { 0x0000 }
def getATTRIBUTE_HISTORICAL_CONSUMPTION() { 0x0400 }
def getATTRIBUTE_ACTIVE_POWER() { 0x050B }
def getATTRIBUTE_FREQUENCY() { 0x0300 }
def getATTRIBUTE_VOLTAGE() { 0x0505 }
def getATTRIBUTE_CURRENT() { 0x0508 }
def getATTRIBUTE_POWERFACTOR() { 0x0510 }
def getTEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE() { 0x0000 }

def convertHexToInt24Bit(value) {
	int result = zigbee.convertHexToInt(value)
	if (result & 0x800000) {
		result |= 0xFF000000
	}
	return result
}

def parse(String description) {
	log.debug "description is $description"
	def event = zigbee.getEvent(description)
	def descMap = zigbee.parseDescriptionAsMap(description)

	if (event) {
		log.info "event enter:$event"
		if (event.name == "switch") {
			return sendEvent(event)
		} else if (event.name == "temperature") {
			return sendEvent(event)
		}
	}

	if (descMap) {
		List result = []
		log.debug "Desc Map: $descMap"

		List attrData = [[clusterInt: descMap.clusterInt, attrInt: descMap.attrInt, value: descMap.value]]
		descMap.additionalAttrs.each {
			attrData << [clusterInt: descMap.clusterInt, attrInt: it.attrInt, value: it.value]
		}
		attrData.each {
			def map = [:]
			if (it.value != null) {
				if (it.clusterInt == zigbee.METERING_CLUSTER && it.attrInt == ATTRIBUTE_HISTORICAL_CONSUMPTION) {
					log.debug "meter"
					map.name = "power"
					map.value = convertHexToInt24Bit(it.value)/powerDivisor
					map.unit = "W"
				} else if (it.clusterInt == zigbee.METERING_CLUSTER && it.attrInt == ATTRIBUTE_READING_INFO_SET) {
					log.debug "energy"
					map.name = "energy"
					map.value = zigbee.convertHexToInt(it.value)/energyDivisor
					map.unit = "kWh"						
				} else if (it.clusterInt == zigbee.ELECTRICAL_MEASUREMENT_CLUSTER && it.attrInt == ATTRIBUTE_FREQUENCY) {
					log.debug "frequency"
					map.name = "frequency"
					map.value = zigbee.convertHexToInt(it.value)/frequencyDivisor
					map.unit = "Hz"
				} else if (it.clusterInt == zigbee.ELECTRICAL_MEASUREMENT_CLUSTER && it.attrInt == ATTRIBUTE_VOLTAGE) {
					log.debug "voltage"
					map.name = "voltage"
					map.value = zigbee.convertHexToInt(it.value)/voltageDivisor
					map.unit = "V"
				} else if (it.clusterInt == zigbee.ELECTRICAL_MEASUREMENT_CLUSTER && it.attrInt == ATTRIBUTE_CURRENT) {
					log.debug "current"
					map.name = "current"
					map.value = zigbee.convertHexToInt(it.value)/currentDivisor
					map.unit = "A"
				} else if (it.clusterInt == zigbee.ELECTRICAL_MEASUREMENT_CLUSTER && it.attrInt == ATTRIBUTE_POWERFACTOR) {
					log.debug "power factor $it.value"
					map.name = "powerFactor"
					map.value = (byte) zigbee.convertHexToInt(it.value)/powerFactorDivisor
					map.unit = "%"
				} else if (it.clusterInt == zigbee.TEMPERATURE_MEASUREMENT_CLUSTER && it.attrInt == TEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE) {
					log.debug "temperature"
					map.name = "temperature"
					map.unit = getTemperatureScale()
					map.value = zigbee.parseHATemperatureValue("temperature: " + (zigbee.convertHexToInt(it.value)), "temperature: ", tempScale)
					log.debug "${device.displayName}: Reported temperature is ${map.value}°$map.unit"
				}
			}
				
			if (map) {
				result << createEvent(map)
			}
			log.debug "Parse returned $map result is: ${result}"
		} // for each attribute
        //log.debug "result=${result}" 
		return result
	} // if (descMap)
}

def off() {
	zigbee.off()
}

def on() {
	zigbee.on()
}

def refresh() {
	log.debug "refresh "
	zigbee.onOffRefresh() +
	zigbee.electricMeasurementPowerRefresh() +
	zigbee.readAttribute(zigbee.METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET) + 
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_FREQUENCY) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_VOLTAGE) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_CURRENT) +
	zigbee.readAttribute(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_POWERFACTOR) +
	zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE)
}

def configure() {
	def configCmds = []
	// this device will send instantaneous demand and current summation delivered every 1 minute

	log.debug "Configuring Reporting"
	configCmds = zigbee.onOffConfig() +
		zigbee.configureReporting(zigbee.METERING_CLUSTER, ATTRIBUTE_HISTORICAL_CONSUMPTION, DataType.INT24, 5, 600, 1) +
		zigbee.configureReporting(zigbee.METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET, DataType.UINT48, 5, 600, 1) +
		zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_FREQUENCY, DataType.UINT16, 10, 600, 3) + /* 3 unit : 0.3Hz */
		zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_VOLTAGE, DataType.UINT16, 5, 600, 3) + /* 3 unit : 0.3V */
		zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_CURRENT, DataType.UINT16, 5, 600, 1) + /* 1 unit : 0.01A */
		zigbee.configureReporting(zigbee.ELECTRICAL_MEASUREMENT_CLUSTER, ATTRIBUTE_POWERFACTOR, DataType.INT8, 10, 600, 1) + /* 1 unit : 0.1% */
		zigbee.configureReporting(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASUREMENT_MEASURED_VALUE_ATTRIBUTE, DataType.INT16, 20, 300, 10 /* 1 uint : 0.1C */)
	return configCmds + refresh()
}

private getActivePowerDivisor() { 1 }
private getPowerDivisor() { 1 }
private getEnergyDivisor() { 1000 }
private getFrequencyDivisor() { 10 }
private getVoltageDivisor() { 10 }
private getCurrentDivisor() { 100 }
private getPowerFactorDivisor() { 1 }
