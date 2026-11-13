package com.wordonline.matching.qa

import com.wordonline.matching.auth.service.UserService
import com.wordonline.matching.deck.repository.UserCardRepository
import com.wordonline.matching.magic.repository.MagicRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

class QaCheatControllerTest {
    private val magics = mock(MagicRepository::class.java)
    private val cards = mock(UserCardRepository::class.java)
    private val controller = QaCheatController(
        QaCheatProperties(accounts = setOf(10L)), WebClient.builder(), mock(UserService::class.java), magics, cards,
    )

    @Test
    fun rejectsGrantForAccountOutsideQaAllowlist() = runTest {
        val result = runCatching { controller.grant(11L, QaCheatController.GrantRequest(3L, 2)) }
        assertEquals(HttpStatus.FORBIDDEN, (result.exceptionOrNull() as ResponseStatusException).statusCode)
        verifyNoInteractions(magics, cards)
    }

    @Test
    fun grantsCopiesToAuthenticatedQaAccount() = runTest {
        `when`(magics.existsById(3L)).thenReturn(Mono.just(true))
        `when`(cards.addCount(10L, 3L, 2)).thenReturn(Mono.just(1L))
        assertEquals(QaCheatController.GrantReply(3L, 2), controller.grant(10L, QaCheatController.GrantRequest(3L, 2)))
        verify(cards).addCount(10L, 3L, 2)
    }
}
