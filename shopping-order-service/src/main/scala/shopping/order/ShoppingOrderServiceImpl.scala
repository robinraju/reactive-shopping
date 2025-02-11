package shopping.order

import org.slf4j.LoggerFactory
import shopping.order.proto.{ OrderRequest, OrderResponse }

import scala.concurrent.Future

class ShoppingOrderServiceImpl extends proto.ShoppingOrderService {

  val logger = LoggerFactory.getLogger(getClass)

  override def order(in: OrderRequest): Future[OrderResponse] = {
    val totalItems = in.items.iterator.map(_.quantity).sum
    logger.info("Order {} items from cart {}", totalItems, in.cartId)
    Future.successful(OrderResponse(ok = true))
  }
}
