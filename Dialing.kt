package com.mohamed.phone.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

object Dialing {
    private fun tm(ctx: Context): TelecomManager = ctx.getSystemService(TelecomManager::class.java)

    fun accounts(ctx: Context): List<PhoneAccountHandle> = try {
        tm(ctx).callCapablePhoneAccounts ?: emptyList()
    } catch (e: Exception) { emptyList() }

    fun defaultAccount(ctx: Context): PhoneAccountHandle? = try {
        tm(ctx).getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL)
    } catch (e: Exception) { null }

    fun label(ctx: Context, h: PhoneAccountHandle): String = try {
        tm(ctx).getPhoneAccount(h)?.label?.toString() ?: "SIM"
    } catch (e: Exception) { "SIM" }

    fun place(ctx: Context, number: String, handle: PhoneAccountHandle?) {
        val extras = Bundle()
        if (handle != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        try {
            tm(ctx).placeCall(Uri.fromParts("tel", number, null), extras)
        } catch (e: SecurityException) { }
    }
}

object Roles {
    fun isDefaultDialer(ctx: Context): Boolean = try {
        ctx.getSystemService(TelecomManager::class.java).defaultDialerPackage == ctx.packageName
    } catch (e: Exception) { false }

    fun isDefaultSms(ctx: Context): Boolean = try {
        Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
    } catch (e: Exception) { false }

    fun dialerIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, ctx.packageName)
        }

    fun smsIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ctx.packageName)
        }
}
