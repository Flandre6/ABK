package com.abk.kernel.utils

import com.abk.kernel.data.model.ArtifactType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Locale
import java.util.zip.ZipFile

data class SignedBundleManifest(
    @SerializedName("schema") val schema: Int = 1,
    @SerializedName("bundle_name") val bundleName: String,
    @SerializedName("artifact_type") val artifactType: String,
    @SerializedName("run_id") val runId: Long,
    @SerializedName("payload_name") val payloadName: String,
    @SerializedName("payload_sha256") val payloadSha256: String,
    @SerializedName("payload_size_bytes") val payloadSizeBytes: Long,
    @SerializedName("created_at") val createdAt: String? = null
)

data class BundleVerificationResult(
    val manifest: SignedBundleManifest,
    val success: Boolean,
    val message: String
)

object ArtifactVerification {
    const val MANIFEST_FILE_NAME: String = "ABK_BUNDLE_MANIFEST.json"
    const val SIGNATURE_FILE_NAME: String = "ABK_BUNDLE_MANIFEST.sig"

    private const val CERT_BASE64 =
        "MIIDOzCCAiOgAwIBAgIUD0EsZlnI9dCyVHCYH5WdIuNOvGUwDQYJKoZIhvcNAQELBQAwLTEdMBsGA1UEAwwUQUJLIEFydGlmYWN0IFNpZ25pbmcxDDAKBgNVBAoMA0FCSzAeFw0yNjA2MTYxNjUxNDlaFw0zNjA2MTMxNjUxNDlaMC0xHTAbBgNVBAMMFEFCSyBBcnRpZmFjdCBTaWduaW5nMQwwCgYDVQQKDANBQkswggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCtsdq497whDaXt7HydCShDveW/oTjw3hRp/fH/myOJtS01W7gATeRhWEj4NP4dTtulhh69pEDzu7ONoNYeSx8oMLaEM7v/S7lkdD0k/TH8cdJ9RG9m7IKyTCvOLgoyCwsaLX0ij8OcL+9+6kxpiCyaBHFwXzvBTkBfong5D2KqnjZ8XAvbSF7RbMLj2BAW4I5m9Gm50VPbPR6e7eItc3L+RVpRShNwSNbQjPEMl6XAAAfU7IM6QIm09DwaLpjYgZP+e8e4TRaCy013sVoqlQ83+bvchMPjqzmLkzH24A2DdCr15A/b41L5w6iTaHd0v5VNnm5/EiMG1hNPAMA08awtAgMBAAGjUzBRMB0GA1UdDgQWBBQmN6pBtZvKIacoR+2PcfYHZCs3JDAfBgNVHSMEGDAWgBQmN6pBtZvKIacoR+2PcfYHZCs3JDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBqtlyYEXY09cteKoIO5mVSSZCO4izDhj2BZJaWSDyjtvpWqUNxAjwiSZMNZe75BnQJDF1vFyR2eZ1pYkmeV9v6GC/NZ07QbuXeGIM/YDr1/sDPfZ48Hr2syRSNKKuvamx4OeAk7tJ1+OKT/zOJXWwME0EKRqs/5ev4RlgOtfDGm+i3z1iDgPpzGjnlJbEAWcHX0+j0xUzBJ5JEQEcUi3raKJ33VNfe1kdspXv70qXN6z2GoX0E61MFeaYWEJ/rTM9RoYAnqIF0WoTo/TVJLxUyQPg6+fD5QGuBrxrH1Ulp88NdOgYh2FUbu7IznsbYN0ucOfEb04Gp3rS995n5+dZ1"

    private val gson = Gson()

    fun requiresTrustedBundle(type: ArtifactType): Boolean = when (type) {
        ArtifactType.KERNEL_IMG,
        ArtifactType.ANYKERNEL3 -> true
        else -> false
    }

    fun verifyBundleFile(bundleFile: File, expectedType: ArtifactType? = null): BundleVerificationResult {
        if (!bundleFile.isFile || !bundleFile.name.lowercase(Locale.ROOT).endsWith(".bundle.zip")) {
            return failureFor(bundleFile.name, expectedType, "Trusted artifact must be a signed .bundle.zip")
        }
        return runCatching {
            ZipFile(bundleFile).use { zip ->
                val manifestEntry = zip.getEntry(MANIFEST_FILE_NAME)
                    ?: return failureFor(bundleFile.name, expectedType, "Missing $MANIFEST_FILE_NAME")
                val signatureEntry = zip.getEntry(SIGNATURE_FILE_NAME)
                    ?: return failureFor(bundleFile.name, expectedType, "Missing $SIGNATURE_FILE_NAME")
                val manifestBytes = zip.getInputStream(manifestEntry).use { it.readBytes() }
                val signatureBytes = zip.getInputStream(signatureEntry).use { it.readBytes() }
                val manifest = gson.fromJson(String(manifestBytes, Charsets.UTF_8), SignedBundleManifest::class.java)
                if (!verifyManifestSignature(manifestBytes, signatureBytes)) {
                    return BundleVerificationResult(manifest, false, "Artifact signature verification failed")
                }
                val manifestType = runCatching { ArtifactType.valueOf(manifest.artifactType) }.getOrNull()
                if (expectedType != null && manifestType != expectedType) {
                    return BundleVerificationResult(manifest, false, "Artifact type mismatch")
                }
                if (manifest.bundleName != bundleFile.name) {
                    return BundleVerificationResult(manifest, false, "Bundle filename mismatch")
                }
                val payloadEntry = zip.getEntry(manifest.payloadName)
                    ?: return BundleVerificationResult(manifest, false, "Missing payload entry ${manifest.payloadName}")
                val payloadBytes = zip.getInputStream(payloadEntry).use { it.readBytes() }
                if (payloadBytes.size.toLong() != manifest.payloadSizeBytes) {
                    return BundleVerificationResult(manifest, false, "Payload size mismatch")
                }
                if (sha256(payloadBytes) != normalizeDigest(manifest.payloadSha256)) {
                    return BundleVerificationResult(manifest, false, "Payload digest mismatch")
                }
                BundleVerificationResult(manifest, true, "Verified ${bundleFile.name}")
            }
        }.getOrElse { error ->
            failureFor(bundleFile.name, expectedType, error.message ?: error::class.java.simpleName)
        }
    }

    fun normalizeDigest(value: String): String = value.trim().lowercase(Locale.ROOT)

    private fun verifyManifestSignature(manifestBytes: ByteArray, signatureBytes: ByteArray): Boolean {
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(artifactCertificate().publicKey)
        verifier.update(manifestBytes)
        return verifier.verify(signatureBytes)
    }

    private fun artifactCertificate(): X509Certificate {
        val der = Base64.getDecoder().decode(CERT_BASE64)
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(der.inputStream()) as X509Certificate
    }

    private fun failureFor(
        bundleName: String,
        expectedType: ArtifactType?,
        message: String
    ): BundleVerificationResult = BundleVerificationResult(
        manifest = SignedBundleManifest(
            bundleName = bundleName,
            artifactType = expectedType?.name ?: ArtifactType.OTHER.name,
            runId = -1L,
            payloadName = "",
            payloadSha256 = "",
            payloadSizeBytes = 0L
        ),
        success = false,
        message = message
    )
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

