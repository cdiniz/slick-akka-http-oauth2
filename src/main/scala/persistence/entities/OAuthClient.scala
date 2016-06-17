package persistence.entities

import java.sql.Timestamp

case class OAuthClient(
                        id: Long,
                        ownerId: Long,
                        grantType: String,
                        clientId: String,
                        clientSecret: String,
                        redirectUri: Option[String],
                        createdAt: Timestamp
                      ) extends BaseEntity

