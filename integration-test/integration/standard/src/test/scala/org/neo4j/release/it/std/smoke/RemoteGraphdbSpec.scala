/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.release.it.std.smoke

import org.specs._

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.remote.RemoteGraphDatabase;

/**
 * Integration smoketest for the neo4j-remote-graphdb component.
 *
 */
class RemoteGraphdbSpec extends SpecificationWithJUnit {

  val RESOURCE_URI = "rmi://rmi-server/neo4j-graphdb"

  "neo4j remote graphdb" should {

    "attach to a remote graph database" in {
      skip("needs to start RMI, spawn neo4j server in a jvm.")
      // TODO: implement this integration test
    }

  }

}