// Copy this file to secrets.h (same folder) and fill in your values.
// secrets.h is git-ignored; this example file is the only one committed.
#pragma once

#define WIFI_SSID     "your-wifi-name"
#define WIFI_PASSWORD "your-wifi-password"

// The PC running the API, on the same Wi-Fi network. Find its IPv4 address with `ipconfig`.
// Windows Firewall must allow inbound TCP 8080 for Java.
#define API_BASE      "http://192.168.1.50:8080"
