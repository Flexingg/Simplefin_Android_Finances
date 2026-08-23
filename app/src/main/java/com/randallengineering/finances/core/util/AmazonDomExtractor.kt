package com.randallengineering.finances.core.util

import com.randallengineering.finances.domain.model.AmazonOrderItem
import com.randallengineering.finances.domain.model.MatchedAmazonOrder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RawDomAmazonItem(
    val title: String = "",
    val asin: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)

@Serializable
data class RawDomAmazonOrder(
    val orderId: String = "",
    val orderDate: String = "",
    val orderTotal: Double = 0.0,
    val items: List<RawDomAmazonItem> = emptyList()
)

object AmazonDomExtractor {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Universal Mobile & Desktop DOM Extraction script for Amazon Orders.
     */
    val EXTRACTION_JS = """
        (function() {
            try {
                var itemsFound = [];
                var seenTitles = {};
                
                // 1. Traverse all card / container elements on the mobile orders page
                var cards = document.querySelectorAll('.order-card, .a-box-group, [data-order-id], .js-order-card, .yo-card, .a-box, [class*="orderCard"], [class*="item-card"], .a-fixed-left-grid, [class*="order-item"]');
                
                cards.forEach(function(card) {
                    var cardText = (card.innerText || card.textContent || '').trim();
                    if (!cardText || cardText.length < 5) return;
                    
                    // Extract Date
                    var date = '';
                    var dateMatch = cardText.match(/(?:Delivered|Ordered|Arriving|Placed)\s+([A-Za-z]+\s+\d+(?:,\s+\d{4})?)/i);
                    if (dateMatch && dateMatch[1]) {
                        date = dateMatch[1];
                    }
                    
                    // Extract Order ID if present
                    var orderId = '';
                    var idMatch = cardText.match(/\d{3}-\d{7}-\d{7}/);
                    if (idMatch) orderId = idMatch[0];
                    
                    // Extract ASIN
                    var asin = '';
                    var link = card.querySelector('a[href*="/dp/"], a[href*="/product/"], a[href*="/gp/product/"], a[href*="/gp/aw/d/"]');
                    if (link) {
                        var href = link.getAttribute('href') || '';
                        var asinMatch = href.match(/\/(?:dp|product|gp\/product|gp\/aw\/d)\/([A-Z0-9]{10})/i);
                        if (asinMatch && asinMatch[1]) asin = asinMatch[1];
                    }
                    
                    // Extract Product Title
                    var title = '';
                    // Try heading / bold text or link title first
                    var titleElem = card.querySelector('h2, h3, h4, h5, [class*="title"], [class*="product-title"], [class*="item-title"], a.a-link-normal, b, strong');
                    if (titleElem) {
                        var t = (titleElem.innerText || '').trim();
                        if (t.length > 5 && !t.match(/^(?:Delivered|Ordered|Arriving|Track|Return|Buy it|Problem|Feedback|Review|View|Details|Archive)/i)) {
                            title = t;
                        }
                    }
                    
                    // Fallback to image alt attribute
                    if (!title) {
                        var img = card.querySelector('img[alt]');
                        if (img) {
                            var alt = (img.getAttribute('alt') || '').trim();
                            if (alt.length > 4 && !alt.match(/^(?:amazon|prime|logo|icon|star|rating|arrow)/i)) {
                                title = alt;
                            }
                        }
                    }
                    
                    // Fallback: parse lines from card text
                    if (!title) {
                        var lines = cardText.split('\n');
                        for (var i = 0; i < lines.length; i++) {
                            var line = lines[i].trim();
                            if (line.length > 8 && !line.match(/^(?:Delivered|Ordered|Arriving|Track|Return|Buy it|Problem|Feedback|Review|View|Details|Total|\$)/i)) {
                                title = line;
                                break;
                            }
                        }
                    }
                    
                    if (title && title.length > 3 && !seenTitles[title]) {
                        seenTitles[title] = true;
                        itemsFound.push({
                            title: title,
                            asin: asin,
                            price: 0.0,
                            date: date || 'Recent Order',
                            orderId: orderId,
                            quantity: 1
                        });
                    }
                });
                
                // 2. Global scan across all images with alt text
                var allImages = document.querySelectorAll('img[alt]');
                allImages.forEach(function(img) {
                    var alt = (img.getAttribute('alt') || '').trim();
                    if (alt.length > 6 && !alt.match(/^(?:amazon|prime|logo|icon|star|rating|arrow|delivered|ordered)/i)) {
                        if (!seenTitles[alt]) {
                            seenTitles[alt] = true;
                            
                            // Find nearest container for date
                            var container = img.closest('div, a, li, section') || img.parentElement;
                            var containerText = container ? (container.innerText || '') : '';
                            var date = '';
                            var dateMatch = containerText.match(/(?:Delivered|Ordered|Arriving|Placed)\s+([A-Za-z]+\s+\d+(?:,\s+\d{4})?)/i);
                            if (dateMatch && dateMatch[1]) {
                                date = dateMatch[1];
                            }
                            
                            itemsFound.push({
                                title: alt,
                                asin: '',
                                price: 0.0,
                                date: date || 'Recent Order',
                                orderId: '',
                                quantity: 1
                            });
                        }
                    }
                });
                
                // 3. Group extracted items into orders
                var groupedOrders = [];
                var dateGroups = {};
                
                itemsFound.forEach(function(item, idx) {
                    var groupKey = (item.orderId ? item.orderId : (item.date ? item.date : ('Group-' + idx)));
                    if (!dateGroups[groupKey]) {
                        dateGroups[groupKey] = [];
                    }
                    dateGroups[groupKey].push(item);
                });
                
                for (var key in dateGroups) {
                    var groupItems = dateGroups[key];
                    var firstItem = groupItems[0];
                    var orderId = firstItem.orderId || ('114-' + Math.floor(1000000 + Math.random() * 9000000) + '-' + Math.floor(1000000 + Math.random() * 9000000));
                    var purchaseDate = firstItem.date || 'Recent';
                    
                    groupedOrders.push({
                        orderId: orderId,
                        orderDate: purchaseDate,
                        orderTotal: 0.0,
                        items: groupItems.map(function(it) {
                            return {
                                title: it.title,
                                asin: it.asin,
                                price: it.price,
                                quantity: it.quantity
                            };
                        })
                    });
                }
                
                return JSON.stringify(groupedOrders);
            } catch(e) {
                return JSON.stringify([]);
            }
        })();
    """.trimIndent()

    fun parseExtractedJson(rawJson: String): List<MatchedAmazonOrder> {
        return try {
            val cleanJson = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                // If WebKit stringified the JSON result, unquote/unescape it
                val unquoted = rawJson.substring(1, rawJson.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                unquoted
            } else {
                rawJson
            }

            val rawOrders = json.decodeFromString<List<RawDomAmazonOrder>>(cleanJson)
            rawOrders.map { raw ->
                val items = raw.items.map { item ->
                    AmazonOrderItem(
                        title = item.title.ifBlank { "Amazon Item" },
                        asin = item.asin,
                        quantityOrdered = item.quantity.coerceAtLeast(1),
                        itemPrice = item.price,
                        itemTax = 0.0,
                        totalPrice = if (item.price > 0) item.price * item.quantity else 0.0
                    )
                }.filter { it.title.length > 3 }

                MatchedAmazonOrder(
                    orderId = raw.orderId.ifBlank { "114-${(1000000..9999999).random()}-${(1000000..9999999).random()}" },
                    purchaseDate = raw.orderDate,
                    orderTotal = raw.orderTotal,
                    orderStatus = "Delivered",
                    items = items
                )
            }.filter { it.items.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
