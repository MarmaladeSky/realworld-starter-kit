package com.marmaladesky.realworld

import cats.effect.{Async, ExitCode}
import cats.implicits._
import com.marmaladesky.realworld.db.{ArticlesRepo, FollowsRepo, UsersRepo}
import com.marmaladesky.realworld.model.AuthContext
import com.marmaladesky.realworld.routes.{ArticlesRoutes, UsersRoutes}
import com.marmaladesky.realworld.services.{ArticlesService, UsersService}
import com.marmaladesky.realworld.utils.SlugGenerator
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.{HttpApp, Method}
import org.typelevel.log4cats.LoggerFactory
import slick.jdbc.JdbcBackend.Database
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.time.Clock
import scala.concurrent.duration._

object RealWorldServer {

  private val DbExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private def app[F[_]: Async: LoggerFactory](db: Database): HttpApp[F] = {
    val usersRepo = new UsersRepo[F](db, DbExecutionContext)
    val followsRepo = new FollowsRepo[F](db, DbExecutionContext)

    val usersService = UsersService.impl[F](Clock.systemUTC(), usersRepo, followsRepo)

    val articlesRepo = new ArticlesRepo[F](db, DbExecutionContext)
    val slugGenerator = new SlugGenerator[F]
    val articlesService = ArticlesService.impl[F](articlesRepo, usersRepo, followsRepo, slugGenerator)

    val authOptMiddleware: AuthMiddleware[F, Option[AuthContext]] = {
      AuthMiddleware(UsersRoutes.authUserOpt(usersService))
    }

    val httpApp = (
      UsersRoutes.publicRoutes(usersService) <+>
        authOptMiddleware {
          UsersRoutes.authedRoutes(usersService) <+>
            ArticlesRoutes.authedRoutes(articlesService)
        }
    ).orNotFound

    val corsPolicy = CORS.policy
      .withAllowOriginHostCi(_ => true)
      .withAllowCredentials(true)
      .withAllowMethodsIn(Set(Method.GET, Method.POST, Method.PUT, Method.DELETE))
      .withMaxAge(3.days)

    val requestLogger = LoggerFactory[F].getLogger

    Logger.httpApp(logHeaders = true, logBody = false, logAction = Some(requestLogger.info(_: String))) {
      corsPolicy(httpApp)
    }
  }

  def stream[F[_]: Async](db: Database, config: AppConfig): fs2.Stream[F, ExitCode] = {
    implicit val logging: LoggerFactory[F] = Slf4jFactory.create[F]

    val errorHandler = new DefaultErrorHandler[F]

    BlazeServerBuilder[F]
      .bindHttp(config.http.port, config.http.host)
      .withHttpApp(app(db))
      .withServiceErrorHandler(errorHandler)
      .serve
  }

}
