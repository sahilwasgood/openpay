package openpay.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import openpay.app.rails.NetKind

object Net {

    /**
     * ONLINE: an active network that actually reaches the internet.
     * SIGNAL: connectivity exists but nothing validates (data off, dead zone).
     * NONE: no usable network at all.
     */
    fun netKind(context: Context): NetKind {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return NetKind.NONE
        val network = cm.activeNetwork ?: return NetKind.NONE
        val caps = cm.getNetworkCapabilities(network) ?: return NetKind.NONE
        return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            NetKind.ONLINE
        } else {
            NetKind.SIGNAL
        }
    }

    fun carrier(context: Context): String? {
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return null
        return runCatching {
            tm.simOperatorName?.ifBlank { null } ?: tm.networkOperatorName?.ifBlank { null }
        }.getOrNull()
    }
}
