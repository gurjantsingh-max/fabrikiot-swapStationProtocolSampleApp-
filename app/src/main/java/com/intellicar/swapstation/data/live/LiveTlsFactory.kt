package com.intellicar.swapstation.data.live

import android.content.Context
import android.util.Base64
import com.intellicar.swapstation.core.config.LiveServerConfig
import java.io.InputStream
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.KeyManagerFactorySpi
import javax.net.ssl.ManagerFactoryParameters
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.TrustManagerFactorySpi
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509KeyManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

/** Everything HiveMQ needs for mutual TLS against the live broker. */
class LiveTlsMaterial(
    /** Presents the device's X.509 client certificate (mutual TLS). */
    val keyManagerFactory: KeyManagerFactory,
    val trustManagerFactory: TrustManagerFactory,
    val hostnameVerifier: HostnameVerifier,
)

/** Where TLS diagnostics go. [error] = something that will make the connection fail. */
interface LiveTlsLogger {
    fun info(message: String)
    fun error(message: String, t: Throwable? = null)
}

/**
 * Builds the TLS material from the PEM files in assets:
 *  - trust store  = ONLY the HW1 CA (peer verification: the broker cert must chain to it)
 *  - key store    = client certificate (client.crt) + its PEM private key (client.key)
 *  - host check   = the broker certificate must be issued for the host we dialled
 *
 * Every stage of the handshake is reported to [LiveTlsLogger], so a failed connection shows
 * WHICH step broke (cert loading, server cert verification, client cert request, host name).
 */
object LiveTlsFactory {

    fun create(context: Context, config: LiveServerConfig, log: LiveTlsLogger): LiveTlsMaterial {
        val assets = context.assets
        val certFactory = CertificateFactory.getInstance("X.509")

        log.info("TLS: loading CA cert from assets/${config.caCertAsset}")
        val caCert = assets.open(config.caCertAsset).use { certFactory.generateCertificate(it) as X509Certificate }
        log.info("TLS: CA cert  ${describe(caCert)}")
        checkValidity("CA cert", caCert, log)

        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("hw1-ca", caCert)
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(trustStore) }

