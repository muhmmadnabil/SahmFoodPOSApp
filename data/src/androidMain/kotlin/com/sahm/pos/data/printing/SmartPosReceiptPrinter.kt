package com.sahm.pos.data.printing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.sahm.pos.domain.ReceiptPrinter
import com.sahm.pos.domain.entity.OrderDetails
import com.sahm.pos.domain.entity.PaymentStatus
import com.sahm.pos.domain.entity.RefundDetails
import com.sahm.pos.domain.repository.OrderRepo
import com.sahm.pos.domain.results.PrintResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import recieptservice.com.recieptservice.PrinterInterface

internal class SmartPosReceiptPrinter(
    context: Context,
    private val orderRepo: OrderRepo,
) : ReceiptPrinter {
    private val appContext = context.applicationContext
    private val printMutex = Mutex()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @Volatile
    private var printer: PrinterInterface? = null

    @Volatile
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            printer = PrinterInterface.Stub.asInterface(service)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            printer = null
            isBound = false
        }
    }

    init {
        bindPrinterService()
    }

    override suspend fun printOrderReceipt(orderId: String): PrintResult =
        withContext(Dispatchers.IO) {
            val details = orderRepo.getOrderDetails(orderId)
                ?: return@withContext PrintResult.Failed("Order not found")

            printWithRetry { service ->
                printOrder(service, details)
            }
        }

    override suspend fun printRefundReceipt(refundId: String): PrintResult =
        withContext(Dispatchers.IO) {
            val details = orderRepo.getRefundDetails(refundId)
                ?: return@withContext PrintResult.Failed("Refund not found")

            printWithRetry { service ->
                printRefund(service, details)
            }
        }

    private suspend fun printWithRetry(job: (PrinterInterface) -> Unit): PrintResult {
        var lastError = "Printer service unavailable"

        repeat(MAX_RETRIES) { attempt ->
            val service = awaitPrinter()
            if (service == null) {
                lastError = "Printer service unavailable"
                delay(RETRY_DELAY_MS * (attempt + 1))
                return@repeat
            }

            try {
                printMutex.withLock {
                    service.beginWork()
                    try {
                        job(service)
                        service.nextLine(3)
                    } finally {
                        service.endWork()
                    }
                }
                return PrintResult.Success
            } catch (exception: Exception) {
                lastError = exception.message ?: exception::class.java.simpleName
                printer = null
                isBound = false
                bindPrinterService()
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }

        return PrintResult.Failed(lastError)
    }

    private suspend fun awaitPrinter(): PrinterInterface? {
        printer?.let { return it }
        bindPrinterService()

        repeat(CONNECTION_WAIT_STEPS) {
            delay(CONNECTION_WAIT_DELAY_MS)
            printer?.let { return it }
        }

        return null
    }

    private fun bindPrinterService() {
        if (isBound) return

        val intent = Intent().apply {
            setClassName(
                PRINTER_SERVICE_PACKAGE,
                PRINTER_SERVICE_CLASS,
            )
        }

        runCatching {
            appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun printOrder(service: PrinterInterface, details: OrderDetails) {
        val order = details.order
        val paidPayment = details.payments.lastOrNull { it.status == PaymentStatus.Paid }

        service.apply {
            setDark(4)
            setAlignment(ALIGN_CENTER)
            setTextSize(TEXT_LARGE)
            setTextBold(true)
            printText("Restaurant POS\n")
            setTextSize(TEXT_MEDIUM)
            printText("ORDER RECEIPT\n")
            setTextBold(false)
            printText("#${order.id}\n")
            nextLine(1)

            setAlignment(ALIGN_LEFT)
            setTextSize(TEXT_NORMAL)
            printLine("Date", dateFormatter.format(Date(order.paidAt ?: order.createdAt)))
            printLine("Cashier", order.cashierName)
            printLine("Order type", order.orderType.name.toDisplayText())
            paidPayment?.let { payment ->
                printLine("Payment", payment.type.name.toDisplayText())
                payment.transactionId?.let { printLine("Transaction", it) }
                payment.cardLast4?.let { printLine("Card", "**** $it") }
            }
            printSeparator()

            setTextBold(true)
            printText("Items\n")
            setTextBold(false)
            details.items.forEach { item ->
                printText("${item.quantity} x ${item.name}\n")
                printText("  ${formatMoney(item.unitPrice)}   ${formatMoney(item.totalAmount)}\n")
            }
            printSeparator()

            printLine("Subtotal", formatMoney(order.subtotalAmount))
            if (order.serviceAmount > 0) printLine("Service", formatMoney(order.serviceAmount))
            if (order.discountAmount > 0) printLine(
                "Discount",
                "-${formatMoney(order.discountAmount)}"
            )
            printLine("Tax", formatMoney(order.taxAmount))
            setTextBold(true)
            printLine("TOTAL", formatMoney(order.totalAmount))
            setTextBold(false)
            nextLine(1)

            setAlignment(ALIGN_CENTER)
            setTextSize(TEXT_SMALL)
            printText("Thank you\n")
        }
    }

    private fun printRefund(service: PrinterInterface, details: RefundDetails) {
        val refund = details.refund

        service.apply {
            setDark(4)
            setAlignment(ALIGN_CENTER)
            setTextSize(TEXT_LARGE)
            setTextBold(true)
            printText("Restaurant POS\n")
            setTextSize(TEXT_MEDIUM)
            printText("REFUND RECEIPT\n")
            setTextBold(false)
            printText("#${refund.id}\n")
            nextLine(1)

            setAlignment(ALIGN_LEFT)
            setTextSize(TEXT_NORMAL)
            printLine("Order", refund.orderId)
            printLine("Date", dateFormatter.format(Date(refund.completedAt ?: refund.createdAt)))
            printLine("Refund type", refund.refundType.name.toDisplayText())
            refund.reason?.takeIf { it.isNotBlank() }?.let { printLine("Reason", it) }
            printSeparator()

            setTextBold(true)
            printText("Items\n")
            setTextBold(false)
            details.items.forEach { item ->
                printText("${item.quantity} x ${item.itemName}\n")
                printText("  ${formatMoney(item.unitPrice)}   ${formatMoney(item.totalRefundAmount)}\n")
            }
            printSeparator()

            setTextBold(true)
            printLine("REFUND TOTAL", formatMoney(refund.amount))
            setTextBold(false)
            nextLine(1)

            setAlignment(ALIGN_CENTER)
            setTextSize(TEXT_SMALL)
            printText("Refund completed\n")
        }
    }

    private fun PrinterInterface.printLine(label: String, value: String) {
        printText("${label.take(LABEL_WIDTH).padEnd(LABEL_WIDTH)} $value\n")
    }

    private fun PrinterInterface.printSeparator() {
        printText("--------------------------------\n")
    }

    private fun formatMoney(value: Long): String {
        val pounds = value / 100
        val piasters = (value % 100).toString().padStart(2, '0')
        return "EGP $pounds.$piasters"
    }

    private fun String.toDisplayText(): String =
        lowercase(Locale.getDefault())
            .replace('_', ' ')
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }

    private companion object {
        const val PRINTER_SERVICE_PACKAGE = "recieptservice.com.recieptservice"
        const val PRINTER_SERVICE_CLASS = "recieptservice.com.recieptservice.service.PrinterService"
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 1_000L
        const val CONNECTION_WAIT_STEPS = 10
        const val CONNECTION_WAIT_DELAY_MS = 200L
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1
        const val TEXT_SMALL = 0.35f
        const val TEXT_NORMAL = 0.55f
        const val TEXT_MEDIUM = 0.65f
        const val TEXT_LARGE = 0.75f
        const val LABEL_WIDTH = 12
    }
}
