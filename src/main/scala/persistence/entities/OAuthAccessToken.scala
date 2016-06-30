package persistence.entities

import java.sql.Timestamp

case class OAuthAccessToken( id: Long,
                             accountId: Long,
                             oauthClientId: Long,
                             accessToken: String,
                             refreshToken: String,
                             createdAt: Timestamp
                           ) extends BaseEntity