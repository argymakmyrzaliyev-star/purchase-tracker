package kz.narxoz.purchase;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchasePolicyTest {
    @ParameterizedTest(name = "{0} -> {1}; allowed = {2}")
    @CsvSource({
            "DRAFT, APPROVED, true",
            "APPROVED, ORDERED, true",
            "DRAFT, ORDERED, false",
            "ORDERED, DRAFT, false"
    })
    void matchesTheFourReadmeRows(PurchaseStatus from, PurchaseStatus to, boolean allowed) {
        if (allowed) {
            assertEquals(to, PurchasePolicy.move(from, to));
        } else {
            assertThrows(IllegalStateException.class, () -> PurchasePolicy.move(from, to));
        }
    }

    @ParameterizedTest(name = "Reject {0} -> {1}")
    @CsvSource({
            "DRAFT, DRAFT",
            "APPROVED, APPROVED",
            "ORDERED, ORDERED",
            "APPROVED, DRAFT",
            "ORDERED, APPROVED"
    })
    void rejectsOtherChanges(PurchaseStatus from, PurchaseStatus to) {
        assertThrows(IllegalStateException.class, () -> PurchasePolicy.move(from, to));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, APPROVED",
            "DRAFT, NULL",
            "NULL, NULL"
    }, nullValues = "NULL")
    void rejectsMissingStatuses(PurchaseStatus from, PurchaseStatus to) {
        assertThrows(NullPointerException.class, () -> PurchasePolicy.move(from, to));
    }
}
