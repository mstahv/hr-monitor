# Vaadin Web Bluetooth Heart Rate Monitor

This is a technology demo & example that reads data from a heart rate monitor belts using Web Bluetooth into a Java app for analysis.

To use this application, you'll need a Chrome browser on a non-iOS device and a heart rate monitor providing its services
in somewhat standard manner (Polar and Suunto hardware tested, others might work or not). R-R intervals are read if available.

For best experience, use Polar H10, which can also provide raw electric potential differences from your chest, that we can plot as an actual ECG curve!

See [the source code](https://github.com/mstahv/hr-monitor) for the most interesting parts.