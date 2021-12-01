package com.example.uberclone.model.Model.EventBus

class DriveRequestReceived {
    var key: String? = null
    var pickupLocation: String? = null
    var pickupLocationString: String? = null
    var destinationLocation: String? = null
    var destinationLocationString: String? = null
    var distanceValue = 0
    var durationValue = 0
    var totalFee = 0.0
    var distanceText: String? = ""
    var durationText: String? = ""
}