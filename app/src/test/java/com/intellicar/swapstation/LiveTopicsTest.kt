package com.intellicar.swapstation

import com.intellicar.swapstation.core.config.LiveTopics
import com.intellicar.swapstation.core.config.OutgoingTopic
import org.junit.Assert.assertEquals
import org.junit.Test

/** Guards the fixed live topic contract: structure and names must match exactly. */
class LiveTopicsTest {
    private val topics = LiveTopics(
        org = "ITPL",
        stationType = "testingStation",
        version = "500.0.0",
        stationId = "testingStation_01",
    )

    @Test fun `commands topic - server to station`() =
        assertEquals("ITPL/testingStation/500.0.0/testingStation_01/RPC/REQUEST", topics.rpcRequest)

    @Test fun `heartbeat topic - station to server`() =
        assertEquals("ITPL/testingStation/500.0.0/testingStation_01/PERIODIC", topics.periodic)

    @Test fun `events topic - station to server`() =
        assertEquals("ITPL/testingStation/500.0.0/testingStation_01/EVENTS", topics.events)

    @Test fun `station only publishes to PERIODIC and EVENTS`() {
        assertEquals(listOf(topics.periodic, topics.events), OutgoingTopic.entries.map { it.resolve(topics) })
    }
}