        val kmf = run {
            log.info("TLS: loading client cert from assets/${config.clientCertAsset}")
            val clientChain = assets.open(config.clientCertAsset).use {
                certFactory.generateCertificates(it).map { c -> c as X509Certificate }
            }
            check(clientChain.isNotEmpty()) { "assets/${config.clientCertAsset} contains no certificate" }
            log.info("TLS: loading client key from assets/${config.clientKeyAsset}")
            val clientKey = assets.open(config.clientKeyAsset).use { readPemPrivateKey(it) }
            val clientCert = clientChain.first()
            log.info("TLS: client cert  ${describe(clientCert)}")
            checkValidity("Client cert", clientCert, log)
            runCatching { clientCert.verify(caCert.publicKey) }
                .onSuccess { log.info("TLS: client cert is signed by the CA ✓") }
                .onFailure { log.error("TLS: client cert is NOT signed by this CA (${it.message}) — broker will likely reject it") }

            val keyBits = (clientKey as? RSAKey)?.modulus?.bitLength()
            log.info("TLS: client key loaded (${clientKey.algorithm}${keyBits?.let { " $it-bit" } ?: ""})")
            if (clientKey is RSAKey && clientCert.publicKey is RSAKey &&
                clientKey.modulus != (clientCert.publicKey as RSAKey).modulus
            ) {
                log.error("TLS: client key does NOT match the client cert — handshake will fail")
            } else if (clientKey is RSAKey) {
                log.info("TLS: client key matches the client cert ✓")
            }

            // In-memory only; the password just satisfies the KeyStore API.
            val keyPassword = CharArray(0)
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                // Send the client chain, plus the CA if client.crt did not include it.
                val chain = if (clientChain.any { it == caCert }) clientChain else clientChain + caCert
                setKeyEntry("hw1-client", clientKey, keyPassword, chain.toTypedArray())
            }
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keyStore, keyPassword) }
        }
        log.info("TLS: trust store + key store ready (protocol ${config.tlsProtocol})")

        return LiveTlsMaterial(
            keyManagerFactory = LoggingKeyManagerFactory(kmf, log),
            trustManagerFactory = LoggingTrustManagerFactory(tmf, log),
            hostnameVerifier = CertHostnameVerifier(log),
        )
    }

    private fun checkValidity(label: String, cert: X509Certificate, log: LiveTlsLogger) {
        val now = Date()
        when {
            now.before(cert.notBefore) -> log.error(
                "TLS: $label is NOT YET VALID (valid from ${cert.notBefore}, device time is $now). " +
                    "Check the device clock/time zone."
            )
            now.after(cert.notAfter) -> log.error("TLS: $label has EXPIRED (on ${cert.notAfter})")
            else -> log.info("TLS: $label valid (${cert.notBefore} → ${cert.notAfter}) ✓")
        }
    }

    internal fun describe(cert: X509Certificate): String =
        "subject=[${cert.subjectX500Principal.name}] issuer=[${cert.issuerX500Principal.name}] " +
            "serial=${cert.serialNumber.toString(16)} sha256=${fingerprint(cert).take(23)}…"

    private fun fingerprint(cert: X509Certificate): String =
        MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString(":") { "%02X".format(it) }

    /** Reads an unencrypted PKCS#8 PEM private key ("-----BEGIN PRIVATE KEY-----"), RSA or EC. */
    private fun readPemPrivateKey(input: InputStream): PrivateKey {
        val pem = input.bufferedReader().readText()
        when {
            "BEGIN RSA PRIVATE KEY" in pem || "BEGIN EC PRIVATE KEY" in pem -> throw IllegalStateException(
                "client.key is in traditional (PKCS#1/SEC1) format; convert it with: " +
                    "openssl pkcs8 -topk8 -nocrypt -in client.key -out client.key"
            )
            "BEGIN ENCRYPTED PRIVATE KEY" in pem -> throw IllegalStateException(
                "client.key is password-protected; export it unencrypted with: " +
                    "openssl pkcs8 -topk8 -nocrypt -in client.key -out client.key"
            )
        }
        val base64 = Regex("-----BEGIN PRIVATE KEY-----(.+?)-----END PRIVATE KEY-----", RegexOption.DOT_MATCHES_ALL)
            .find(pem)?.groupValues?.get(1)
            ?: throw IllegalStateException("client.key has no '-----BEGIN PRIVATE KEY-----' block")
        val spec = PKCS8EncodedKeySpec(Base64.decode(base64, Base64.DEFAULT))
        for (algorithm in listOf("RSA", "EC")) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec)
            } catch (_: InvalidKeySpecException) {
                // try the next algorithm
            }
        }
        throw IllegalStateException("client.key is neither an RSA nor an EC PKCS#8 key")
    }

    // ---------------------------------------------------------------- trust manager (server cert)

    private class LoggingTrustManagerFactory(delegate: TrustManagerFactory, log: LiveTlsLogger) :
        TrustManagerFactory(Spi(delegate, log), delegate.provider, delegate.algorithm) {

        private class Spi(private val delegate: TrustManagerFactory, private val log: LiveTlsLogger) :
            TrustManagerFactorySpi() {
            override fun engineInit(ks: KeyStore?) = delegate.init(ks)
            override fun engineInit(spec: ManagerFactoryParameters?) = delegate.init(spec)
            override fun engineGetTrustManagers(): Array<TrustManager> =
                delegate.trustManagers.map { tm ->
                    if (tm is X509TrustManager) LoggingTrustManager(tm, log) else tm
                }.toTypedArray()
        }
    }

    /** Logs the certificate chain the broker presents and whether it chains to our CA. */
    private class LoggingTrustManager(
        private val delegate: X509TrustManager,
        private val log: LiveTlsLogger,
    ) : X509ExtendedTrustManager() {

        private fun checkServer(chain: Array<out X509Certificate>, authType: String, check: () -> Unit) {
            log.info("TLS: server sent ${chain.size} cert(s), authType=$authType")
            chain.forEachIndexed { i, c -> log.info("TLS:   server[$i] ${describe(c)}  valid ${c.notBefore} → ${c.notAfter}") }
            try {
                check()
                log.info("TLS: server certificate trusted (chains to HW1 CA) ✓")
            } catch (e: CertificateException) {
                log.error("TLS: server certificate REJECTED: ${e.message}", e)
                throw e
            }
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) =
            checkServer(chain, authType) { delegate.checkServerTrusted(chain, authType) }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket?) =
            checkServer(chain, authType) {
                (delegate as? X509ExtendedTrustManager)?.checkServerTrusted(chain, authType, socket)
                    ?: delegate.checkServerTrusted(chain, authType)
            }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine?) =
            checkServer(chain, authType) {
                (delegate as? X509ExtendedTrustManager)?.checkServerTrusted(chain, authType, engine)
                    ?: delegate.checkServerTrusted(chain, authType)
            }

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
            delegate.checkClientTrusted(chain, authType)

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket?) =
            delegate.checkClientTrusted(chain, authType)

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine?) =
            delegate.checkClientTrusted(chain, authType)

        override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
    }

    // ---------------------------------------------------------------- key manager (client cert)

    private class LoggingKeyManagerFactory(delegate: KeyManagerFactory, log: LiveTlsLogger) :
        KeyManagerFactory(Spi(delegate, log), delegate.provider, delegate.algorithm) {

        private class Spi(private val delegate: KeyManagerFactory, private val log: LiveTlsLogger) :
            KeyManagerFactorySpi() {
            override fun engineInit(ks: KeyStore?, password: CharArray?) = delegate.init(ks, password)
            override fun engineInit(spec: ManagerFactoryParameters?) = delegate.init(spec)
            override fun engineGetKeyManagers(): Array<KeyManager> =
                delegate.keyManagers.map { km ->
                    if (km is X509KeyManager) LoggingKeyManager(km, log) else km
                }.toTypedArray()
        }
    }

    /** Logs when the broker asks for our client certificate and which one we send. */
    private class LoggingKeyManager(
        private val delegate: X509KeyManager,
        private val log: LiveTlsLogger,
    ) : X509ExtendedKeyManager() {

        private fun chooseClient(keyTypes: Array<out String>?, issuers: Array<out Principal>?, choose: () -> String?): String? {
            log.info(
                "TLS: broker REQUESTED a client certificate (key types=${keyTypes?.joinToString()}, " +
                    "accepted issuers=${issuers?.joinToString(" | ") { it.name } ?: "any"})"
            )
            val alias = choose()
            if (alias == null) {
                log.error("TLS: no matching client certificate for this request — broker will reject the handshake")
            } else {
                val cert = delegate.getCertificateChain(alias)?.firstOrNull()
                log.info("TLS: sending client cert alias=$alias ${cert?.let { describe(it) } ?: ""}")
            }
            return alias
        }

        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) =
            chooseClient(keyType, issuers) { delegate.chooseClientAlias(keyType, issuers, socket) }

        override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?) =
            chooseClient(keyType, issuers) {
                (delegate as? X509ExtendedKeyManager)?.chooseEngineClientAlias(keyType, issuers, engine)
                    ?: delegate.chooseClientAlias(keyType, issuers, null)
            }

        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = delegate.getClientAliases(keyType, issuers)
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = delegate.getServerAliases(keyType, issuers)
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) =
            delegate.chooseServerAlias(keyType, issuers, socket)
        override fun getCertificateChain(alias: String?) = delegate.getCertificateChain(alias)
        override fun getPrivateKey(alias: String?) = delegate.getPrivateKey(alias)
    }

    // ---------------------------------------------------------------- host name check

    /**
     * The HW1 certificates have no subjectAltName extension, only CN=hw1.buzzenergy.in. Android's
     * default verifier ignores CN, so it would reject a perfectly valid broker. This verifier
     * checks SAN DNS names when present and falls back to the CN otherwise. The certificate
     * chain itself has already been validated against the HW1 CA by the trust manager.
     */
    private class CertHostnameVerifier(private val log: LiveTlsLogger) : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            log.info("TLS: handshake done — protocol=${session.protocol}, cipher=${session.cipherSuite}")
            val cert = session.peerCertificates.firstOrNull() as? X509Certificate
            if (cert == null) {
                log.error("TLS: hostname check failed — broker sent no X.509 certificate")
                return false
            }
            val host = hostname.lowercase().trimEnd('.')
            val sanDns = cert.subjectAlternativeNames
                ?.filter { it.size >= 2 && it[0] == 2 } // 2 = dNSName
                ?.mapNotNull { it[1] as? String }
                .orEmpty()
            val names = sanDns.ifEmpty { listOfNotNull(commonName(cert.subjectX500Principal)) }
            val ok = names.any { matches(host, it.lowercase().trimEnd('.')) }
            val source = if (sanDns.isEmpty()) "CN" else "SAN"
            if (ok) log.info("TLS: hostname '$host' matches cert $source $names ✓")
            else log.error("TLS: hostname '$host' does NOT match cert $source $names — connection refused")
            return ok
        }

        private fun commonName(principal: X500Principal): String? =
            principal.getName(X500Principal.RFC2253)
                .split(',')
                .map { it.trim() }
                .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
                ?.substring(3)

        private fun matches(host: String, pattern: String): Boolean {
            if (!pattern.startsWith("*.")) return host == pattern
            val suffix = pattern.substring(1) // ".example.com"
            return host.endsWith(suffix) && !host.removeSuffix(suffix).contains('.')
        }
    }
}
