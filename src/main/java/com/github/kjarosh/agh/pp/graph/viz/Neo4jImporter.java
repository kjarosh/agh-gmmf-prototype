package com.github.kjarosh.agh.pp.graph.viz;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class Neo4jImporter implements AutoCloseable {
    private static final int BATCH_SIZE = 2000;

    private final Driver driver;
    private final Session session;

    public Neo4jImporter(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        session = driver.session();
    }

    public void purge() {
        log.info("Purging database");
        try (Transaction tx = session.beginTransaction()) {
            tx.run("match (n) detach delete n");
            tx.commit();
        }
        log.info("Database purged");
    }

    public void importGraph(Graph graph) {
        log.info("Importing graph");
        int vertexCount = graph.allVertices().size();
        int edgeCount = graph.allEdges().size();
        Transaction tx = session.beginTransaction();
        try {
            int imported = 0;
            for (Vertex vertex : graph.allVertices()) {
                importVertex(tx, vertex);
                ++imported;

                if (BATCH_SIZE > 0 && imported % BATCH_SIZE == 0) {
                    printStatus("vertices", imported, vertexCount);
                    tx.commit();
                    tx.close();
                    tx = session.beginTransaction();
                }
            }
            imported = 0;
            for (Edge v : graph.allEdges()) {
                importEdge(tx, v);
                ++imported;

                if (BATCH_SIZE > 0 && imported % BATCH_SIZE == 0) {
                    printStatus("edges", imported, edgeCount);
                    tx.commit();
                    tx.close();
                    tx = session.beginTransaction();
                }
            }

            log.info("Committing transaction");
            tx.commit();
        } finally {
            tx.close();
        }

        log.info("Graph imported");
    }

    private void printStatus(String desc, int imported, int count) {
        log.info("Importing {}: {}/{} ({} %)", desc, imported, count, 100D * imported / count);
    }

    private void importVertex(Transaction tx, Vertex vertex) {
        tx.run("CREATE (" + nodeDesc(vertex) + " { id: $id, type: $type })", Values.parameters(
                "id", vertex.id().toString(),
                "type", vertex.type().toString().toLowerCase()));
    }

    private String nodeDesc(Vertex vertex) {
        return ":" + vertex.type().toString();
    }

    private void importEdge(Transaction tx, Edge edge) {
        tx.run("MATCH (left), (right) " +
                "WHERE left.id = $src AND right.id = $dst AND NOT (left) -[:IN]-> (right) " +
                "CREATE (left) -[:IN]-> (right)", Values.parameters(
                "src", edge.src().toString(),
                "dst", edge.dst().toString()));
    }

    @Override
    public void close() {
        session.close();
        driver.close();
    }
}
