package com.marmaladesky.realworld.db

import cats.effect.Async
import com.marmaladesky.realworld.db.gen.Tables
import com.marmaladesky.realworld.db.gen.Tables.FollowsRow
import slick.jdbc.JdbcBackend.Database
import com.marmaladesky.realworld.db.DbProfile.api._
import slick.dbio.SuccessAction
import slick.jdbc.JdbcBackend

import scala.concurrent.ExecutionContext

class FollowsRepo[F[_]: Async](db: Database, ec: ExecutionContext) {

  private implicit val database: JdbcBackend.Database = db
  private implicit val executionContext: ExecutionContext = ec

  def addFollow(master: Long, slave: Long): F[FollowsRow] = {
    val q = for {
      existingOpt <- Tables.Follows
        .filter { r => r.masterId === master && r.slaveId === slave }
        .result
        .map { _.headOption }
      upserted <- existingOpt match {
        case Some(existing) =>
          SuccessAction(existing)
        case None =>
          Tables.Follows
            .+= { FollowsRow(master, slave) }
            .flatMap { _ =>
              Tables.Follows
                .filter { r => r.masterId === master && r.slaveId === slave }
                .result
                .map { _.headOption.getOrElse(throw new RuntimeException(s"Failed to verify row")) }
            }
      }
    } yield upserted

    liftQ { q.transactionally }
  }

  def deleteFollow(master: Long, slave: Long): F[Boolean] = {
    val q = Tables.Follows
      .filter { r => r.masterId === master && r.slaveId === slave }
      .delete
      .map { _ > 0 }

    liftQ { q.transactionally }
  }

  def isFollowed(master: Long, slave: Long): F[Boolean] = {
    val q = Tables.Follows
      .filter { r => r.masterId === master && r.slaveId === slave }
      .exists
      .result

    liftQ { q.transactionally }
  }

  def getFollowed(slave: Long): F[Seq[FollowsRow]] = {
    val q = Tables.Follows.filter { r => r.slaveId === slave }.result

    liftQ { q.transactionally }
  }

}
