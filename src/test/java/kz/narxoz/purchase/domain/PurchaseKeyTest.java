package kz.narxoz.purchase.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseKeyTest {
    @Test
    void keepsTheHumanNumberFromLab1() {
        assertEquals("PUR-001", new PurchaseKey("PUR-001").value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\n "})
    void rejectsNullOrBlankBusinessKeys(String value) {
        assertThrows(IllegalArgumentException.class, () -> new PurchaseKey(value));
    }
}
