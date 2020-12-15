package shopping.cart

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.pattern.StatusReply
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import shopping.cart.ShoppingCart.Summary

object ShoppingCartSpec {
  val config = ConfigFactory
    .parseString("""
      akka.actor.serialization-bindings {
        "shopping.cart.CborSerializable" = jackson-cbor
      }
      """)
    .withFallback(EventSourcedBehaviorTestKit.config)
}

class ShoppingCartSpec
    extends ScalaTestWithActorTestKit(ShoppingCartSpec.config)
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val cartId = "testCart"
  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[
      ShoppingCart.Command,
      ShoppingCart.Event,
      ShoppingCart.State](system, ShoppingCart(cartId))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    eventSourcedTestKit.clear()
  }

  "The shopping cart" should {
    "add an item" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, _))

      result1.reply.isSuccess should ===(true)

      val result2 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 13, _))

      // Item already added
      result2.reply.isError should ===(true)

    }

    "checkout" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, _))

      result1.reply.isSuccess should ===(true)

      val result2 = eventSourcedTestKit
        .runCommand[StatusReply[ShoppingCart.Summary]](ShoppingCart.Checkout)
      result2.reply should ===(
        StatusReply.Success(
          ShoppingCart.Summary(Map("foo" -> 42), checkedOut = true)))

      result2.event.asInstanceOf[ShoppingCart.CheckedOut].cartId should ===(
        cartId)

      val result3 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("bar", 13, _))
      result3.reply.isError should ===(true)
    }

    "get" in {
      val result1 =
        eventSourcedTestKit.runCommand[StatusReply[ShoppingCart.Summary]](
          ShoppingCart.AddItem("foo", 42, _))
      result1.reply.isSuccess should ===(true)

      val result2 = eventSourcedTestKit.runCommand[Summary](ShoppingCart.Get)

      result2.reply should ===(
        ShoppingCart.Summary(Map("foo" -> 42), checkedOut = false))
    }
  }
}
