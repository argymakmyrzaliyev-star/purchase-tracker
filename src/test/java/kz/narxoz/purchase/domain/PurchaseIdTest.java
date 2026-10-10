package kz.narxoz.purchase.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseIdTest {
    @Test
    void keepsAValidUuid() {
        UUID value = UUID.randomUUID();
        assertEquals(value, PurchaseId.parse(value.toString()).value());
    }

    @Test
    void generatesSeparateSurrogateIds() {
        assertNotEquals(PurchaseId.newId(), PurchaseId.newId());
    }

    @Test
    void rejectsNullUuid() {
        assertThrows(IllegalArgumentException.class, () -> new PurchaseId(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\n ", "PR-19", "not-a-uuid"})
    void rejectsMissingOrInvalidIds(String value) {
        assertThrows(IllegalArgumentException.class, () -> PurchaseId.parse(value));
    }
}
