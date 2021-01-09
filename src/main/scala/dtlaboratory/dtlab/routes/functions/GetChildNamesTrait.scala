package dtlaboratory.dtlab.routes.functions

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.pattern._
import com.typesafe.scalalogging.LazyLogging
import dtlaboratory.dtlab.Conf._
import dtlaboratory.dtlab.models._
import dtlaboratory.dtlab.observe.Observer
import spray.json._

trait GetChildNamesTrait extends Directives with JsonSupport with LazyLogging {

  def applyChildrenFmt(dtp: DtPath): Route = {
    get {
      onSuccess(dtDirectory ask GetChildrenNames(dtp)) {
        case c: DtChildren =>
          Observer("actor_route_get_children_names_success")
          complete(
            HttpEntity(ContentTypes.`application/json`,
                       c.children.toJson.prettyPrint))
        case DtErr(emsg) =>
          Observer("actor_route_get_failure")
          complete(StatusCodes.NotFound, emsg)
        case e =>
          Observer("actor_route_get_unk_err")
          logger.warn(s"unable to handle: $e")
          complete(StatusCodes.InternalServerError)
      }
    }
  }

  def handleGetChildNames(segs: List[String]): Route = {
    Observer("actor_route_children_query")
    dtlaboratory.dtlab.models.DtPath(segs :+ "children") match {
      case Some(p: DtPath) =>
        val fullPath = dtlaboratory.dtlab.models
          .DtPath(new DtTypeName("root"), new DtInstanceName("root"), Some(p))
        applyChildrenFmt(fullPath)
      case _ =>
        logger.warn(s"can not extract DtPath from $segs")
        Observer("bad_request")
        complete(StatusCodes.BadRequest)
    }
  }

}