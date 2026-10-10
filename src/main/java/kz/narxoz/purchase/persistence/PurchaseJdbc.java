package kz.narxoz.purchase.persistence;

import kz.narxoz.purchase.domain.DuplicatePurchase;
import kz.narxoz.purchase.domain.PurchaseId;
import kz.narxoz.purchase.domain.PurchaseKey;
import kz.narxoz.purchase.domain.PurchaseRepository;
import kz.narxoz.purchase.domain.PurchaseRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PurchaseJdbc implements PurchaseRepository {
    private final JdbcTemplate jdbc;

    public PurchaseJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(PurchaseRequest purchase) {
        try {
            jdbc.update("""
                    INSERT INTO purchase_request (id, business_key, status, title)
                    VALUES (?, ?, ?, ?)
                    """, purchase.id().value(), purchase.businessKey().value(),
                    PurchaseStatusMapper.toDatabase(purchase.status()), purchase.title());
        } catch (DuplicateKeyException ex) {
            // PostgreSQL unique violations (23505) become a domain error.
            // Other integrity errors, such as CHECK violations, are not relabeled.
            throw new DuplicatePurchase(purchase.businessKey(), ex);
        }
    }

    @Override
    public long count(PurchaseKey businessKey) {
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM purchase_request WHERE business_key = ?",
                Long.class, businessKey.value());
        return count == null ? 0 : count;
    }

    @Override
    public Optional<PurchaseRequest> find(PurchaseKey businessKey) {
        List<PurchaseRequest> purchases = jdbc.query("""
                SELECT id, business_key, status, title
                FROM purchase_request WHERE business_key = ?
                """, (rs, rowNum) -> new PurchaseRequest(
                new PurchaseId(rs.getObject("id", UUID.class)),
                new PurchaseKey(rs.getString("business_key")),
                PurchaseStatusMapper.fromDatabase(rs.getString("status")),
                rs.getString("title")), businessKey.value());
        return purchases.stream().findFirst();
    }
}
