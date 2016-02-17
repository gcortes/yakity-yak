/**
 * Yakity-Yak
 * Derived from LANnouncer Alerter by Tony McNamara
 *  
 *  Version 1.0, 17 Feb 2016
 *
 *  Copyright 2016 William C. Rowe
 *
 *	This Device Handler will interface with any server running on a local LAN. It sends commands
 *	in a standard query string and expects completion events in response. I have also written
 *	a coresponding server in Ruby that uses OS X application to carry out requests.
 *
 *	For those too young to remember, and that's most people, the name of the app is based on the 1958
 *	hit by the Coasters called "Yakety Yak". Don't talk back.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
    definition (name: "Yakity-Yak", namespace: "Datasyne", author: "William C. Rowe") {
        capability "Alarm"
        capability "Speech Synthesis"
        capability "Notification"
        capability "Tone"
        attribute  "Yakity-Yak","string"
        attribute "alarm", "string"
        /* SmartThings suggests adding the followiing */
        capability "Sensor"
        capability "Actuator"
    }
    preferences {
        input("DeviceLocalLan", "string", title:"Server IP Address",
        	description:"Your server's I.P. address", defaultValue:"" ,
        	required: false, displayDuringSetup: true)
        input("DevicePort", "string", title:"Server Port",
    	    description:"The port that the server listens on", defaultValue:"1035",
    	    required: false, displayDuringSetup: true)
    }

    simulator {
        
    }

    tiles {
        standardTile("alarm", "device.alarm", width: 2, height: 2) {
            state "off", label:'off', action:'alarm.both', icon:"st.alarm.alarm.alarm",
            	backgroundColor:"#ffffff"
            state "strobe", label:'strobe', action:'alarm.off', icon:"st.Lighting.light11",
            	backgroundColor:"#e86d13"
            state "siren", label:'siren', action:'alarm.off', icon:"st.alarm.alarm.alarm",
            	backgroundColor:"#e86d13"
        	state "both", label:'alarm', action:'alarm.off', icon:"st.alarm.alarm.alarm",
            	backgroundColor:"#e86d13"
        }
        standardTile("strobe", "device.alarm", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"alarm.strobe", icon:"st.secondary.strobe"
        }        
        standardTile("siren", "device.alarm", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"alarm.siren", icon:"st.secondary.siren"
        }             
        standardTile("speak", "device.speech", inactiveLabel: false, decoration: "flat") 
        {
            state "default", label:'Speak', action:"Speech Synthesis.speak", icon:"st.Electronics.electronics13"
        }
        standardTile("toast", "device.notification", inactiveLabel: false, decoration: "flat") {
            state "default", label:'Notify', action:"notification.deviceNotification", icon:"st.Kids.kids1"
        }
        standardTile("beep", "device.tone", inactiveLabel: false, decoration: "flat") {
            state "default", label:'Tone', action:"tone.beep", icon:"st.Entertainment.entertainment2"
        }

        main (["alarm"]);
        details(["alarm","strobe","siren","speak","toast","beep"]);
    }
}

String getVersion() {return "1.0";}

// handle commands
def off() {
    log.debug "YY - executing 'off'"
    def command="?alarm=off"
    sendCommand(command)
}

def strobe() {
    log.debug "YY - executing 'strobe'"
    def command="?alarm=strobe"
    sendCommand(command)
}

def siren() {
    log.debug "YY - executing 'siren'"
    def command="?alarm=siren"
    sendCommand(command)
}

def beep() {
    log.debug "YY - executing 'beep'"
    def command="?tone=beep"
    sendCommand(command)
}
def both() {
    log.debug "YY - executing 'both'"
    def command="?alarm=both"
    sendCommand(command)
}

def speak(speech) {
    log.debug "YY - executing 'speak'"
    if (!speech?.trim()) {
        speech = "Yakity Yak Version ${version}"
    }
    if (speech?.trim()) {
		speech = URLEncoder.encode(speech)
	    def command="?speak="+speech
        sendCommand(command)
    }
}

def deviceNotification(textMsg) {
    log.debug "YY - executing notification with "+textMsg
    if (!textMsg?.trim()) {
        textMsg = "Yakity Yak Version ${version}";
    }
    if (textMsg?.trim()) {
		textMsg = URLEncoder.encode(textMsg)
        def command="?text="+textMsg
        sendCommand(command)
    }
}    

/* Send to IP  */
private sendCommand(queryString) {
    log.info "YY - sending command "+ queryString+" to "+DeviceLocalLan+":"+DevicePort
    if (DeviceLocalLan?.trim()) {
        def hosthex = convertIPtoHex(DeviceLocalLan)
        def porthex = convertPortToHex(DevicePort)
        device.deviceNetworkId = "$hosthex:$porthex"

        def headers = [:] 
        headers.put("HOST", "$DeviceLocalLan:$DevicePort")

        def method = "GET"

        def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: "/"+queryString,
            headers: headers
            );

		log.debug hubAction
        hubAction;
    }
}
def parse(String description) {
    log.debug "YY - parsing '${description}'"
    
    def msg = parseLanMessage(description);
	def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response

	log.debug "YY - as LAN: " + msg;
    log.debug "YY - message Body: " + body;
    if (body == "strobe:on") {
		log.debug "Parse: strobe on received. Sending event";
    	sendEvent(name: "alarm", value: "strobe")
    }
    if (body == "strobe:off") {
		log.debug "Parse: strobe off received. Sending event";
    	sendEvent(name: "alarm", value: "off")
    }
    if (body == "siren:on") {
		log.debug "Parse: siren on received. Sending event";
    	sendEvent(name: "alarm", value: "siren")
    }
    if (body == "siren:off") {
		log.debug "Parse: siren off received. Sending event";
    	sendEvent(name: "alarm", value: "off")
    }
    if (body == "both:on") {
		log.debug "Parse: siren and strobe on received. Sending event";
    	sendEvent(name: "alarm", value: "both")
    }
    if (body == "both:off") {
		log.debug "Parse: siren and strobe off received. Sending event";
    	sendEvent(name: "alarm", value: "off")
    }
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    log.debug hexport
    return hexport
}
