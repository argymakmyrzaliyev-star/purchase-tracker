package kz.narxoz.purchase.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchasePolicyTest {
    private static Rule chain() {
        Rule approvalGuard = new UnapprovedCannotOrder();
        Rule transitions = new TransitionRule();
        return (from, to) -> {
            approvalGuard.check(from, to);
            transitions.check(from, to);
        };
    }

    @ParameterizedTest(name = "{0} -> {1}; allowed = {2}")
    @CsvSource({
            "DRAFT, APPROVED, true",
            "APPROVED, ORDERED, true",
            "DRAFT, ORDERED, false",
            "ORDERED, DRAFT, false"
    })
    void matchesTheFourReadmeRows(PurchaseStatus from, PurchaseStatus to, boolean allowed) {
        if (allowed) {
            assertEquals(to, new PurchasePolicy(chain()).move(from, to));
        } else {
            assertThrows(IllegalStateException.class, () -> new PurchasePolicy(chain()).move(from, to));
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
        assertThrows(IllegalStateException.class, () -> new PurchasePolicy(chain()).move(from, to));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, APPROVED",
            "DRAFT, NULL",
            "NULL, NULL"
    }, nullValues = "NULL")
    void rejectsMissingStatuses(PurchaseStatus from, PurchaseStatus to) {
        assertThrows(NullPointerException.class, () -> new PurchasePolicy(chain()).move(from, to));
    }
}
