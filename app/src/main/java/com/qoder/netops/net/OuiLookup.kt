package com.qoder.netops.net

/**
 * Small, curated OUI table for the vendors most often seen on a home/office LAN.
 * Unknown prefixes simply return null; MACs are never uploaded anywhere.
 */
object OuiLookup {

    private val vendors = mapOf(
        "AC:BC:32" to "Apple",
        "A4:83:E7" to "Apple",
        "F0:18:98" to "Apple",
        "70:3E:AC" to "Apple",
        "88:66:A5" to "Apple",
        "A8:5C:2C" to "Apple",
        "00:1E:C2" to "Apple",
        "3C:5A:B4" to "Google",
        "F4:F5:E8" to "Google",
        "54:60:09" to "Google",
        "B8:27:EB" to "Raspberry Pi",
        "DC:A6:32" to "Raspberry Pi",
        "E4:5F:01" to "Raspberry Pi",
        "00:0C:29" to "VMware",
        "00:50:56" to "VMware",
        "08:00:27" to "VirtualBox",
        "00:1C:42" to "Parallels",
        "18:FE:34" to "Espressif",
        "24:0A:C4" to "Espressif",
        "84:F3:EB" to "Espressif",
        "3C:71:BF" to "Espressif",
        "A4:CF:12" to "Espressif",
        "50:C7:BF" to "TP-Link",
        "C0:25:E9" to "TP-Link",
        "F0:9F:C2" to "Ubiquiti",
        "78:8A:20" to "Ubiquiti",
        "24:5A:4C" to "Ubiquiti",
        "00:17:88" to "Philips Hue",
        "64:B4:73" to "Xiaomi",
        "F8:A4:5F" to "Xiaomi",
        "8C:53:C3" to "Xiaomi",
        "34:CE:00" to "Xiaomi",
        "9C:99:A0" to "Xiaomi",
        "00:11:32" to "Synology",
        "90:09:D0" to "Synology",
        "00:1B:21" to "Intel",
        "3C:97:0E" to "Wistron",
        "54:EE:75" to "Wistron",
        "00:24:81" to "HP",
        "3C:D9:2B" to "HP",
        "00:1B:A9" to "Brother",
        "8C:77:12" to "Samsung",
        "E8:50:8B" to "Samsung",
        "BC:20:A4" to "Samsung",
        "C8:19:F7" to "Samsung",
        "F4:9F:54" to "Samsung",
        "48:46:FB" to "Huawei",
        "78:F5:FD" to "Huawei",
        "00:E0:FC" to "Huawei",
        "D4:6E:5C" to "Huawei",
        "2C:56:DC" to "ASUS",
        "1C:87:2C" to "ASUS",
        "38:2C:4A" to "ASUS",
        "F8:32:E4" to "ASUS",
        "04:D4:C4" to "ASUS",
        "FC:AA:14" to "Gigabyte",
        "1C:1B:0D" to "Gigabyte",
        "2C:F0:5D" to "MSI",
        "44:65:0D" to "Amazon",
        "74:C2:46" to "Amazon",
        "F0:27:2D" to "Amazon",
        "38:F7:3D" to "Amazon",
        "FC:65:DE" to "Amazon",
        "68:54:FD" to "Amazon",
        "00:1A:A1" to "Cisco",
        "00:B0:0C" to "Cisco",
        "00:1D:7E" to "Cisco-Linksys"
    )

    fun vendorFor(mac: String?): String? {
        if (mac.isNullOrBlank() || mac.length < 8) return null
        val prefix = mac.substring(0, 8).uppercase()
        return vendors[prefix]
    }
}