package de.esserjan.com.sol.ui

import de.esserjan.com.catalog.persist
import de.esserjan.com.sol
import de.esserjan.com.sol.{Currency, service}
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.fixture
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatra.test.scalatest.ScalatraSuite

@RunWith(classOf[JUnitRunner])
class SolUiControllerTest
  extends fixture.FlatSpec
  with ScalatraSuite
  with MockitoSugar {

  val productsDao = mock[persist.ProductsDao]

  val offerFacade = mock[service.OfferFacade]

  val promotionFacade = mock[service.PromotionFacade]

  case class FixtureParam(product: persist.Product,
                          productNames: Seq[persist.ProductName],
                          offers: Seq[sol.Offer],
                          promos: Seq[sol.Promotion])

  override def withFixture(test: OneArgTest) = {
    val product = mock[persist.Product]
    when(product.id).thenReturn("abc")
    when(product.category).thenReturn(None)

    val productName = mock[persist.ProductName]
    when(productName.name).thenReturn("mockedProductName")
    when(productName.languageId).thenReturn("mockedLanguageTag")

    val offer = mock[sol.Offer]
    when(offer.product).thenReturn(product)
    val price = mock[sol.Price]
    when(offer.price).thenReturn(price)
    when(price.as(/*Currency*/ any())).thenReturn(BigDecimal(123.45d))

    val promo = mock[sol.ProductPromotion]
    when(promo.product).thenReturn(product)
    when(promo.price).thenReturn(price)

    super.withFixture(
      test.toNoArgTest(
        FixtureParam(product, List(productName), List(offer), List(promo))))
  }

  addServlet(new SolUiController(
    offerFacade,
    promotionFacade,
    productsDao),
    "/ui")

  val ACCEPT_LANGUAGE_HEADER =
    Map("Accept-Language" -> java.util.Locale.GERMANY.toLanguageTag)

  "SolUiController" should "list all offers" in { fixture =>
    val product = fixture.product
    val names = fixture.productNames
    val offers = fixture.offers

    doReturn(Some((product, names)))
      .when(productsDao)
      .findByIdWithName(/*productId*/ anyString())
    doReturn(offers)
      .when(offerFacade)
      .getAllOffers(product)

    get("/ui/offers/abc", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }

  it should "display the best offer" in { fixture =>
    val product = fixture.product
    val names = fixture.productNames
    val offers = fixture.offers

    doReturn(Some((product, names)))
      .when(productsDao)
      .findByIdWithName(/*productId*/ anyString())
    doReturn(Some(offers.head))
      .when(offerFacade)
      .getBestPriceOffer(/*product*/ any[persist.Product](), /*currency*/ any[Currency]())

    get("/ui/offers/abc/best", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }

  it should "gracefully fail on no best offer" in { fixture =>
    val product = fixture.product
    val names = fixture.productNames

    doReturn(Some((product, names)))
      .when(productsDao)
      .findByIdWithName(/*productId*/ anyString())
    doReturn(None)
      .when(offerFacade)
      .getBestPriceOffer(/*product*/ any[persist.Product](), /*currency*/ any[Currency]())

    get("/ui/offers/abc/best", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(404)
    }
  }

  it should "gracefully fail on unknown product" in { fixture =>
    doReturn(None)
      .when(productsDao)
      .findByIdWithName(/*productId*/ anyString())

    get("/ui/offers/abc/best", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(404)
    }
  }

  ignore should "suggest promotions" in { fixture =>
    val product = fixture.product
    val names = fixture.productNames
    val promos = fixture.promos

    doReturn(Some((product, names)))
      .when(productsDao)
      .findByIdWithName(/*productId*/ anyString())

    doReturn(promos)
      .when(promotionFacade)
      .getExclusivePromotions(product)

    get("/promotions/pabc") {
      status should equal(200)
    }
  }
}
