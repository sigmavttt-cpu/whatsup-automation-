package com.whatsup.automation.data.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LicenseTokenCryptoTest {

    private lateinit var crypto: LicenseTokenCrypto

    @Before
    fun setUp() {
        crypto = LicenseTokenCrypto()
    }

    @Test
    fun testGenerateAndValidateValidToken() {
        val deviceId = "4B7E6F8A1C2D3E4F5A6B7C8D9E0F1A2B3C4D5E6F7A8B9C0D1E2F3A4B5C6D7E8F"
        val token = crypto.generateToken(deviceId, durationDays = 30, planType = "VIP")

        assertTrue("Token should start with SIGMA-", token.startsWith("SIGMA-"))
        assertTrue("Token should be a large hash (>= 120 chars)", token.length >= 120)

        val result = crypto.validateToken(token, currentDeviceFingerprint = deviceId)
        assertTrue("Expected valid result, got: $result", result is LicenseValidationResult.Valid)

        val validPayload = (result as LicenseValidationResult.Valid).payload
        assertEquals(deviceId, validPayload.deviceId)
        assertEquals("VIP", validPayload.planType)
        assertTrue("Expires at should be in future", validPayload.expiresAtMillis > System.currentTimeMillis())
    }

    @Test
    fun testDeviceMismatchRejected() {
        val originalDevice = "AAAABBBBCCCCDDDDEEEEFFFF0000111122223333444455556666777788889999"
        val differentDevice = "9999888877776666555544443333222211110000FFFFEEEEDDDDCCCCBBBBAAAA"

        val token = crypto.generateToken(originalDevice, durationDays = 15, planType = "STANDARD")
        val result = crypto.validateToken(token, currentDeviceFingerprint = differentDevice)

        assertTrue("Expected device mismatch, got: $result", result is LicenseValidationResult.DeviceMismatch)
    }

    @Test
    fun testExpiredTokenDetected() {
        val deviceId = "11223344556677889900AABBCCDDEEFF11223344556677889900AABBCCDDEEFF"
        // إصدار ترخيص بتاريخ سابق (منتهٍ قبل 5 أيام)
        val pastIssuedAt = System.currentTimeMillis() - (10L * 24L * 60L * 60L * 1000L)
        val token = crypto.generateToken(deviceId, durationDays = 5, planType = "VIP", issuedAtMillis = pastIssuedAt)

        val result = crypto.validateToken(token, currentDeviceFingerprint = deviceId)
        assertTrue("Expected expired result, got: $result", result is LicenseValidationResult.Expired)
    }

    @Test
    fun testTamperedSignatureRejected() {
        val deviceId = "11223344556677889900AABBCCDDEEFF11223344556677889900AABBCCDDEEFF"
        val token = crypto.generateToken(deviceId, durationDays = 30)

        // تلاعب بتوقيع الهاش بتغيير آخر حرف
        val tamperedToken = token.dropLast(1) + (if (token.last() == 'A') 'B' else 'A')
        val result = crypto.validateToken(tamperedToken, currentDeviceFingerprint = deviceId)

        assertTrue("Expected invalid signature, got: $result", result is LicenseValidationResult.InvalidSignature)
    }

    @Test
    fun testUniversalTokenAcceptedOnAnyDevice() {
        val token = crypto.generateToken(deviceId = "ALL", durationDays = 365, planType = "LIFETIME")
        val anyDevice = "SOME_RANDOM_DEVICE_FINGERPRINT_123456789"

        val result = crypto.validateToken(token, currentDeviceFingerprint = anyDevice)
        assertTrue("Universal token should be valid for any device", result is LicenseValidationResult.Valid)
    }

    @Test
    fun testPermanentUniversalToken() {
        val testToken = "SIGMA-QUxMfDB8VklQfDE3MDAwMDAwMDAwMDA.727C748D80810F14AC07C96A48CB10AF61A18B51DA272FFCED65627056B4E58B"
        val randomDevice1 = "DEVICE_A_11112222333344445555666677778888"
        val randomDevice2 = "DEVICE_B_99998888777766665555444433332222"

        val res1 = crypto.validateToken(testToken, randomDevice1)
        val res2 = crypto.validateToken(testToken, randomDevice2)

        assertTrue(res1 is LicenseValidationResult.Valid)
        assertTrue(res2 is LicenseValidationResult.Valid)

        val payload = (res1 as LicenseValidationResult.Valid).payload
        assertEquals("ALL", payload.deviceId)
        assertEquals(0L, payload.expiresAtMillis) // 0L means Permanent / Non-expiring
        assertEquals("VIP", payload.planType)
    }
}
