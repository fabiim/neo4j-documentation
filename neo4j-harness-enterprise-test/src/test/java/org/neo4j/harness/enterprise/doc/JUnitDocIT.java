/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.harness.enterprise.doc;

import com.neo4j.harness.junit.rule.CommercialNeo4jRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.doc.server.HTTP;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.test.rule.TestDirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.internal.helpers.collection.Iterables.single;
import static org.neo4j.internal.helpers.collection.Iterators.count;
import static org.neo4j.server.ServerTestUtils.getRelativePath;

public class JUnitDocIT
{
    @ClassRule
    public static TestDirectory testDirectory = TestDirectory.testDirectory();

    // tag::useEnterpriseJUnitRule[]
    @Rule
    public Neo4jRule neo4j = new CommercialNeo4jRule()
            .withFixture( "CREATE (admin:Admin)" )
            .withConfig( GraphDatabaseSettings.legacy_certificates_directory.name(),
                    getRelativePath( testDirectory.storeDir(), GraphDatabaseSettings.legacy_certificates_directory ) )
            .withFixture( graphDatabaseService ->
            {
                try ( Transaction tx = graphDatabaseService.beginTx() )
                {
                    graphDatabaseService.createNode( Label.label( "Admin" ) );
                    tx.success();
                }
                return null;
            } );

    @Test
    public void shouldWorkWithServer() throws Exception
    {
        // Given
        URI serverURI = neo4j.httpURI();

        // When I access the server
        HTTP.Response response = HTTP.GET( serverURI.toString() );

        // Then it should reply
        assertEquals( 200, response.status() );

        // and we have access to underlying GraphDatabaseService
        try ( Transaction tx = neo4j.getGraphDatabaseService().beginTx() )
        {
            assertEquals( 2, count( neo4j.getGraphDatabaseService().findNodes( Label.label( "Admin" ) ) ) );
            tx.success();
        }
    }
    // end::useEnterpriseJUnitRule[]

    @Test
    public void shouldUserEnterpriseFeatures() throws Exception
    {
        // Given
        GraphDatabaseService db = neo4j.getGraphDatabaseService();

        // When I create property existence constraint
        try ( Transaction tx = db.beginTx() )
        {
            try ( Result result = db.execute( "CREATE CONSTRAINT ON (user:User) ASSERT exists(user.name)" ) )
            {
                // nothing to-do
            }
            tx.success();
        }

        // Then I can access created constraint
        try ( Transaction tx = db.beginTx() )
        {
            ConstraintDefinition constraint = single( db.schema().getConstraints() );

            assertTrue( constraint.isConstraintType( ConstraintType.NODE_PROPERTY_EXISTENCE ) );
            assertEquals( Label.label( "User" ), constraint.getLabel() );
            assertEquals( "name", single( constraint.getPropertyKeys() ) );

            tx.success();
        }
    }
}
