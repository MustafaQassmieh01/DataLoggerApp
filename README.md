# DataLoggerApp
My remake of the mobile Dev Project
so Ill make this short on what the app is supposed to do once Im finished
*features*
* first connect to a bluetooth device
* Master and slave implemented :
  * i.e. master will send the slave commands and the slave will submit results or reply 
  * initiation will be on the Master side
* Slave will send data from the sensors
* Master will have a list of commands (the requirement in the actaul app was RS232 so thats what Ill be running with Ill be sure to include a documentation regarding how the cmds work)
* all this on android 9 and up meaning you have to implement proper checks for each API level from Pie (28) -> vanilla_ice_cream (35) [kinda makes sense, because you might want to use an old phone as a sensor not a $1200 mobile with the latest features to test temprature of a room or rotation in a plane]
* alarm sound system
* channels for different sensors
* graphical interphase and proper visualization of data

# for later in the project:
 it would be interesting to have a form of uploading this into the cloud and capturing the data from other devices
 another thing could be implementing this online from the computer or using the cellphone service PSTN, but before doing that we have to work more on cyber security since it wouldn't be very nice if a hacker was able to grab data from the censors using our application
 
// this is it for now shout out to Philipp Lackner
https://youtube.com/@philipplackner?si=1AbDJgCsgpg5LSfc
he helped a lot when it came to Bluetooth implementation (for legal reasons i will not say more without my lawyer present)

I setteled on this structure for the project:
```
/bluetoothapp
│
├── core/
│   ├── BaseViewModel.kt
│   ├── BluetoothRole.kt         ← Enum for MASTER / SLAVE
│   ├── Command.kt               ← Data class for commands
│   └── Result.kt                ← Sealed class for success/failure results
│
├── data/
│   ├── bluetooth/
│   │   ├── BluetoothManager.kt  ← Entry point to Android Bluetooth API
│   │   ├── SlaveHandler.kt      ← Handles incoming commands & responds
│   │   ├── MasterHandler.kt     ← Sends commands & processes replies
│   │   └── BluetoothDeviceModel.kt
│   ├── logger/
│   │   ├── SensorLogger.kt
│   │   └── FileLogger.kt
│
├── domain/
│   ├── model/
│   │   └── SensorData.kt
│   ├── usecase/
│   │   ├── StartAsMaster.kt
│   │   ├── StartAsSlave.kt
│   │   ├── SendCommand.kt
│   │   ├── ReceiveCommand.kt
│   │   └── LogSensorData.kt
│
├── presentation/
│   ├── viewmodel/
│   │   └── BluetoothViewModel.kt
│   ├── ui/
│   │   ├── StartupActivity.kt   ← Mode selection (Master/Slave)
│   │   ├── MasterFragment.kt    ← View for master controls
│   │   └── SlaveFragment.kt     ← View for responding/logging
│
└── App.kt
```
