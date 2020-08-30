package somind.dtlab.routes.functions

import akka.pattern._
import com.typesafe.scalalogging.LazyLogging
import somind.dtlab.Conf._
import somind.dtlab.models._
import spray.json._

import scala.concurrent.Future

/**
  * Enable working with telemetry with meaningful names instead of the index values from
  * the type definition.
  *
  * Each marshaller will lookup the type definition and replace the index field with a text string.
  *
  * A "named" text string is the name of the DT prop.
  * A "pathed" text string is a DtPath with dot notation and the name of the DT prop appended.
  */
object Marshallers extends JsonSupport with LazyLogging {

  type Marshaller = (DtState, String, DtPath) => Future[Option[String]]

  private def fmt(s: DtState,
                  t: String,
                  dtp: DtPath,
                  dottedName: Boolean = false): Future[Option[String]] = {
    val f = dtDirectory ask t
    f.map {
      case Some(dt: DtType) =>
        val names: List[String] = dt.props.getOrElse(Set()).toList
        Some(
          s.state
            .map(pair => {
              val (nameIdx, origTelem) = pair
              val lookedUpName: String = {
                if (nameIdx >= names.length)
                  "unknown"
                else if (dottedName)
                  s"$dtp/${names(nameIdx)}".replace('/', '.').substring(11)
                else
                  names(nameIdx)
              }
              NamedTelemetry(lookedUpName, origTelem.value, origTelem.datetime)
            })
            .toJson
            .prettyPrint)
      case _ => None
    }
  }

  def namedFmt(s: DtState, t: String, dtp: DtPath): Future[Option[String]] =
    fmt(s, t, dtp)

  def pathedFmt(s: DtState, t: String, dtp: DtPath): Future[Option[String]] =
    fmt(s, t, dtp, dottedName = true)

  //noinspection ScalaUnusedSymbol
  def indexedFmt(s: DtState, t: String, dtp: DtPath): Future[Option[String]] =
    Future {
      Some(s.state.values.toJson.prettyPrint)
    }

}
