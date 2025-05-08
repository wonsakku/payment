package com.example.paymentservice.payment.adapter.out.persistent.repository

import com.example.paymentservice.payment.domain.PaymentEvent
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.math.BigInteger

@Repository
class R2DBCPaymentRepository(
    private val databaseClient: DatabaseClient,
    private val transactionalOperator: TransactionalOperator
) : PaymentRepository {


    override fun save(paymentEvent: PaymentEvent): Mono<Void> {
        return insertPaymentEvent(paymentEvent)
            .flatMap { selectPaymentEventId() }
            .flatMap { paymentEventId -> insertPaymentOrders(paymentEvent, paymentEventId) }
            .`as`(transactionalOperator::transactional)
            .then()
    }

    private fun insertPaymentEvent(paymentEvent: PaymentEvent): Mono<Long> {
        return databaseClient.sql(INSERT_PAYMENT_EVENT_QUERY)
            .bind("buyerId", paymentEvent.buyerId)
            .bind("orderName", paymentEvent.orderName)
            .bind("orderId", paymentEvent.orderId)
            .fetch()
            .rowsUpdated()
    }

    private fun selectPaymentEventId() = databaseClient.sql(LAST_INSERT_ID_QUERY)
        .fetch()
        .first()
        .map { (it["LAST_INSERT_ID()"] as BigInteger).toLong() }

    private fun insertPaymentOrders(
        paymentEvent: PaymentEvent,
        paymentEventId: Long
    ): Mono<Long> {
        val valueClauses = paymentEvent.paymentOrders.joinToString(", ") { paymentOrder ->
            "($paymentEventId, ${paymentOrder.sellerId}, '${paymentOrder.orderId}', ${paymentOrder.productId}, ${paymentOrder.amount}, '${paymentOrder.paymentStatus}')"
        }

        return databaseClient.sql(INSERT_PAYMENT_ORDER_QUERY(valueClauses))
            .fetch()
            .rowsUpdated()
    }


    companion object {
        val INSERT_PAYMENT_EVENT_QUERY = """
      INSERT INTO payment_events (buyer_id, order_name, order_id)
      VALUES (:buyerId, :orderName, :orderId) 
    """.trimIndent()

        val LAST_INSERT_ID_QUERY = """
      SELECT LAST_INSERT_ID()
    """.trimIndent()

        val INSERT_PAYMENT_ORDER_QUERY = fun (valueClauses: String) = """
      INSERT INTO payment_orders (payment_event_id, seller_id, order_id, product_id, amount, payment_order_status) 
      VALUES $valueClauses
    """.trimIndent()

        val SELECT_PENDING_PAYMENT_QUERY = """
      SELECT pe.id as payment_event_id, pe.payment_key, pe.order_id, po.id as payment_order_id, po.payment_order_status, po.amount, po.failed_count, po.threshold
      FROM payment_events pe
      INNER JOIN payment_orders po ON po.payment_event_id = pe.id
      WHERE (po.payment_order_status = 'UNKNOWN' OR (po.payment_order_status = 'EXECUTING' AND po.updated_at <= :updatedAt - INTERVAL 3 MINUTE))
      AND po.failed_count < po.threshold
      LIMIT 10 
    """.trimIndent()

        val SELECT_PAYMENT_EVENT_QUERY = """
      SELECT pe.id as payment_event_id, po.id as payment_order_id, pe.order_id, pe.order_name, pe.buyer_id, pe.is_payment_done, po.seller_id, po.product_id, po.payment_order_status, po.amount, po.ledger_updated, po.wallet_updated 
      FROM payment_events pe
      INNER JOIN payment_orders po ON pe.order_id = po.order_id
      WHERE pe.order_id = :orderId 
    """.trimIndent()

        val UPDATE_PAYMENT_ORDER_LEDGER_DONE_QUERY = """ 
      UPDATE payment_orders 
      SET ledger_updated = true  
      WHERE payment_event_id = :paymentEventId
    """.trimIndent()

        val UPDATE_PAYMENT_ORDER_WALLET_DONE_QUERY = """
      UPDATE payment_orders 
      SET wallet_updated = true  
      WHERE payment_event_id = :paymentEventId
    """.trimIndent()

        val UPDATE_PAYMENT_EVENT_COMPLETE_QUERY = """
      UPDATE payment_events 
      SET is_payment_done = true
      WHERE id = :paymentEventId
    """.trimIndent()

    }
}