package com.wordonline.matching.qa

import com.wordonline.matching.auth.service.UserId
import com.wordonline.matching.auth.service.UserService
import com.wordonline.matching.deck.repository.UserCardRepository
import com.wordonline.matching.magic.repository.MagicRepository
import com.wordonline.matching.matching.dto.MatchedInfoDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@RestController
@Profile("qa")
@ConditionalOnProperty(name = ["artel.qa.enabled"], havingValue = "true")
@RequestMapping("/api/artel-qa")
class QaCheatController(
    private val properties: QaCheatProperties,
    private val builder: WebClient.Builder,
    private val users: UserService,
    private val magics: MagicRepository,
    private val cards: UserCardRepository,
) {
    @PostMapping("/matches")
    suspend fun match(@UserId principal: Long?, @RequestBody request: MatchRequest): MatchedInfoDto = coroutineScope {
        val userId = requireQaAccount(principal)
        val peer = request.peerId
        if (peer != null) requireQaAccount(peer)
        if (peer == userId) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        val gameUrl = properties.gameUrl
            ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)
        val ready = try {
            builder.clone().baseUrl(gameUrl.toString()).build()
                .post().uri("/api/server/artel-qa/matches")
                .bodyValue(CreateMatch(userId, peer))
                .retrieve().bodyToMono(Ready::class.java)
                .timeout(Duration.ofSeconds(5))
                .awaitSingle()
        } catch (e: WebClientResponseException) {
            val status = if (e.statusCode == HttpStatus.CONFLICT) HttpStatus.CONFLICT else HttpStatus.SERVICE_UNAVAILABLE
            throw ResponseStatusException(status, "QA match unavailable", e)
        }
        if (!ready.sessionId.startsWith("artel-qa-") ||
            (ready.leftId != userId && ready.rightId != userId) ||
            (peer != null && ready.leftId != peer && ready.rightId != peer)
        ) throw ResponseStatusException(HttpStatus.BAD_GATEWAY)

        val left = async { users.getUserDetail(ready.leftId).awaitSingle() }
        val right = async { users.getUserDetail(ready.rightId).awaitSingle() }
        MatchedInfoDto("ARTEL QA", ready.server, left.await(), right.await(), ready.sessionId,
            ready.webSocketUrl, ready.mapType)
    }

    @PostMapping("/magics")
    suspend fun grant(@UserId principal: Long?, @RequestBody request: GrantRequest): GrantReply {
        val userId = requireQaAccount(principal)
        if (request.magicId <= 0 || request.count !in 1..100 || !magics.existsById(request.magicId).awaitSingle()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
        if (cards.addCount(userId, request.magicId, request.count).awaitSingle() != 1L) {
            throw ResponseStatusException(HttpStatus.CONFLICT)
        }
        return GrantReply(request.magicId, request.count)
    }

    private fun requireQaAccount(id: Long?): Long {
        if (id == null || !properties.allows(id)) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        return id
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun reject(exception: ResponseStatusException): ResponseEntity<Void> =
        ResponseEntity.status(exception.statusCode).build()

    data class MatchRequest(val peerId: Long? = null)
    data class CreateMatch(val userId: Long, val peerId: Long?)
    data class Ready(val sessionId: String, val server: String, val webSocketUrl: String, val mapType: String,
                     val leftId: Long, val rightId: Long)
    data class GrantRequest(val magicId: Long, val count: Int)
    data class GrantReply(val magicId: Long, val count: Int)
}
