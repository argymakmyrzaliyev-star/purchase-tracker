package kz.narxoz.purchase.persistence;

import kz.narxoz.purchase.config.Application;
import kz.narxoz.purchase.domain.*;
import kz.narxoz.purchase.handler.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** No test transaction: counts are observed after the service has committed/rolled back. */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class PurchaseJdbcIntegrationTest {
    @Autowired
    private PurchaseService service;
    @Autowired
    private PurchaseRepository purchases;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void createCleanTableFromHandWrittenSchema() throws Exception {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        try (var connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL", connection.getMetaData().getDatabaseProductName());
        }
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS purchase_tracker_lab3_test");
        assertEquals("purchase_tracker_lab3_test",
                jdbc.queryForObject("SELECT current_schema()", String.class));
        jdbc.execute("DROP TABLE IF EXISTS purchase_tracker_lab3_test.purchase_request");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void secondStatementRollsBack() throws Exception {
        DuplicatePurchase error = assertThrows(DuplicatePurchase.class,
                () -> service.insertTwice("PR-19"));

        assertUniqueViolation(error);
        assertEquals(0, countCommitted("PR-19"));
        assertEquals(0, purchases.count(new PurchaseKey("PR-19")));
        assertTrue(service.find("PR-19").isEmpty());
    }

    @Test
    void secondRequestKeepsTheFirst() throws Exception {
        PurchaseRequest first = service.register(PurchaseId.newId(), "PR-19", "First laptop");
        assertEquals(1, countCommitted("PR-19"));

        DuplicatePurchase error = assertThrows(DuplicatePurchase.class,
                () -> service.register(PurchaseId.newId(), "PR-19", "Second laptop"));

        assertUniqueViolation(error);
        assertEquals(1, countCommitted("PR-19"));
        assertEquals(1, service.count("PR-19"));
        assertEquals(first, service.find("PR-19").orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(PurchaseStatus.class)
    void storesAndReadsEveryProductStatus(PurchaseStatus status) {
        PurchaseRequest expected = new PurchaseRequest(PurchaseId.newId(),
                new PurchaseKey("PR-" + status), status, "Monitor for the team");
        purchases.insert(expected);
        assertEquals(expected, purchases.find(expected.businessKey()).orElseThrow());
        assertNotNull(jdbc.queryForObject(
                "SELECT created_at FROM purchase_request WHERE business_key = ?",
                java.time.OffsetDateTime.class, expected.businessKey().value()));
    }

    @Test
    void placeholdersKeepQuotedInputsAsData() throws Exception {
        String key = "PR-19'; DELETE FROM purchase_request; --";
        PurchaseRequest original = service.register(PurchaseId.newId(), "PR-SAFE", "Existing request");
        PurchaseRequest quoted = service.register(PurchaseId.newId(), key, "Manager's laptop");
        assertEquals(1, countCommitted(key));
        assertEquals(quoted, service.find(key).orElseThrow());
        assertEquals(original, service.find("PR-SAFE").orElseThrow());
    }

    @Test
    void schemaRejectsUnknownStatus() {
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("""
                        INSERT INTO purchase_request (id, business_key, status, title)
                        VALUES (?, ?, ?, ?)
                        """, UUID.randomUUID(), "PR-INVALID", "UNKNOWN", "Invalid status"));
        assertEquals("23514", sqlState(error));
        assertEquals(0, purchases.count(new PurchaseKey("PR-INVALID")));
    }

    @Test
    void schemaRejectsNullBusinessKey() {
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("""
                        INSERT INTO purchase_request (id, business_key, status, title)
                        VALUES (?, ?, ?, ?)
                        """, UUID.randomUUID(), null, "DRAFT", "Missing key"));
        assertEquals("23502", sqlState(error));
    }

    @Test
    void mapperRejectsUnknownTextEvenIfConstraintWasBypassed() {
        jdbc.execute("ALTER TABLE purchase_request DROP CONSTRAINT purchase_request_status_known");
        jdbc.update("""
                INSERT INTO purchase_request (id, business_key, status, title)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), "PR-CORRUPT", "UNKNOWN", "Legacy data");
        assertThrows(IllegalStateException.class, () -> purchases.find(new PurchaseKey("PR-CORRUPT")));
    }

    private long countCommitted(String key) throws SQLException {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT count(*) FROM purchase_request WHERE business_key = ?")) {
            assertTrue(connection.getAutoCommit());
            statement.setString(1, key);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }

    private void assertUniqueViolation(DuplicatePurchase error) {
        var cause = assertInstanceOf(DataIntegrityViolationException.class, error.getCause());
        assertEquals("23505", sqlState(cause));
    }

    private String sqlState(DataIntegrityViolationException error) {
        return assertInstanceOf(SQLException.class, error.getMostSpecificCause()).getSQLState();
    }
}
