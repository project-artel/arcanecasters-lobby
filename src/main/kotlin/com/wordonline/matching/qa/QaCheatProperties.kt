package com.wordonline.matching.qa

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties("artel.qa")
data class QaCheatProperties(
    val accounts: Set<Long> = emptySet(),
    val gameUrl: URI? = null,
) {
    fun allows(id: Long): Boolean = id > 0 && id in accounts
}
