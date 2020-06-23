metadata {
    definition (name: "Soilmoisture", namespace: "magnusa", author: "magnusaga", version: "1.1.0") {
        capability "Relative Humidity Measurement"
        capability "Refresh"
        capability : "Text"
    }

    preferences {
        input("rpiip", "string",
            title: "Arduino Adress",
            description: "Arduino IP adress",
            required: true,
            defaultValue:"192.168.1.150",
            displayDuringSetup: true
        )
        	input("rpiport", "string",
            title: "Arduino Port",
            description: "Arduino Port",
            defaultValue:80,
            required: true,
            displayDuringSetup: true
        )
            input("poller", "enum",
       	 	options: ["one","five","fifteen","twenty"],
            title: "Polling time",
            description: "Polling time in minutes",
            defaultValue:"one",
            required: true,
            displayDuringSetup: true
        )
    }

    simulator {

    }
    
    main("switch")


}
def initialized() {
   log.debug "init"
   //runEvery5Minutes(handler)
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    refreshHandler()
    initialized()
 
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    refreshHandler()

}

def handler(){
   log.debug "updating every minute"
refreshHandler();
}


// Returns the address of the hub itself
private String getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

def refreshHandler() {
    log.debug "Refreshing"
	if(!rpiip || !rpiport){
    log.debug "no ip or port"
    return
    }
    
    // Setting Network Device Id
    def iphex = convertIPtoHex(rpiip)
    def porthex = convertPortToHex(rpiport)
    if (device.deviceNetworkId != "$iphex:$porthex") {
        device.deviceNetworkId = "$iphex:$porthex"
    }
    log.debug "Device Network Id set to ${device.deviceNetworkId}"

    def headers = [:]
    headers.put("HOST", "$rpiip:$rpiport")
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/sensor",
        headers: headers,
        query: [
            callback: getCallBackAddress()
        ]
    )
    hubAction
}

// Parse events into attributes
def parse(String description) {
	log.debug "answer"
    //{"distance":"asd","moisture":688}
    def msg = parseLanMessage(description)
 
    if(msg?.json?.moisture){
    log.debug "Installed with settings: ${msg?.json}"
        def percentage = (msg?.json?.moisture/700)*100
    sendEvent(name: "humidity", value: Math.floor(percentage)) 
    }else {
    if (sendPush) {
        sendPush("Cant get data")
    }
    }

  
 
}

private String getRPiAddress() {
    return "${rpiip}:${rpiport}"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}
