package kz.narxoz.purchase.persistence;

import kz.narxoz.purchase.domain.PurchaseStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseStatusMapperTest {
    @ParameterizedTest
    @EnumSource(PurchaseStatus.class)
    void allProductStatusesRoundTrip(PurchaseStatus status) {
        assertEquals(status, PurchaseStatusMapper.fromDatabase(PurchaseStatusMapper.toDatabase(status)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"UNKNOWN", "OPEN", "draft", " DRAFT "})
    void unknownDatabaseTextIsRejected(String value) {
        assertThrows(IllegalStateException.class, () -> PurchaseStatusMapper.fromDatabase(value));
    }
}
